# Spec: Módulo de Produtos e Estoque

## Visão Geral

Este módulo gerencia o cadastro de produtos, suas tabelas de preço e o controle de estoque por depósito. Um produto existe independentemente de depósitos — o estoque é uma relação entre produto, depósito e localização física.

---

## Entidades

### Product

Representa um item comercializável no sistema.

| Campo         | Tipo        | Obrigatório | Regras                                                                 |
|---------------|-------------|-------------|------------------------------------------------------------------------|
| `id`          | UUID        | sim         | Gerado automaticamente                                                 |
| `name`        | String      | sim         | Mín. 3 caracteres, Máx. 100 caracteres. Único no sistema               |
| `description` | String      | não         | Máx. 500 caracteres                                                    |
| `sku`         | String      | sim         | Único no sistema. Formato: `[A-Z]{2,10}-[0-9]{1,6}` ex: `ABC-1234`   |
| `category`    | String      | não         | Máx. 50 caracteres                                                     |
| `defaultPrice`| BigDecimal  | sim         | Maior que zero. Usado quando não há tabela de preço aplicável          |
| `status`      | Enum        | sim         | `ACTIVE` ou `INACTIVE`. Padrão: `ACTIVE`                              |
| `createdAt`   | Timestamp   | sim         | Gerado automaticamente                                                 |
| `updatedAt`   | Timestamp   | sim         | Atualizado automaticamente a cada alteração                            |

---

### PriceTable

Tabela de preços nomeada e expansível. Um produto pode ter zero ou mais entradas em tabelas de preço.

| Campo       | Tipo       | Obrigatório | Regras                                    |
|-------------|------------|-------------|-------------------------------------------|
| `id`        | UUID       | sim         | Gerado automaticamente                    |
| `name`      | String     | sim         | Ex: `VAREJO`, `ATACADO`, `PARCEIRO`       |
| `productId` | UUID       | sim         | Referência ao produto                     |
| `price`     | BigDecimal | sim         | Maior que zero, não negativo, máx. 2 casas decimais |
| `createdAt` | Timestamp  | sim         | Gerado automaticamente                    |

---

### Warehouse (Depósito)

Representa um local físico onde produtos são armazenados.

| Campo      | Tipo    | Obrigatório | Regras                          |
|------------|---------|-------------|---------------------------------|
| `id`       | UUID    | sim         | Gerado automaticamente          |
| `name`     | String  | sim         | Mín. 3 caracteres, Máx. 100     |
| `address`  | String  | sim         | Máx. 255 caracteres             |
| `active`   | Boolean | sim         | Padrão: `true`                  |
| `createdAt`| Timestamp| sim        | Gerado automaticamente          |

---

### WarehouseStock (Estoque no Depósito)

Relaciona produto, depósito, quantidade e localização física dentro do depósito.

| Campo          | Tipo    | Obrigatório | Regras                                                           |
|----------------|---------|-------------|------------------------------------------------------------------|
| `id`           | UUID    | sim         | Gerado automaticamente                                           |
| `productId`    | UUID    | sim         | Referência ao produto                                            |
| `warehouseId`  | UUID    | sim         | Referência ao depósito                                           |
| `quantity`     | Int     | sim         | Não negativo. Zero é permitido                                   |
| `location`     | String  | sim         | Localização física. Ex: `5-E`, `CORREDOR-3`, `PRATELEIRA-B2`   |
| `updatedAt`    | Timestamp| sim        | Atualizado automaticamente a cada alteração                      |

> **Restrição:** A combinação `(productId, warehouseId)` deve ser única — um produto só tem uma entrada por depósito.

---

## Operações

### 1. Criar Produto

**Endpoint:** `POST /products`

**Request body:**
```json
{
  "name": "Caneta Esferográfica Azul",
  "description": "Caneta ponta média 1.0mm",
  "sku": "CAN-001",
  "category": "Papelaria",
  "defaultPrice": 2.50
}
```

**Comportamentos esperados:**

