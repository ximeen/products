# Spec: Módulo de Depósito (Warehouse)

## Visão Geral

Este módulo gerencia os depósitos físicos onde produtos são armazenados. Um depósito é um local nomeado com endereço, podendo ser ativado ou inativado. O estoque de produtos por depósito é gerenciado pelo módulo de Produtos via `WarehouseStock`.

---

## Entidade

### Warehouse

| Campo       | Tipo      | Obrigatório | Regras                                      |
|-------------|-----------|-------------|---------------------------------------------|
| `id`        | UUID      | sim         | Gerado automaticamente                      |
| `name`      | String    | sim         | Mín. 3 caracteres, Máx. 100. Único no sistema |
| `address`   | String    | sim         | Mín. 5 caracteres, Máx. 255 caracteres      |
| `active`    | Boolean   | sim         | Padrão: `true`                              |
| `createdAt` | Timestamp | sim         | Gerado automaticamente. Sempre em UTC       |
| `updatedAt` | Timestamp | sim         | Atualizado automaticamente a cada alteração. Sempre em UTC |

---

## Operações

### 1. Criar Depósito

**Endpoint:** `POST /warehouses`

**Request body:**
```json
{
  "name": "Depósito Central",
  "address": "Rua das Flores, 123 - São Paulo, SP"
}
```

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Dados válidos | Depósito criado com `active = true`. Retorna `201 Created` com o depósito |
| `name` ausente ou vazio | `400 Bad Request` — `WAREHOUSE_NAME_REQUIRED` — "Nome é obrigatório" |
| `name` com menos de 3 caracteres | `400 Bad Request` — `WAREHOUSE_NAME_TOO_SHORT` — "Nome deve ter no mínimo 3 caracteres" |
| `name` com mais de 100 caracteres | `400 Bad Request` — `WAREHOUSE_NAME_TOO_LONG` — "Nome deve ter no máximo 100 caracteres" |
| `name` já existente no sistema | `409 Conflict` — `WAREHOUSE_NAME_ALREADY_EXISTS` — "Já existe um depósito com este nome" |
| `address` ausente ou vazio | `400 Bad Request` — `WAREHOUSE_ADDRESS_REQUIRED` — "Endereço é obrigatório" |
| `address` com menos de 5 caracteres | `400 Bad Request` — `WAREHOUSE_ADDRESS_TOO_SHORT` — "Endereço deve ter no mínimo 5 caracteres" |
| `address` com mais de 255 caracteres | `400 Bad Request` — `WAREHOUSE_ADDRESS_TOO_LONG` — "Endereço deve ter no máximo 255 caracteres" |

---

### 2. Listar Depósitos

**Endpoint:** `GET /warehouses`

**Query params opcionais:**

| Param    | Tipo    | Descrição                                          |
|----------|---------|----------------------------------------------------|
| `active` | Boolean | Filtra por status. Padrão: `true` (apenas ativos)  |
| `page`   | Int     | Página (padrão: 0)                                 |
| `size`   | Int     | Itens por página (padrão: 20, máx: 100)            |

**Ordenação padrão:** `name ASC`

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Requisição sem filtros | `200 OK` — retorna apenas depósitos com `active = true`, ordenados por `name ASC` |
| `active=false` | `200 OK` — retorna apenas depósitos inativos |
| Nenhum depósito encontrado | `200 OK` com lista vazia `[]` |
| `size` acima de 100 | `400 Bad Request` — `INVALID_PAGE_SIZE` — "Tamanho máximo por página é 100" |

---

### 3. Buscar Depósito por ID

**Endpoint:** `GET /warehouses/{id}`

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| ID existe (ativo ou inativo) | `200 OK` com o depósito |
| ID não existe | `404 Not Found` — "Depósito não encontrado" |
| ID em formato inválido (não UUID) | `400 Bad Request` — "ID inválido" |

> Busca por ID retorna o depósito independentemente do status `active` — útil para auditoria.

---

### 4. Atualizar Depósito

**Endpoint:** `PATCH /warehouses/{id}`

**Request body:** Todos os campos são opcionais — envie apenas os campos que deseja alterar.

```json
{
  "name": "Depósito Central Norte",
  "address": "Av. Paulista, 1000 - São Paulo, SP"
}
```

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Dados válidos | `200 OK` com depósito atualizado. `updatedAt` é renovado |
| Depósito não encontrado | `404 Not Found` — `WAREHOUSE_NOT_FOUND` — "Depósito não encontrado" |
| Depósito inativo | Permitido — campos de dados podem ser atualizados mesmo inativo |
| `name` alterado para um já existente em outro depósito | `409 Conflict` — `WAREHOUSE_NAME_ALREADY_EXISTS` — "Já existe um depósito com este nome" |
| `name` mantido igual (mesmo depósito) | Permitido — não gera conflito |
| Campo enviado com valor inválido | Mesmos codes e erros `400` do criar |
| `null` explícito em campo obrigatório (ex: `{"name": null}`) | `400 Bad Request` — mesmo code do campo ausente |
| Body vazio `{}` | `400 Bad Request` — `EMPTY_UPDATE_BODY` — "Nenhum campo informado para atualização" |

> **Nota:** O campo `active` **não** pode ser alterado por esta operação — use a operação de Inativar/Reativar para isso.

---

### 5. Inativar ou Reativar Depósito

**Endpoint:** `PATCH /warehouses/{id}/status`

**Request body:**
```json
{
  "active": false
}
```

