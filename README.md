# Products API

API RESTful para gerenciamento de produtos, estoques e depósitos desenvolvida com Kotlin e Spring Boot.

## Stack Tecnológica

| Categoria | Tecnologia |
|-----------|------------|
| Runtime | JVM 21 (Eclipse Temurin) |
| Linguagem | Kotlin 2.2 |
| API | Spring Boot 4 + Spring Web MVC |
| ORM | Spring Data JPA + Hibernate |
| Validação | Jakarta Bean Validation |
| Database | PostgreSQL |
| Migrações | Flyway |
| Build | Gradle (Kotlin DSL) |

## Arquitetura

O projeto segue os princípios da **Clean Architecture** com quatro camadas:

```
src/main/kotlin/com/ximenes/products/
├── domain/           # Regras de negócio (núcleo)
├── application/      # Casos de uso
├── infrastructure/   # Implementações externas (JPA, Controllers)
└── shared/           # Código compartilhado (erros, utils)
```

### Camadas

- **Domain**: Entidades, value objects e interfaces de repositórios. Zero dependências externas.
- **Application**: Casos de uso que orquestram o domínio. Depende apenas do domain.
- **Infrastructure**: Repositórios JPA, controllers REST e adapters.
- **Shared**: Código reutilizável — erros, constantes e configurações.

## Começando

### Pré-requisitos

- JDK 21
- PostgreSQL 15+
- Docker (opcional)

### Configuração com Docker

```bash
docker compose up -d
```

Isso inicia:
- PostgreSQL na porta 5432
- Redis na porta 6379 (quando configurado)

### Configuração Manual

Crie um banco de dados PostgreSQL:

```sql
CREATE DATABASE products_db;
```

Configure as variáveis de ambiente:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/products_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=sua_senha
SERVER_PORT=8080
```

### Executando a Aplicação

```bash
./gradlew bootRun
```

A API estará disponível em `http://localhost:8080`

## Endpoints

### Products

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

### Warehouses

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

## Exemplos de Requisição

### Criar Produto

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Caneta Esferográfica Azul",
    "description": "Caneta ponta média 1.0mm",
    "sku": "CAN-001",
    "category": "Papelaria",
    "defaultPrice": 2.50
  }'
```

### Listar Produtos

```bash
curl "http://localhost:8080/products?category=Papelaria&page=0&size=20"
```

### Criar Warehouse

```bash
curl -X POST http://localhost:8080/warehouses \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Depósito Central",
    "address": "Rua das Flores, 123 - São Paulo, SP"
  }'
```

## Scripts Disponíveis

| Comando | Descrição |
|---------|-----------|
| `./gradlew bootRun` | Servidor em modo desenvolvimento |
| `./gradlew build` | Build de produção |
| `./gradlew test` | Executar testes |
| `./gradlew flywayMigrate` | Aplicar migrações |
| `./gradlew flywayInfo` | Status das migrações |

## Estrutura do Projeto

### Domain Layer

```
domain/
├── entities/
│   ├── product/
│   │   ├── Product.kt
│   │   ├── ProductStatus.kt
│   │   ├── IProductRepository.kt
│   │   └── value_objects/
│   │       ├── Sku.kt
│   │       └── Price.kt
│   ├── warehouse/
│   │   ├── Warehouse.kt
│   │   └── IWarehouseRepository.kt
│   └── warehouse_stock/
│       ├── WarehouseStock.kt
│       └── IWarehouseStockRepository.kt
└── shared/
    ├── Entity.kt
    └── ValueObject.kt
```

### Application Layer

```
application/
└── use_cases/
    ├── product/
    │   ├── CreateProduct.kt
    │   ├── GetProduct.kt
    │   ├── ListProducts.kt
    │   ├── UpdateProduct.kt
    │   └── DeleteProduct.kt
    └── warehouse/
        ├── CreateWarehouse.kt
        ├── GetWarehouseById.kt
        ├── ListWarehouses.kt
        ├── UpdateWarehouse.kt
        └── ChangeWarehouseStatus.kt
```

### Infrastructure Layer

```
infrastructure/
├── database/
│   ├── jpa/
│   │   ├── entities/
│   │   │   ├── ProductJpaEntity.kt
│   │   │   ├── WarehouseJpaEntity.kt
│   │   │   └── WarehouseStockJpaEntity.kt
│   │   └── repositories/
│   │       ├── ProductJpaRepository.kt
│   │       ├── WarehouseJpaRepository.kt
│   │       └── WarehouseStockJpaRepository.kt
│   └── repositories/
│       ├── ProductRepositoryImpl.kt
│       ├── WarehouseRepositoryImpl.kt
│       └── WarehouseStockRepositoryImpl.kt
└── http/spring/
    ├── controllers/
    │   ├── ProductController.kt
    │   └── WarehouseController.kt
    └── config/
        └── GlobalExceptionHandler.kt
```

## Migrações

As migrações do banco de dados estão em `src/main/resources/db/migration/`.

Para aplicar migrações:

```bash
./gradlew flywayMigrate
```

## Testes

Os testes seguem o padrão de domain-driven design, sem dependência do Spring:

```bash
./gradlew test
```

### Estrutura de Testes

```
src/test/kotlin/com/ximenes/products/
└── domain/entities/
    └── product/
        └── ProductTest.kt
```

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|-----------|--------|-----------|
| `SERVER_PORT` | 8080 | Porta do servidor |
| `SPRING_PROFILES_ACTIVE` | development | Perfil do Spring |
| `SPRING_DATASOURCE_URL` | jdbc:postgresql://localhost:5432/dbname | URL do banco |
| `SPRING_DATASOURCE_USERNAME` | postgres | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | — | Senha do banco |
| `SPRING_DATA_REDIS_HOST` | localhost | Host do Redis |
| `SPRING_DATA_REDIS_PORT` | 6379 | Porta do Redis |

## Decisões de Design

1. **Domain Puro**: O domínio não contém nenhuma anotação de framework (JPA, Spring, Jakarta). É compilável e testável de forma isolada.

2. **Value Objects**: SKU e Price são value objects imutáveis com validação interna.

3. **PATCH para Atualização**: Atualizações são parciais — apenas os campos enviados são alterados.

4. **Validação Fail-Fast**: Validações de negócio ocorrem no domínio, antes de chegar ao banco.

5. **Exclusão Física com Guarda**: Produtos com estoque em depósitos não podem ser deletados.

6. **Inativação de Depósito Protegida**: Um depósito com produtos (quantity > 0) não pode ser inativado.

## Segurança

O projeto inclui specs de testes de segurança para SQL Injection em `/docs/specs/security_spec.md`.

Execute os testes de segurança:

```bash
./gradlew test --tests "*Sqli*"
```

## Licença

MIT