| Cenário | Resultado |
|--------|-----------|
| Dados válidos | Produto criado com `status = ACTIVE`. Retorna `201 Created` com o produto |
| `name` ausente ou vazio | `400 Bad Request` — "Nome é obrigatório" |
| `name` com menos de 3 caracteres | `400 Bad Request` — "Nome deve ter no mínimo 3 caracteres" |
| `name` com mais de 100 caracteres | `400 Bad Request` — "Nome deve ter no máximo 100 caracteres" |
| `name` já existente no sistema | `409 Conflict` — "Já existe um produto com este nome" |
| `sku` ausente ou vazio | `400 Bad Request` — "SKU é obrigatório" |
| `sku` em formato inválido (ex: `abc-123`, `AB123`) | `400 Bad Request` — "SKU deve seguir o formato ABC-1234 (letras maiúsculas, hífen, números)" |
| `sku` já existente no sistema | `409 Conflict` — "SKU já cadastrado no sistema" |
| `defaultPrice` ausente | `400 Bad Request` — "Preço padrão é obrigatório" |
| `defaultPrice` igual a zero | `400 Bad Request` — "Preço padrão deve ser maior que zero" |
| `defaultPrice` negativo | `400 Bad Request` — "Preço padrão não pode ser negativo" |

---

### 2. Listar Produtos

**Endpoint:** `GET /products`

**Query params opcionais:**

| Param      | Tipo   | Descrição                              |
|------------|--------|----------------------------------------|
| `category` | String | Filtra por categoria (exact match)     |
| `status`   | Enum   | `ACTIVE` ou `INACTIVE`. Padrão: `ACTIVE` |
| `page`     | Int    | Página (padrão: 0)                     |
| `size`     | Int    | Itens por página (padrão: 20, máx: 100)|

**Comportamentos esperados:**

| Cenário | Resultado |
|--------|-----------|
| Requisição válida | `200 OK` com lista paginada de produtos |
| Nenhum produto encontrado | `200 OK` com lista vazia `[]` |
| `size` acima de 100 | `400 Bad Request` — "Tamanho máximo por página é 100" |

---

### 3. Buscar Produto por ID

**Endpoint:** `GET /products/{id}`

**Comportamentos esperados:**

| Cenário | Resultado |
|--------|-----------|
| ID existe | `200 OK` com o produto |
| ID não existe | `404 Not Found` — "Produto não encontrado" |
| ID em formato inválido (não UUID) | `400 Bad Request` — "ID inválido" |

---

### 4. Atualizar Produto

**Endpoint:** `PATCH /products/{id}`

**Request body:** Todos os campos são opcionais — envie apenas os campos que deseja alterar. Campos não enviados permanecem inalterados.

```json
{
  "name": "Caneta Esferográfica Vermelha",
  "defaultPrice": 3.00
}
```

**Comportamentos esperados:**

| Cenário | Resultado |
|--------|-----------|
| Dados válidos | `200 OK` com produto atualizado. `updatedAt` é renovado |
| Produto não encontrado | `404 Not Found` — "Produto não encontrado" |
| `name` alterado para um já existente em outro produto | `409 Conflict` — "Já existe um produto com este nome" |
| `name` mantido igual (mesmo produto) | Permitido — não gera conflito |
| `sku` alterado para um já existente em outro produto | `409 Conflict` — "SKU já cadastrado no sistema" |
| `sku` mantido igual (mesmo produto) | Permitido — não gera conflito |
| Campo enviado com valor inválido | Mesmos erros `400` do criar |
| Body vazio `{}` | `400 Bad Request` — "Nenhum campo informado para atualização" |

---

### 5. Deletar Produto

**Endpoint:** `DELETE /products/{id}`

> Exclusão física — o produto é removido permanentemente do banco de dados.

**Comportamentos esperados:**

| Cenário | Resultado |
|--------|-----------|
| Produto existe | `204 No Content` |
| Produto não encontrado | `404 Not Found` — "Produto não encontrado" |
| Produto possui estoque em algum depósito | `409 Conflict` — "Não é possível excluir produto com estoque ativo em depósitos" |

### 6. Inativar Depósito

**Endpoint:** `PATCH /warehouses/{id}`

> Esta operação cobre a inativação de um depósito (`active: false`). Outros campos do depósito também podem ser atualizados via PATCH seguindo o mesmo padrão do produto.

**Request body:**
```json
{
  "active": false
}
```

**Comportamentos esperados:**

| Cenário | Resultado |
|--------|-----------|
| Depósito existe e não tem produtos com `quantity > 0` | `200 OK` — depósito inativado |
| Depósito não encontrado | `404 Not Found` — "Depósito não encontrado" |
| Depósito possui produtos com `quantity > 0` | `409 Conflict` com lista de produtos bloqueantes (ver abaixo) |
| Depósito já está inativo | `200 OK` — operação idempotente, sem efeito |

