# Spec: Módulo de Autenticação e Usuários

## Visão Geral

A autenticação é delegada ao **Keycloak** — um Identity Provider (IdP) open source que gerencia usuários, senhas, sessões e tokens JWT. O Spring Boot **não gerencia senhas nem sessões** — ele apenas valida os tokens emitidos pelo Keycloak e protege os endpoints com base nas roles contidas no token.

---

## Arquitetura de Auth

```
Usuário
  │
  ├─ POST /auth/login ──────────────► Keycloak
  │                                      │
  │                         emite JWT com roles
  │                                      │
  │◄─────────────────────────────────────┘
  │
  ├─ GET /products (Authorization: Bearer <jwt>) ──► Spring Boot
  │                                                      │
  │                                          valida JWT com Keycloak
  │                                                      │
  │                                          verifica role no token
  │                                                      │
  │◄─────────────────────── 200 OK ou 401/403 ───────────┘
```

**Responsabilidades:**

| Responsabilidade | Quem faz |
|-----------------|----------|
| Gerenciar senhas | Keycloak |
| Emitir tokens JWT | Keycloak |
| Validar tokens | Spring Security + Keycloak |
| Gerenciar roles | Keycloak (sincronizado via API Admin) |
| Proteger endpoints | Spring Security |
| CRUD de usuários | Spring Boot (via Keycloak Admin API) |

---

## Infraestrutura

### Keycloak via Docker

O Keycloak sobe junto com PostgreSQL e a aplicação via `docker-compose.yml`:

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:24.0
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
    KC_DB: postgres
    KC_DB_URL: jdbc:postgresql://postgres:5432/${DB_NAME}
    KC_DB_USERNAME: ${DB_USER}
    KC_DB_PASSWORD: ${DB_PASSWORD}
  ports:
    - "8180:8080"
  command: start-dev