#### Inativar (`active: false`)

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Depósito existe e não tem produtos com `quantity > 0` | `200 OK` — depósito inativado |
| Depósito não encontrado | `404 Not Found` — `WAREHOUSE_NOT_FOUND` — "Depósito não encontrado" |
| Depósito já está inativo | `200 OK` — operação idempotente, sem efeito |
| Depósito possui produtos com `quantity > 0` | `409 Conflict` — `WAREHOUSE_HAS_ACTIVE_STOCK` — com lista de produtos bloqueantes (ver abaixo) |

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

> Apenas produtos com `quantity > 0` aparecem na lista. Produtos zerados não bloqueiam a inativação e permanecem vinculados ao depósito para auditoria.

#### Reativar (`active: true`)

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Depósito existe e está inativo | `200 OK` — depósito reativado |
| Depósito não encontrado | `404 Not Found` — `WAREHOUSE_NOT_FOUND` — "Depósito não encontrado" |
| Depósito já está ativo | `200 OK` — operação idempotente, sem efeito |

---

## Resposta Padrão

### Depósito (response)
```json
{
  "id": "uuid",
  "name": "Depósito Central",
  "address": "Rua das Flores, 123 - São Paulo, SP",
  "active": true,
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```

### Erro (response)
```json
{
  "code": "WAREHOUSE_NAME_ALREADY_EXISTS",
  "error": "Já existe um depósito com este nome",
  "details": ["campo: detalhe adicional"]
}
```

> O campo `code` é um identificador fixo em `SCREAMING_SNAKE_CASE` — o frontend deve usá-lo para tratar erros específicos, nunca fazer parse da string `error`.

---

## Decisões de Design

1. **Nome único:** O nome é um identificador de negócio — dois depósitos com o mesmo nome causariam confusão operacional. Unicidade é validada em criação e atualização.

2. **Inativar e atualizar são operações separadas:** `PATCH /warehouses/{id}` altera dados (nome, endereço). `PATCH /warehouses/{id}/status` altera o ciclo de vida (`active`). Essa separação evita que uma atualização acidental de dados inative um depósito sem querer.

3. **Busca por ID ignora status:** `GET /warehouses/{id}` retorna o depósito ativo ou inativo — necessário para consultas de auditoria e para o sistema de estoque referenciar depósitos históricos.

4. **Atualização permitida em depósito inativo:** Dados como nome e endereço podem ser corrigidos mesmo após inativação — útil para corrigir informações históricas sem precisar reativar o depósito.

5. **Entradas de estoque preservadas após inativação:** Registros de `WarehouseStock` com `quantity = 0` permanecem vinculados ao depósito inativo para fins de auditoria — preservando o histórico de quais produtos já estiveram naquele depósito.

6. **Operações de status são idempotentes:** Inativar um depósito já inativo (ou reativar um já ativo) retorna `200 OK` sem erro — comportamento seguro para chamadas repetidas.

7. **`location` é único por depósito:** Dois produtos não podem ocupar a mesma localização física num depósito. A combinação `(warehouseId, location)` deve ser única em `WarehouseStock`. Isso reflete a realidade física — uma prateleira tem um produto.

8. **`null` explícito em PATCH não é aceito em campos obrigatórios:** `name` e `address` rejeitam `null` com o mesmo code de campo ausente. Não há campos opcionais em Warehouse, portanto `null` nunca limpa nada.

9. **Todos os timestamps em UTC:** O sistema opera exclusivamente em UTC. Conversão de fuso horário é responsabilidade do cliente.

---

## Domain Puro (Framework-Free)

Seguindo o mesmo princípio do módulo de Produtos, a entidade `Warehouse` não deve conter nenhuma anotação de framework:

```kotlin
// ✅ domain/entities/warehouse/WarehouseEntity.kt
class Warehouse private constructor(
    private val props: WarehouseProps,
    id: String? = null
) : Entity<WarehouseProps>(props, id) {

    val name: String get() = props.name
    val address: String get() = props.address
    val active: Boolean get() = props.active

    fun deactivate() {
        props.active = false
        touch()
    }

    fun reactivate() {
        props.active = true
        touch()
    }

    companion object {
        fun create(props: WarehouseProps, id: String? = null): Warehouse {
            require(props.name.trim().length >= 3) { "Nome deve ter no mínimo 3 caracteres" }
            require(props.name.trim().length <= 100) { "Nome deve ter no máximo 100 caracteres" }
            require(props.address.trim().length >= 5) { "Endereço deve ter no mínimo 5 caracteres" }
            require(props.address.trim().length <= 255) { "Endereço deve ter no máximo 255 caracteres" }
            return Warehouse(props.copy(name = props.name.trim(), address = props.address.trim()), id)
        }
    }
}
```

---

## Processo de Desenvolvimento

### Sequência esperada de commits

```
chore(warehouse): adiciona migração SQL da tabela warehouses
feat(warehouse): cria entidade Warehouse no domain
test(warehouse): testa criação, validações e ciclo de vida da entidade Warehouse
feat(warehouse): define interface IWarehouseRepository
feat(warehouse): implementa caso de uso CreateWarehouse
test(warehouse): testa caso de uso CreateWarehouse
feat(warehouse): implementa caso de uso UpdateWarehouse
test(warehouse): testa caso de uso UpdateWarehouse
feat(warehouse): implementa caso de uso ChangeWarehouseStatus (inativar/reativar)
test(warehouse): testa caso de uso ChangeWarehouseStatus
feat(warehouse): implementa WarehouseRepositoryImpl com JPA
feat(warehouse): adiciona endpoints POST, GET, PATCH /warehouses
test(warehouse): testa endpoints de warehouse (integração)
```

---

## Fora de Escopo (por ora)

- Transferência de estoque entre depósitos
- Histórico de ativações/inativações
- Capacidade máxima do depósito
- Múltiplos responsáveis por depósito