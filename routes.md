# Rotas da API

## Products

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/products` | Criar produto |
| `GET` | `/products/{id}` | Obter produto por ID |
| `GET` | `/products` | Listar produtos (paginado) |
| `PATCH` | `/products/{id}` | Atualizar produto |
| `DELETE` | `/products/{id}` | Deletar produto |

**Query Parameters (Listar):**
- `category` (opcional): Filtrar por categoria
- `status` (opcional): Filtrar por status (ACTIVE, INACTIVE)
- `page` (padrão: 0): Número da página
- `size` (padrão: 20, máx: 100): Tamanho por página

---

## Warehouses

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/warehouses` | Criar warehouse |
| `GET` | `/warehouses` | Listar warehouses (paginado) |
| `GET` | `/warehouses/{id}` | Obter warehouse por ID |
| `PATCH` | `/warehouses/{id}` | Atualizar warehouse |
| `PATCH` | `/warehouses/{id}/status` | Alterar status do warehouse |

**Query Parameters (Listar):**
- `active` (opcional): Filtrar por status ativo
- `page` (padrão: 0): Número da página
- `size` (padrão: 20, máx: 100): Tamanho por página