```

### Configuração do Realm

| Configuração | Valor |
|-------------|-------|
| Realm | `products` |
| Client | `products-api` |
| Grant Type | `password` (para login direto) |
| Token Type | JWT (Bearer) |
| Auto-registro | **Desabilitado** — apenas ADMIN cria usuários |

---

## Roles

| Role | Descrição |
|------|-----------|
| `ADMIN` | Acesso total ao sistema, incluindo gerenciamento de usuários |
| `MANAGER` | Gerencia produtos e depósitos, sem acesso a usuários |
| `VIEWER` | Apenas leitura de produtos |

> **Extensível:** novas roles podem ser criadas no Keycloak e mapeadas para novos endpoints sem alteração na arquitetura existente. Ao criar uma nova role, basta definir suas permissões na tabela da seção de Controle de Acesso e proteger os endpoints correspondentes no Spring Security.

---

## Controle de Acesso por Endpoint

### Produtos

| Endpoint | VIEWER | MANAGER | ADMIN |
|----------|--------|---------|-------|
| `GET /products` | ✅ | ✅ | ✅ |
| `GET /products/{id}` | ✅ | ✅ | ✅ |
| `POST /products` | ❌ | ✅ | ✅ |
| `PATCH /products/{id}` | ❌ | ✅ | ✅ |
| `DELETE /products/{id}` | ❌ | ✅ | ✅ |

### Depósitos

| Endpoint | VIEWER | MANAGER | ADMIN |
|----------|--------|---------|-------|
| `GET /warehouses` | ❌ | ✅ | ✅ |
| `GET /warehouses/{id}` | ❌ | ✅ | ✅ |
| `POST /warehouses` | ❌ | ✅ | ✅ |
| `PATCH /warehouses/{id}` | ❌ | ✅ | ✅ |
| `PATCH /warehouses/{id}/status` | ❌ | ✅ | ✅ |

### Usuários

| Endpoint | VIEWER | MANAGER | ADMIN |
|----------|--------|---------|-------|
| `GET /users` | ❌ | ❌ | ✅ |
| `GET /users/{id}` | ❌ | ❌ | ✅ |
| `POST /users` | ❌ | ❌ | ✅ |
| `PATCH /users/{id}` | ❌ | ❌ | ✅ |
| `DELETE /users/{id}` | ❌ | ❌ | ✅ |
| `GET /users/me` | ✅ | ✅ | ✅ |

> `GET /users/me` retorna o perfil do próprio usuário autenticado — acessível por qualquer role.

---

## Entidade de Usuário

O usuário é gerenciado **primariamente no Keycloak**. O Spring Boot mantém uma tabela local `users` apenas com dados de perfil estendido — sem senha, sem sessão.

### Keycloak (gerencia)
- `id` (Keycloak UUID — usado como chave de sincronização)
- `email` (login)
- `senha` (hash gerenciado pelo Keycloak)
- `roles`
- `active/enabled`

### Tabela local `users` (perfil estendido)

| Campo        | Tipo      | Obrigatório | Regras                                      |
|--------------|-----------|-------------|---------------------------------------------|
| `id`         | UUID      | sim         | Mesmo UUID do Keycloak (sincronizado)       |
| `name`       | String    | sim         | Mín. 3 caracteres, Máx. 100                 |
| `email`      | String    | sim         | Único. Sincronizado com Keycloak            |
| `phone`      | String    | não         | Máx. 20 caracteres                          |
| `jobTitle`   | String    | não         | Cargo/Função. Máx. 100 caracteres           |
| `createdAt`  | Timestamp | sim         | Gerado automaticamente. Sempre em UTC       |
| `updatedAt`  | Timestamp | sim         | Atualizado automaticamente. Sempre em UTC   |

> **Regra de sincronização:** o `id` do usuário local é sempre o mesmo UUID gerado pelo Keycloak. Isso garante rastreabilidade entre os dois sistemas sem duplicação de identidade.

---

## Operações

### 1. Login

**Endpoint:** `POST /auth/login`

> Esta chamada é feita **diretamente ao Keycloak** — não passa pelo Spring Boot.

**Request body:**
```json
{
  "username": "usuario@email.com",
  "password": "senha123"
}
```

**Resposta de sucesso:**
```json
{
  "access_token": "eyJhbGci...",
  "expires_in": 300,
  "refresh_token": "eyJhbGci...",
  "token_type": "Bearer"
}
```

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Credenciais válidas | `200 OK` com `access_token` e `refresh_token` |
| Credenciais inválidas | `401 Unauthorized` — "Credenciais inválidas" |
| Usuário desabilitado no Keycloak | `401 Unauthorized` — "Usuário inativo" |

---

### 2. Refresh Token

**Endpoint:** `POST /auth/refresh`

> Também feita **diretamente ao Keycloak**.

**Request body:**
```json
{
  "refresh_token": "eyJhbGci..."
}
```

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Refresh token válido | `200 OK` com novo `access_token` |
| Refresh token expirado ou inválido | `401 Unauthorized` |

---

### 3. Criar Usuário

**Endpoint:** `POST /users`

**Requer role:** `ADMIN`

**Request body:**
```json
{
  "name": "João Silva",
  "email": "joao@empresa.com",
  "phone": "11999990000",
  "jobTitle": "Analista de Estoque",
  "role": "MANAGER",
  "password": "senhaTemporaria123"
}
```

**Fluxo interno:**
1. Cria o usuário no Keycloak via Admin API
2. Atribui a role no Keycloak
3. Salva o perfil estendido na tabela local `users` com o UUID retornado pelo Keycloak
4. Retorna o usuário criado

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Dados válidos | `201 Created` com o usuário (sem senha) |
| Requisição sem token | `401 Unauthorized` |
| Token de role insuficiente (não ADMIN) | `403 Forbidden` |
| `name` ausente ou vazio | `400 Bad Request` — `USER_NAME_REQUIRED` — "Nome é obrigatório" |
| `name` com menos de 3 caracteres | `400 Bad Request` — `USER_NAME_TOO_SHORT` — "Nome deve ter no mínimo 3 caracteres" |
| `email` ausente ou inválido | `400 Bad Request` — `USER_EMAIL_INVALID` — "Email inválido" |
| `email` já existente | `409 Conflict` — `USER_EMAIL_ALREADY_EXISTS` — "Email já cadastrado" |
| `role` inválida ou inexistente | `400 Bad Request` — `USER_ROLE_INVALID` — "Role inválida" |
| `password` ausente ou vazio | `400 Bad Request` — `USER_PASSWORD_REQUIRED` — "Senha temporária é obrigatória" |
| Keycloak cria usuário mas banco local falha | Rollback: usuário é deletado do Keycloak. Retorna `500 Internal Server Error` — `USER_CREATION_FAILED` — "Erro ao finalizar criação do usuário" |
| Falha na comunicação com Keycloak | `502 Bad Gateway` — `KEYCLOAK_UNAVAILABLE` — "Erro ao comunicar com o serviço de autenticação" |

---

### 4. Listar Usuários

**Endpoint:** `GET /users`

**Requer role:** `ADMIN`

**Query params opcionais:**

| Param    | Tipo   | Descrição                          |
|----------|--------|------------------------------------|
| `role`   | String | Filtra por role                    |
| `page`   | Int    | Página (padrão: 0)                 |
| `size`   | Int    | Itens por página (padrão: 20, máx: 100) |

**Ordenação padrão:** `createdAt DESC`

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Requisição válida | `200 OK` com lista paginada ordenada por `createdAt DESC` |
| Sem token ou role insuficiente | `401` ou `403` |
| Lista vazia | `200 OK` com `[]` |

---

### 5. Buscar Usuário por ID

**Endpoint:** `GET /users/{id}`

**Requer role:** `ADMIN`

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| ID existe | `200 OK` com o usuário |
| ID não existe | `404 Not Found` — `USER_NOT_FOUND` — "Usuário não encontrado" |
| ID inválido (não UUID) | `400 Bad Request` — `INVALID_ID` — "ID inválido" |
| Sem token ou role insuficiente | `401` ou `403` |

---

### 6. Meu Perfil

**Endpoint:** `GET /users/me`

**Requer:** qualquer token válido (todas as roles)

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Token válido e perfil local existe | `200 OK` com perfil do usuário autenticado |
| Token válido mas perfil local não encontrado | `404 Not Found` — `USER_PROFILE_NOT_FOUND` — "Perfil não encontrado, contate o administrador" |
| Sem token | `401 Unauthorized` |

> O `id` do usuário é extraído do próprio JWT — sem precisar passar ID na URL.

---

### 7. Atualizar Usuário

**Endpoint:** `PATCH /users/{id}`

**Requer role:** `ADMIN`

**Request body:** todos os campos opcionais.

```json
{
  "name": "João Silva Santos",
  "phone": "11988880000",
  "jobTitle": "Gerente de Estoque",
  "role": "MANAGER"
}
```

> `email` e `password` **não são atualizáveis** por este endpoint — operações sensíveis gerenciadas diretamente no Keycloak.

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Dados válidos | `200 OK` com usuário atualizado |
| Usuário não encontrado | `404 Not Found` — `USER_NOT_FOUND` — "Usuário não encontrado" |
| `role` alterada | Atualizada no Keycloak. Refletida no próximo token (tokens existentes continuam válidos até expirar — máx. 5 minutos) |
| `null` explícito em campo opcional (ex: `{"phone": null}`) | Campo é limpo |
| `null` explícito em campo obrigatório (ex: `{"name": null}`) | `400 Bad Request` — mesmo code do campo ausente |
| Body vazio `{}` | `400 Bad Request` — `EMPTY_UPDATE_BODY` — "Nenhum campo informado para atualização" |
| Sem token ou role insuficiente | `401` ou `403` |

---

### 8. Deletar Usuário

**Endpoint:** `DELETE /users/{id}`

**Requer role:** `ADMIN`

**Fluxo interno:**
1. Desabilita o usuário no Keycloak (não exclui — preserva histórico)
2. Mantém o registro local em `users` para auditoria

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Usuário existe | `204 No Content` |
| Usuário não encontrado | `404 Not Found` — `USER_NOT_FOUND` — "Usuário não encontrado" |
| ADMIN tentando deletar a si mesmo | `409 Conflict` — `ADMIN_CANNOT_DELETE_SELF` — "Administrador não pode remover a própria conta" |
| Sem token ou role insuficiente | `401` ou `403` |

---

## Respostas Padrão

### Erro (response)
```json
{
  "code": "USER_EMAIL_ALREADY_EXISTS",
  "error": "Email já cadastrado",
  "details": ["campo: detalhe adicional"]
}
```

> O campo `code` é um identificador fixo em `SCREAMING_SNAKE_CASE` — o frontend deve usá-lo para tratar erros específicos, nunca fazer parse da string `error`.

### Usuário (response)
```json
{
  "id": "uuid",
  "name": "João Silva",
  "email": "joao@empresa.com",
  "phone": "11999990000",
  "jobTitle": "Analista de Estoque",
  "role": "MANAGER",
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```

---

## Decisões de Design

1. **Keycloak como fonte da verdade de identidade:** senha, sessão e roles vivem no Keycloak. O Spring Boot nunca toca em senha — elimina uma classe inteira de vulnerabilidades.

2. **UUID sincronizado:** o `id` local é o mesmo UUID do Keycloak. Sem tabela de mapeamento extra, sem risco de dessincronização de identidade.

2a. **Rollback em falha de criação:** se o Keycloak criar o usuário com sucesso mas o banco local falhar, o sistema deleta o usuário do Keycloak antes de retornar erro. Garante consistência entre os dois sistemas.

3. **Deletar = desabilitar no Keycloak:** usuários deletados são desabilitados no Keycloak, não excluídos. O registro local em `users` é preservado para auditoria — quem criou o produto, quem moveu o estoque.

4. **Email e senha fora do PATCH:** alteração de email e senha são operações sensíveis com fluxos próprios no Keycloak (verificação de email, reset de senha). Não faz sentido expô-las num PATCH genérico.

4a. **Mudança de role não invalida tokens ativos:** tokens JWT existentes continuam válidos até expirar (máx. 5 minutos). Esse comportamento é documentado e aceito — não é um bug. O tempo de expiração curto minimiza a janela de risco.

4b. **`null` explícito em PATCH limpa campos opcionais:** `phone` e `jobTitle` aceitam `null` e são zerados. `name` e `email` rejeitam `null`. Campos não enviados permanecem inalterados.

4c. **Todos os timestamps em UTC:** O sistema opera exclusivamente em UTC. Conversão de fuso horário é responsabilidade do cliente.

5. **ADMIN não pode se auto-deletar:** evita o cenário de o sistema ficar sem administrador ativo.

6. **Roles extensíveis:** novas roles são criadas no Keycloak e mapeadas no Spring Security sem mudança estrutural. A spec deve ser atualizada com a tabela de permissões da nova role antes da implementação.

7. **`GET /users/me` acessível a todas as roles:** todo usuário autenticado precisa conseguir ver o próprio perfil — independente de permissões.

8. **`502` em falha do Keycloak:** o sistema deve tratar falhas de comunicação com o Keycloak de forma explícita, não deixar estourar como `500` genérico.

---

## Domain Puro (Framework-Free)

A entidade `User` no domain não carrega nenhuma anotação de framework:

```kotlin
// ✅ domain/entities/user/UserEntity.kt
class User private constructor(
    private val props: UserProps,
    id: String? = null
) : Entity<UserProps>(props, id) {

    val name: String get() = props.name
    val email: String get() = props.email
    val phone: String? get() = props.phone
    val jobTitle: String? get() = props.jobTitle
    val role: UserRole get() = props.role

    companion object {
        fun create(props: UserProps, id: String? = null): User {
            require(props.name.trim().length >= 3) { "Nome deve ter no mínimo 3 caracteres" }
            require(props.name.trim().length <= 100) { "Nome deve ter no máximo 100 caracteres" }
            return User(props.copy(name = props.name.trim()), id)
        }
    }
}

enum class UserRole { ADMIN, MANAGER, VIEWER }
```

---

## Processo de Desenvolvimento

### Sequência esperada de commits

```
chore(auth): adiciona Keycloak ao docker-compose
chore(auth): configura realm, client e roles no Keycloak
chore(auth): adiciona dependência Spring Security + OAuth2 Resource Server
chore(auth): migração SQL da tabela users
feat(auth): configura Spring Security para validar JWT do Keycloak
test(auth): testa proteção de endpoints sem token e com roles insuficientes
feat(user): cria entidade User no domain
test(user): testa criação e validações da entidade User
feat(user): define interface IUserRepository e IKeycloakGateway
feat(user): implementa caso de uso CreateUser
test(user): testa caso de uso CreateUser
feat(user): implementa caso de uso UpdateUser
test(user): testa caso de uso UpdateUser
feat(user): implementa UserRepositoryImpl com JPA
feat(user): implementa KeycloakGatewayImpl via Admin API
feat(user): adiciona endpoints POST, GET, PATCH, DELETE /users
feat(user): adiciona endpoint GET /users/me
test(user): testa endpoints de usuários (integração)
```

---

## Variáveis de Ambiente

```env
KEYCLOAK_URL=http://localhost:8180
KEYCLOAK_REALM=products
KEYCLOAK_CLIENT_ID=products-api
KEYCLOAK_CLIENT_SECRET=seu_client_secret
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASSWORD=sua_senha_admin
```

---

## Fora de Escopo (por ora)

- Reset de senha por email
- Verificação de email no cadastro
- Login social (Google, GitHub)
- Refresh token gerenciado pelo Spring Boot
- Múltiplos realms
- Auditoria de login (último acesso, tentativas falhas)