**Resposta de erro quando há produtos com estoque:**

```json
{
  "error": "Estoques não podem ser inativados quando há produtos nele, defina um lugar para esses produtos",
  "details": {
    "warehouseId": "uuid-do-deposito",
    "warehouseName": "Depósito Central",
    "productsWithStock": [
      {
        "productId": "uuid",
        "productName": "Caneta Esferográfica Azul",
        "sku": "CAN-001",
        "quantity": 42,
        "location": "5-E"
      },
      {
        "productId": "uuid",
        "productName": "Caderno Universitário",
        "sku": "CAD-002",
        "quantity": 15,
        "location": "CORREDOR-3"
      }
    ]
  }
}
```

> Apenas produtos com `quantity > 0` aparecem na lista. Produtos zerados (`quantity = 0`) não bloqueiam a inativação.

---

## Formato do SKU

O SKU deve obedecer a expressão regular:

```
^[A-Z]{2,10}-[0-9]{1,6}$
```

**Exemplos válidos:** `CAN-001`, `PROD-1234`, `AB-9`, `NOTEBOOK-100`

**Exemplos inválidos:** `can-001` (minúsculas), `AB123` (sem hífen), `A-001` (menos de 2 letras), `MUITASLETRAS123-001` (mais de 10 letras)

---

## Respostas Padrão

### Produto (response)
```json
{
  "id": "uuid",
  "name": "Caneta Esferográfica Azul",
  "description": "Caneta ponta média 1.0mm",
  "sku": "CAN-001",
  "category": "Papelaria",
  "defaultPrice": 2.50,
  "status": "ACTIVE",
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```

### Erro (response)
```json
{
  "error": "mensagem de erro",
  "details": ["campo: detalhe adicional"]
}
```

---

## Decisões de Design

1. **`defaultPrice` no produto:** Todo produto nasce com um preço padrão obrigatório. Tabelas de preço (`PriceTable`) são opcionais e gerenciadas em módulo separado.

2. **Estoque desacoplado do produto:** A quantidade nunca fica no produto. Toda movimentação de estoque passa pela entidade `WarehouseStock`, que vincula produto + depósito + localização física.

3. **Localização física no estoque:** Cada entrada de `WarehouseStock` tem um campo `location` livre (ex: `5-E`, `CORREDOR-3`) para identificar onde o produto está fisicamente no depósito.

4. **Exclusão física com guarda:** O produto pode ser deletado, mas o sistema impede a exclusão se houver registros de `WarehouseStock` com `quantity > 0` para ele.

5. **SKU e Nome únicos:** Ambos são identificadores de negócio. O sistema valida unicidade em criação e atualização — conflito com o próprio registro não gera erro.

6. **PATCH em vez de PUT:** Atualizações são parciais — apenas os campos enviados são alterados. Body vazio é rejeitado explicitamente para evitar chamadas acidentais sem efeito.

7. **Inativação de depósito bloqueada por estoque:** Um depósito não pode ser inativado enquanto tiver produtos com `quantity > 0`. O erro lista todos os produtos bloqueantes com localização, para o operador decidir para onde movê-los antes de prosseguir.

8. **Produtos zerados não bloqueiam:** `quantity = 0` significa que o produto está cadastrado no depósito mas sem unidades físicas — não bloqueia inativação.

9. **Entradas de estoque permanecem após inativação:** Ao inativar um depósito, os registros de `WarehouseStock` com `quantity = 0` **não são deletados**. Eles ficam vinculados ao depósito inativo para fins de auditoria — preservando o histórico de quais produtos já estiveram naquele depósito.

---

## Domain Puro (Framework-Free)

O `domain` é o núcleo do sistema e **não deve ter nenhuma dependência externa** — nem Spring, nem JPA, nem Jakarta, nem qualquer biblioteca de infraestrutura. Ele deve ser compilável e testável de forma completamente isolada.

### Regra de ouro

> Se uma classe do `domain` tiver qualquer `import` que não seja da stdlib Kotlin ou de outro arquivo do próprio `domain`, ela está errada.

### O que pertence ao domain

