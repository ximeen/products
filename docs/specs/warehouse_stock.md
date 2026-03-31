# Spec: Módulo de WarehouseStock (Estoque em Depósito)

## Visão Geral

Este módulo gerencia o vínculo entre produtos e depósitos — defining onde cada produto está armazenado fisicamente, em que quantidade e em qual localização dentro do depósito.

---

## Entidade

### WarehouseStock

| Campo         | Tipo      | Obrigatório | Regras                                                                    |
|---------------|-----------|-------------|---------------------------------------------------------------------------|
| `id`          | UUID      | sim         | Gerado automaticamente                                                    |
| `productId`   | UUID      | sim         | Referência ao produto                                                     |
| `warehouseId` | UUID      | sim         | Referência ao depósito                                                    |
| `quantity`    | Int       | sim         | Não negativo. Padrão: 0                                                  |
| `location`    | String    | sim         | Localização física. Ex: `5-E`, `CORREDOR-3`, `PRATELEIRA-B2`           |
| `createdAt`   | Timestamp | sim         | Gerado automaticamente                                                    |
| `updatedAt`   | Timestamp | sim         | Atualizado automaticamente a cada alteração                               |

> **Restrição:** A combinação `(productId, warehouseId)` deve ser única — um produto só pode ter uma entrada por depósito.

---

## Decisões de Design

### 1. Location único por depósito

Cada localização física (`location`) é **única dentro de um depósito**. Dois produtos diferentes não podem ocupar a mesma localização (`5-E`) no mesmo depósito. Isso representa a realidade física onde um espaço só pode guardar um item.

### 2. Estoque zerado não é exclusão

Quando `quantity` chega a zero, o registro **não é deletado**. Ele permanece para:
- Preservar histórico de quais produtos já estuvo-
- Permitir reativação rápida (basta aumentar quantity novamente)
- Manter a referência de localização para auditoria

### 3. Adição vs Atualização

O vínculo entre produto e depósito pode ser:
- **Novo**: produto ainda não está no depósito → criar registro
- **Existente**: produto já está no depósito → atualizar quantity e/ou location

---

## Operações

### 1. Adicionar Produto ao Depósito

**Endpoint:** `POST /warehouse-stocks`

**Request body:**
```json
{
  "productId": "uuid-do-produto",
  "warehouseId": "uuid-do-deposito",
  "quantity": 50,
  "location": "5-E"
}
```

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Dados válidos, produto novo no depósito | `201 Created` com registro criado |
| Dados válidos, produto já existe no depósito | `200 OK` — quantity atualizada |
| `productId` não existe | `404 Not Found` — "Produto não encontrado" |
| `warehouseId` não existe | `404 Not Found` — "Depósito não encontrado" |
| `warehouseId` inativo | `409 Conflict` — "Depósito está inativo" |
| `location` já ocupada por outro produto no mesmo depósito | `409 Conflict` — "Localização já ocupada no depósito" |
| `quantity` negativo | `400 Bad Request` — "Quantidade não pode ser negativa" |
| `location` vazio ou maior que 50 | `400 Bad Request` — "Localização inválida" |

---

### 2. Atualizar Estoque (quantity ou location)

**Endpoint:** `PATCH /warehouse-stocks/{id}`

**Request body:** Todos os campos são opcionais — envie apenas os campos que deseja alterar.

```json
{
  "quantity": 100,
  "location": "10-A"
}
```

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Dados válidos | `200 OK` com estoque atualizado |
| Estoque não encontrado | `404 Not Found` — "Estoque não encontrado" |
| `location` pertence a outro produto no mesmo depósito | `409 Conflict` — "Localização já ocupada no depósito" |
| `quantity` negativo | `400 Bad Request` — "Quantidade não pode ser negativa" |
| Body vazio `{}` | `400 Bad Request` — "Nenhum campo informado para atualização" |

---

### 3. Zerar Estoque (Remover produto do depósito)

**Endpoint:** `DELETE /warehouse-stocks/{id}` ou `PATCH /warehouse-stocks/{id}` com `quantity: 0`

> **Decisão:** DELETE remove o registro fisicamente. Mas como `quantity = 0` preserva o registro, a operação recomendada é **PATCH com quantity: 0** para manter o histórico.

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Estoque existe | `200 OK` — quantity definida como 0 |
| Estoque não encontrado | `404 Not Found` — "Estoque não encontrado" |

---

### 4. Listar Estoques por Depósito

**Endpoint:** `GET /warehouse-stocks?warehouseId={id}`

**Query params:**

| Param         | Tipo    | Descrição                              |
|---------------|---------|----------------------------------------|
| `warehouseId` | UUID    | Obrigatório. Filtrar por depósito      |
| `page`        | Int     | Página (padrão: 0)                    |
| `size`        | Int     | Itens por página (padrão: 20, máx: 100)|

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Dados válidos | `200 OK` com lista de estoques |
| `warehouseId` não existe | `404 Not Found` — "Depósito não encontrado" |

---

### 5. Listar Estoques por Produto

**Endpoint:** `GET /warehouse-stocks?productId={id}`

**Query params:**

| Param        | Tipo | Descrição                              |
|--------------|------|----------------------------------------|
| `productId`  | UUID | Obrigatório. Filtrar por produto       |
| `page`       | Int  | Página (padrão: 0)                    |
| `size`       | Int  | Itens por página (padrão: 20, máx: 100)|

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| Dados válidos | `200 OK` com lista de estoques |
| `productId` não existe | `404 Not Found` — "Produto não encontrado" |

---

### 6. Obter Estoque por ID

**Endpoint:** `GET /warehouse-stocks/{id}`

**Comportamentos esperados:**

| Cenário | Resultado |
|---------|-----------|
| ID existe | `200 OK` com o estoque |
| ID não existe | `404 Not Found` — "Estoque não encontrado" |

---

## Respostas Padrão

### WarehouseStock (response)

```json
{
  "id": "uuid",
  "productId": "uuid",
  "warehouseId": "uuid",
  "quantity": 50,
  "location": "5-E",
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z"
}
```

### Lista de estoques (response)

```json
{
  "stocks": [
    {
      "id": "uuid",
      "productId": "uuid",
      "productName": "Caneta Esferográfica Azul",
      "warehouseId": "uuid",
      "warehouseName": "Depósito Central",
      "quantity": 50,
      "location": "5-E"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## Validações de Location

- Obrigatório
- Mínimo 1 caractere, máximo 50
- Pode conter letras, números, hífen e underscore
- Único por depósito (não pode haver dois produtos na mesma `location`)

---

## Fora de Escopo (por ora)

- Movimentação de estoque (entrada/saída com histórico)
- Transferência entre depósitos
- Alerta de estoque baixo
- Relatório de capacidade do depósito