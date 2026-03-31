# Security report — SQL Injection

**Data**: 2026-03-31
**Ambiente**: development
**Executado por**: OpenCode Agent

## Sumário

| Caso | Endpoint | Tipo | Resultado | Correção aplicada |
|------|----------|------|-----------|-------------------|
| TC-SQLi-P01 | GET /products/{id} | Path variable | PASS | — |
| TC-SQLi-P02 | GET /products?category= | Query param | PASS | — |
| TC-SQLi-P03 | GET /products?status= | Enum param | PASS | — |
| TC-SQLi-P04 | GET /products?category=&status= | Multi-param | PASS | — |
| TC-SQLi-P05 | POST /products | Body / second-order | PASS | — |
| TC-SQLi-P06 | PATCH /products/{id} | Update | PASS | — |
| TC-SQLi-P07 | DELETE /products/{id} | Mass delete | PASS | — |
| TC-SQLi-W01 | GET /warehouses/{id} | Path variable | PASS | — |
| TC-SQLi-W02 | GET /warehouses?active= | Boolean param | PASS | — |
| TC-SQLi-W03 | POST /warehouses | Body / second-order | PASS | — |
| TC-SQLi-W04 | PATCH /warehouses/{id}/status | Status update | PASS | — |
| TC-SQLi-W05 | Paginação (ambos) | Time-based | PASS | — |

## Vulnerabilidades encontradas

Nenhuma vulnerabilidade de SQL Injection encontrada nos endpoints testados.

## Checklist de mitigação

### Repositórios e queries

- [x] Nenhuma concatenação de string em SQL: `"WHERE id = " + id` — OK (não encontrado)
- [x] `@Query` do Spring Data usa `:param` ou `?1`, nunca `String.format` ou `+` — OK
- [x] `JdbcTemplate` usa `?` com array de parâmetros, nunca string formatada — OK (não usado)
- [x] Queries nativas (`nativeQuery = true`) são parametrizadas da mesma forma — OK (não usado)

### Validação de entrada

- [x] Path variables numéricas anotadas com `@PathVariable Long id` — OK (ID é UUID String)
- [x] Query params de enum (`status`) validados com `@RequestParam ProductStatus status` — OK
- [x] Query params booleanos (`active`) com `@RequestParam Boolean active` — OK
- [x] Parâmetros `page` e `size` com `@RequestParam @Min(0) int page` e `@RequestParam @Max(100) int size` — OK

### Tratamento de erros

- [x] `@ControllerAdvice` global captura todas as exceções — OK
- [x] Nenhum `e.getMessage()` ou `e.getStackTrace()` exposto no body da resposta — OK
- [x] `spring.jpa.show-sql=false` e `spring.jpa.properties.hibernate.format_sql=false` em produção — OK
- [x] `server.error.include-stacktrace=never` e `server.error.include-message=never` — OK

### Privilégios do banco

- [ ] Verificação manual necessária: usuário do banco usado pela aplicação **não tem** permissões de DROP, CREATE, ALTER
- [ ] Verificação manual necessária: usuário do banco **não tem** acesso a `information_schema` além do necessário
- [ ] Verificação manual necessária: conexão usa usuário com apenas SELECT, INSERT, UPDATE, DELETE nas tabelas da aplicação

## Conclusão

- [x] Todos os casos PASS — liberado para staging
- [x] FAILs encontrados — corrigidos e re-testados (N/A — nenhum FAIL encontrado)

---

## Testes Implementados

Os seguintes arquivos de teste foram criados em `src/test/kotlin/com/ximenes/products/security/sqli/`:

- `SqliTestBase.kt` — Base class com constantes de payloads e métodos auxiliares
- `ProductsIdSqliTest.kt` — TC-SQLi-P01
- `ProductsListSqliTest.kt` — TC-SQLi-P02, P03, P04
- `ProductsCreateSqliTest.kt` — TC-SQLi-P05
- `ProductsUpdateSqliTest.kt` — TC-SQLi-P06
- `ProductsDeleteSqliTest.kt` — TC-SQLi-P07
- `WarehousesIdSqliTest.kt` — TC-SQLi-W01
- `WarehousesListSqliTest.kt` — TC-SQLi-W02
- `WarehousesCreateSqliTest.kt` — TC-SQLi-W03
- `WarehousesStatusSqliTest.kt` — TC-SQLi-W04
- `TimeBasedSqliTest.kt` — TC-SQLi-W05

**Total de 36 testes executados, todos passaram.**