| O que é | Onde fica | Exemplo |
|--------|-----------|---------|
| Entidade de negócio | `domain/entities/` | `Product`, `Warehouse` |
| Value Object | `domain/entities/.../value_objects/` | `Sku`, `Price` |
| Interface de repositório | `domain/entities/` | `IProductRepository` |
| Enum de negócio | `domain/entities/` | `ProductStatus`, `UserRole` |
| Exceções de domínio | `domain/shared/` | `DomainException` |
| Classe base `Entity` | `domain/shared/` | `Entity<T>` |
| Classe base `ValueObject` | `domain/shared/` | `ValueObject<T>` |

### O que NÃO pertence ao domain

| Proibido | Por quê |
|---------|---------|
| `@Entity`, `@Table`, `@Column` | Anotações JPA — pertencem à infraestrutura |
| `@Component`, `@Service`, `@Bean` | Anotações Spring — pertencem à infraestrutura |
| `@NotBlank`, `@Valid` | Jakarta Bean Validation — pertence à apresentação |
| Qualquer acesso a banco, HTTP ou I/O | Efeitos colaterais — pertencem à infraestrutura |

### Exemplo correto

```kotlin
// ✅ domain/entities/product/ProductEntity.kt
// Nenhum import externo — apenas stdlib Kotlin
class Product private constructor(
    private val props: ProductProps,
    id: String? = null
) : Entity<ProductProps>(props, id) {

    val name: String get() = props.name
    val sku: Sku get() = props.sku
    val defaultPrice: Price get() = props.defaultPrice

    companion object {
        fun create(props: ProductProps, id: String? = null): Product {
            require(props.name.trim().length >= 3) { "Nome deve ter no mínimo 3 caracteres" }
            require(props.name.trim().length <= 100) { "Nome deve ter no máximo 100 caracteres" }
            return Product(props.copy(name = props.name.trim()), id)
        }
    }
}
```

```kotlin
// ❌ ERRADO — JPA vazando no domain
@Entity                          // <- proibido
@Table(name = "products")        // <- proibido
class Product(
    @Id val id: String,          // <- proibido
    @NotBlank val name: String   // <- proibido
)
```

### Onde as anotações de framework ficam

As anotações JPA vivem em uma classe **separada** na infraestrutura, que mapeia o domain para o banco:

```kotlin
// ✅ infrastructure/database/jpa/entities/ProductJpaEntity.kt
@Entity
@Table(name = "products")
data class ProductJpaEntity(
    @Id val id: String,
    @Column(nullable = false) val name: String,
    // ...
)
```

O repositório converte entre as duas representações via `toDomain()` e `toJpaEntity()` — o domain nunca vê o JPA, o JPA nunca vaza pro domain.

### Benefício direto

Testes de entidades e casos de uso rodam **sem subir o Spring**, sem banco, sem contexto — apenas JUnit puro. Isso torna o feedback loop de desenvolvimento muito mais rápido.

---

## Processo de Desenvolvimento

### Commits Atômicos

Cada etapa da implementação deve gerar um commit isolado e significativo. Um commit atômico representa **uma única unidade de trabalho completa** — não um arquivo, não uma feature inteira.

**Regra:** o código no commit deve compilar e os testes existentes devem passar. Nunca commitar estado quebrado.

#### Convenção de mensagem (Conventional Commits)

```
<tipo>(escopo): descrição curta em português
```

| Tipo | Quando usar |
|------|-------------|
| `feat` | Nova funcionalidade ou caso de uso |
| `test` | Adição ou correção de testes |
| `refactor` | Melhoria de código sem mudar comportamento |
| `fix` | Correção de bug |
| `chore` | Setup, configuração, dependências |
| `docs` | Alterações em specs ou documentação |

#### Sequência esperada de commits por operação

Seguindo a arquitetura em camadas, cada operação deve gerar commits nesta ordem:

```
chore(produto): adiciona migração SQL da tabela products
feat(produto): cria entidade Product e value object SKU
test(produto): testa criação e validações da entidade Product
feat(produto): define interface IProductRepository
feat(produto): implementa caso de uso CreateProduct
test(produto): testa caso de uso CreateProduct
feat(produto): implementa ProductRepositoryImpl com JPA
feat(produto): adiciona endpoint POST /products
test(produto): testa endpoint POST /products (integração)
```

> Cada linha acima = um commit. A spec de cada operação define os cenários — os testes devem cobrir todos eles.

---

## Fora de Escopo (por ora)

- Autenticação e autorização
- Movimentação de estoque (entrada/saída)
- Histórico de alterações de preço
- Gerenciamento de tabelas de preço (CRUD de PriceTable)
- Gerenciamento de depósitos (CRUD de Warehouse)