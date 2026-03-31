# SECURITY_SPEC.md — SQL Injection

> Spec dedicada de testes de segurança para SQL Injection.
> Execute com: `opencode "run SECURITY_SPEC.md"`
> Gera automaticamente `security-report.md` ao finalizar.

---

## Contexto

- **Stack**: Java 21 + Spring Boot 3, PostgreSQL
- **Auth**: JWT Bearer token
- **Base URL**: `/api` (ajustar conforme ambiente)
- **Escopo desta spec**: SQL Injection em todas as rotas de Products e Warehouses

---

## Instruções para o agente

1. Para cada caso de teste, criar o arquivo indicado em `src/test/java/.../security/sqli/`
2. Usar `MockMvc` + `@SpringBootTest` ou `@WebMvcTest` conforme o caso
3. Usar `TestContainers` com PostgreSQL real — **não usar H2**
4. **Nunca** alterar código de produção para forçar PASS
5. Se encontrar `FAIL`:
   - Localizar a linha vulnerável no repositório/service
   - Aplicar correção (prepared statement / Spring Data parametrizado)
   - Re-executar e confirmar PASS
6. Ao finalizar todos os casos, gerar `security-report.md` na raiz do projeto

---

## Setup de teste

### Dependências necessárias (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

### `application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  jpa:
    show-sql: true
  server:
    error:
      include-stacktrace: never
      include-message: never
```

### Base class de teste

```java
// src/test/java/.../security/SqliTestBase.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class SqliTestBase {

    @Autowired
    protected MockMvc mockMvc;

    // Payloads reutilizados em todos os testes
    protected static final String[] CLASSIC_PAYLOADS = {
        "' OR '1'='1",
        "' OR '1'='1'--",
        "' OR 1=1#",
        "\" OR \"1\"=\"1"
    };

    protected static final String[] BLIND_PAYLOADS = {
        "1 AND 1=1--",
        "1 AND 1=2--"
    };

    protected static final String[] TIME_PAYLOADS = {
        "'; SELECT pg_sleep(5)--",
        "' AND pg_sleep(3)=0--",
        "'; SELECT CASE WHEN (1=1) THEN pg_sleep(5) ELSE pg_sleep(0) END--"
    };

    protected static final String[] UNION_PAYLOADS = {
        "1 UNION SELECT NULL--",
        "1 UNION SELECT NULL,NULL--",
        "1 UNION SELECT NULL,NULL,NULL--",
        "1 UNION SELECT table_name,NULL FROM information_schema.tables--",
        "1 UNION SELECT username,password FROM users--"
    };

    protected static final String[] ERROR_PAYLOADS = {
        "'",
        "1'",
        "1 AND EXTRACTVALUE(1,CONCAT(0x7e,(SELECT version())))--",
        "1; SELECT 1/0--"
    };

    protected static final String[] FORBIDDEN_TERMS = {
        "postgresql", "syntax error", "org.hibernate", "jdbc",
        "at com.", "stacktrace", "information_schema", "pg_catalog",
        "column", "relation", "table"
    };
}
```

---

## Casos de teste — Products

---

### TC-SQLi-P01 — `GET /products/{id}` — Path variable injection

**Severidade**: Crítico
**Arquivo**: `ProductsIdSqliTest.java`

**Descrição**: O `{id}` é um path variable numérico. Testa se valores não numéricos com payloads SQL são tratados como tipo inválido antes de chegar ao banco.

**Payloads**:
```
/products/' OR '1'='1
/products/1 OR 1=1--
/products/1; DROP TABLE products--
/products/1 UNION SELECT username,password FROM users--
```

**Critério**:
- `PASS`: HTTP 400 ou 404 para todos os payloads; body sem detalhes técnicos
- `FAIL`: HTTP 200 com dados, ou body com stack trace / termos SQL internos

```java
@Test
void sqli_getProductById_pathVariable_shouldRejectInjection() throws Exception {
    String[] maliciousPaths = {
        "' OR '1'='1",
        "1 OR 1=1--",
        "1; DROP TABLE products--",
        "1 UNION SELECT username,password FROM users--"
    };

    for (String path : maliciousPaths) {
        MvcResult result = mockMvc.perform(get("/products/" + path))
            .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();

        assertThat(status).as("Payload: %s", path).isIn(400, 404);

        for (String term : FORBIDDEN_TERMS) {
            assertThat(body).as("Body não deve expor '%s' para payload: %s", term, path)
                .doesNotContainIgnoringCase(term);
        }
    }
}
```

---

### TC-SQLi-P02 — `GET /products?category=` — Query param injection (filtro de categoria)

**Severidade**: Crítico
**Arquivo**: `ProductsListSqliTest.java`

**Descrição**: O parâmetro `category` é usado em cláusula WHERE. É um vetor clássico para union-based e blind injection.

**Payloads**:
```
category=' OR '1'='1
category=electronics' OR '1'='1'--
category=1 UNION SELECT table_name,NULL FROM information_schema.tables--
category=1 AND SUBSTRING((SELECT version()),1,1)='P'--
```

**Critério**:
- `PASS`: HTTP 400 ou lista vazia `[]`; nenhum dado de outras tabelas no body
- `FAIL`: Lista com dados reais para payload OR, ou dados de `information_schema`

```java
@ParameterizedTest
@ValueSource(strings = {
    "' OR '1'='1",
    "electronics' OR '1'='1'--",
    "1 UNION SELECT table_name,NULL FROM information_schema.tables--",
    "1 AND SUBSTRING((SELECT version()),1,1)='P'--"
})
void sqli_listProducts_categoryParam_shouldNotInject(String payload) throws Exception {
    MvcResult result = mockMvc.perform(get("/products")
            .param("category", payload))
        .andReturn();

    String body = result.getResponse().getContentAsString();

    for (String term : FORBIDDEN_TERMS) {
        assertThat(body).doesNotContainIgnoringCase(term);
    }

    // Se retornar 200, deve ser lista vazia — nunca dados reais via payload
    if (result.getResponse().getStatus() == 200) {
        assertThat(body).isEqualTo("[]").as("Payload não deve retornar dados reais");
    }
}
```

---

### TC-SQLi-P03 — `GET /products?status=` — Query param injection (filtro de status)

**Severidade**: Alto
**Arquivo**: `ProductsListSqliTest.java` (mesmo arquivo do P02)

**Descrição**: `status` aceita valores `ACTIVE` ou `INACTIVE`. Testa se valores fora do enum com payloads SQL são rejeitados antes do banco.

**Payloads**:
```
status=ACTIVE' OR '1'='1'--
status=' UNION SELECT username,password FROM users--
status=ACTIVE; DROP TABLE products--
```

**Critério**:
- `PASS`: HTTP 400 para qualquer valor fora de `ACTIVE`/`INACTIVE`
- `FAIL`: HTTP 200 com dados, ou erro técnico exposto

```java
@ParameterizedTest
@ValueSource(strings = {
    "ACTIVE' OR '1'='1'--",
    "' UNION SELECT username,password FROM users--",
    "ACTIVE; DROP TABLE products--",
    "INVALID_VALUE"
})
void sqli_listProducts_statusParam_shouldValidateEnum(String payload) throws Exception {
    mockMvc.perform(get("/products")
            .param("status", payload))
        .andExpect(status().isBadRequest());
}
```

---

### TC-SQLi-P04 — `GET /products?category=&status=` — Combinação de parâmetros

**Severidade**: Alto
**Arquivo**: `ProductsListSqliTest.java`

**Descrição**: Testa injection quando múltiplos filtros são combinados — a query gerada pode ser mais complexa e criar vetores adicionais.

**Payloads**:
```
category=electronics&status=ACTIVE' OR '1'='1'--
category=' OR 1=1--&status=ACTIVE
category=1&status=ACTIVE UNION SELECT NULL,NULL--
```

**Critério**:
- `PASS`: HTTP 400 ou lista vazia; sem dados externos
- `FAIL`: Dados de outras tabelas ou erro técnico exposto

```java
@Test
void sqli_listProducts_combinedParams_shouldNotInject() throws Exception {
    mockMvc.perform(get("/products")
            .param("category", "electronics")
            .param("status", "ACTIVE' OR '1'='1'--"))
        .andExpect(status().isBadRequest());

    MvcResult result = mockMvc.perform(get("/products")
            .param("category", "' OR 1=1--")
            .param("status", "ACTIVE"))
        .andReturn();

    String body = result.getResponse().getContentAsString();
    for (String term : FORBIDDEN_TERMS) {
        assertThat(body).doesNotContainIgnoringCase(term);
    }
}
```

---

### TC-SQLi-P05 — `POST /products` — Body injection no cadastro

**Severidade**: Alto
**Arquivo**: `ProductsCreateSqliTest.java`

**Descrição**: Campos do body (`name`, `category`, `description`) podem ser usados em queries subsequentes. Testa injection direta e second-order.

**Payloads no body**:
```json
{ "name": "produto'; DROP TABLE products;--", "category": "test" }
{ "name": "produto' OR '1'='1", "category": "test" }
{ "category": "' UNION SELECT username,password FROM users--" }
```

**Critério**:
- `PASS`: HTTP 201 com dados sanitizados armazenados literalmente, ou HTTP 400 se validação rejeitar
- `FAIL`: HTTP 500, stack trace, ou operações colaterais no banco

```java
@ParameterizedTest
@MethodSource("bodyInjectionPayloads")
void sqli_createProduct_bodyFields_shouldSanitizeOrReject(String name, String category) throws Exception {
    String body = String.format(
        "{\"name\":\"%s\",\"category\":\"%s\",\"price\":10.0,\"status\":\"ACTIVE\"}",
        name, category
    );

    MvcResult result = mockMvc.perform(post("/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andReturn();

    int status = result.getResponse().getStatus();

    // Deve aceitar como string literal (201) ou rejeitar (400) — nunca 500
    assertThat(status).as("Payload: name=%s", name).isIn(201, 400);

    String responseBody = result.getResponse().getContentAsString();
    for (String term : FORBIDDEN_TERMS) {
        assertThat(responseBody).doesNotContainIgnoringCase(term);
    }
}

static Stream<Arguments> bodyInjectionPayloads() {
    return Stream.of(
        Arguments.of("produto'; DROP TABLE products;--", "test"),
        Arguments.of("produto' OR '1'='1", "test"),
        Arguments.of("normal", "' UNION SELECT username,password FROM users--"),
        Arguments.of("test'--", "electronics")
    );
}
```

---

### TC-SQLi-P06 — `PATCH /products/{id}` — Update injection

**Severidade**: Alto
**Arquivo**: `ProductsUpdateSqliTest.java`

**Descrição**: Testa injection no path variable `{id}` e nos campos do body durante atualização. Risco de second-order se o nome atualizado for usado em queries futuras.

**Critério**:
- `PASS`: HTTP 400/404 para id inválido; HTTP 200/400 para body com payload armazenado literalmente
- `FAIL`: HTTP 500, dados de outros produtos afetados, ou stack trace

```java
@Test
void sqli_updateProduct_pathId_shouldRejectNonNumeric() throws Exception {
    mockMvc.perform(patch("/products/' OR '1'='1'--")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"novo nome\"}"))
        .andExpect(status().isBadRequest());
}

@Test
void sqli_updateProduct_body_shouldNotAffectOtherRecords(
        @Autowired ProductRepository repo) throws Exception {

    // Setup: dois produtos no banco
    Long targetId = createProduct("produto-alvo");
    Long otherId  = createProduct("produto-outro");

    // Tenta update com payload no nome
    mockMvc.perform(patch("/products/" + targetId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"test'; UPDATE products SET name='HACKED' WHERE '1'='1'--\"}"))
        .andReturn();

    // O outro produto deve estar intacto
    Product other = repo.findById(otherId).orElseThrow();
    assertThat(other.getName()).isEqualTo("produto-outro");
}
```

---

### TC-SQLi-P07 — `DELETE /products/{id}` — Delete injection (mass delete)

**Severidade**: Crítico
**Arquivo**: `ProductsDeleteSqliTest.java`

**Descrição**: Injection no `{id}` do DELETE pode resultar em deleção em massa de registros se a query não for parametrizada.

**Payloads**:
```
DELETE /products/1 OR 1=1--
DELETE /products/1; DELETE FROM products WHERE 1=1--
DELETE /products/' OR '1'='1'--
```

**Critério**:
- `PASS`: HTTP 400/404 para payloads; apenas o produto alvo deletado quando id válido
- `FAIL`: HTTP 200 deletando múltiplos registros, ou stack trace

```java
@Test
void sqli_deleteProduct_shouldNotMassDelete(@Autowired ProductRepository repo) throws Exception {
    Long id1 = createProduct("produto-1");
    Long id2 = createProduct("produto-2");
    Long id3 = createProduct("produto-3");

    // Tenta mass delete via injection
    mockMvc.perform(delete("/products/1 OR 1=1--"))
        .andExpect(status().isBadRequest());

    // Todos os produtos devem continuar existindo
    assertThat(repo.count()).isGreaterThanOrEqualTo(3);
}
```

---

## Casos de teste — Warehouses

---

### TC-SQLi-W01 — `GET /warehouses/{id}` — Path variable injection

**Severidade**: Crítico
**Arquivo**: `WarehousesIdSqliTest.java`

**Descrição**: Mesmo padrão do P01, aplicado ao endpoint de warehouses.

```java
@ParameterizedTest
@ValueSource(strings = {
    "' OR '1'='1",
    "1 OR 1=1--",
    "1 UNION SELECT username,password FROM users--",
    "1; DROP TABLE warehouses--"
})
void sqli_getWarehouseById_shouldRejectInjection(String payload) throws Exception {
    MvcResult result = mockMvc.perform(get("/warehouses/" + payload))
        .andReturn();

    int status = result.getResponse().getStatus();
    String body = result.getResponse().getContentAsString();

    assertThat(status).isIn(400, 404);
    for (String term : FORBIDDEN_TERMS) {
        assertThat(body).doesNotContainIgnoringCase(term);
    }
}
```

---

### TC-SQLi-W02 — `GET /warehouses?active=` — Query param injection (filtro boolean)

**Severidade**: Alto
**Arquivo**: `WarehousesListSqliTest.java`

**Descrição**: `active` é um booleano. Testa se valores não booleanos com SQL são rejeitados por validação de tipo antes de chegar ao banco.

**Payloads**:
```
active=true' OR '1'='1'--
active=1 UNION SELECT name,location FROM warehouses--
active=' OR 1=1--
active=true; DROP TABLE warehouses--
```

**Critério**:
- `PASS`: HTTP 400 para qualquer valor não booleano
- `FAIL`: HTTP 200 com dados, ou erro técnico exposto

```java
@ParameterizedTest
@ValueSource(strings = {
    "true' OR '1'='1'--",
    "1 UNION SELECT name,NULL FROM warehouses--",
    "' OR 1=1--",
    "true; DROP TABLE warehouses--",
    "not_a_boolean"
})
void sqli_listWarehouses_activeParam_shouldValidateBoolean(String payload) throws Exception {
    MvcResult result = mockMvc.perform(get("/warehouses")
            .param("active", payload))
        .andReturn();

    // Tipo inválido deve ser rejeitado como 400
    assertThat(result.getResponse().getStatus()).isEqualTo(400);

    String body = result.getResponse().getContentAsString();
    for (String term : FORBIDDEN_TERMS) {
        assertThat(body).doesNotContainIgnoringCase(term);
    }
}
```

---

### TC-SQLi-W03 — `POST /warehouses` — Body injection no cadastro

**Severidade**: Alto
**Arquivo**: `WarehousesCreateSqliTest.java`

**Descrição**: Campos como `name` e `location` de warehouses podem ser vetores de second-order injection se usados em queries futuras sem parametrização.

```java
@ParameterizedTest
@MethodSource("warehouseBodyPayloads")
void sqli_createWarehouse_bodyFields_shouldSanitizeOrReject(String name) throws Exception {
    String body = String.format(
        "{\"name\":\"%s\",\"location\":\"São Paulo\",\"active\":true}", name
    );

    MvcResult result = mockMvc.perform(post("/warehouses")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andReturn();

    int status = result.getResponse().getStatus();
    assertThat(status).as("Payload: %s", name).isIn(201, 400);

    String responseBody = result.getResponse().getContentAsString();
    for (String term : FORBIDDEN_TERMS) {
        assertThat(responseBody).doesNotContainIgnoringCase(term);
    }
}

static Stream<String> warehouseBodyPayloads() {
    return Stream.of(
        "CD São Paulo'; DROP TABLE warehouses;--",
        "CD' OR '1'='1",
        "'; INSERT INTO warehouses (name) VALUES ('hacked');--",
        "CD' UNION SELECT username,password FROM users--"
    );
}
```

---

### TC-SQLi-W04 — `PATCH /warehouses/{id}/status` — Status update injection

**Severidade**: Alto
**Arquivo**: `WarehousesStatusSqliTest.java`

**Descrição**: Endpoint especializado de alteração de status. Testa injection no `{id}` e no body do novo status.

**Critério**:
- `PASS`: HTTP 400/404 para id com payload; HTTP 400 para status fora do domínio
- `FAIL`: Status de outros warehouses alterado, ou stack trace

```java
@Test
void sqli_updateWarehouseStatus_pathId_shouldRejectInjection() throws Exception {
    mockMvc.perform(patch("/warehouses/1 OR 1=1--/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"active\":false}"))
        .andExpect(status().isBadRequest());
}

@Test
void sqli_updateWarehouseStatus_body_shouldNotAffectOthers(
        @Autowired WarehouseRepository repo) throws Exception {

    Long targetId  = createWarehouse("CD-alvo",  true);
    Long otherId   = createWarehouse("CD-outro", true);

    // Payload que tenta desativar todos
    mockMvc.perform(patch("/warehouses/" + targetId + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"active\":false,\"sql\":\"'; UPDATE warehouses SET active=false WHERE '1'='1'--\"}"))
        .andReturn();

    // Outro warehouse deve continuar ativo
    Warehouse other = repo.findById(otherId).orElseThrow();
    assertThat(other.isActive()).isTrue();
}
```

---

### TC-SQLi-W05 — Time-based em listagem paginada (Products e Warehouses)

**Severidade**: Crítico
**Arquivo**: `TimeBasedSqliTest.java`

**Descrição**: Parâmetros de paginação `page` e `size` são numéricos mas podem ser vetores de time-based injection se não validados por tipo.

**Payloads**:
```
/products?page=0&size='; SELECT pg_sleep(5)--
/products?page=0 AND pg_sleep(3)=0&size=20
/warehouses?page=0&size=20&active=true' AND pg_sleep(3)=0--
```

**Critério**:
- `PASS`: Resposta < 1000ms para todos os payloads; HTTP 400 para valores não numéricos
- `FAIL`: Resposta ≥ 3000ms — confirma execução do pg_sleep

```java
@ParameterizedTest
@MethodSource("paginationPayloads")
void sqli_pagination_timeBased_shouldNotDelay(String url) throws Exception {
    long start = System.currentTimeMillis();

    mockMvc.perform(get(url)).andReturn();

    long elapsed = System.currentTimeMillis() - start;
    assertThat(elapsed)
        .as("URL: %s — não deve atrasar por pg_sleep", url)
        .isLessThan(1000L);
}

static Stream<String> paginationPayloads() {
    return Stream.of(
        "/products?page=0&size='; SELECT pg_sleep(5)--",
        "/products?page=0 AND pg_sleep(3)=0&size=20",
        "/warehouses?page=0&size=20&active=true' AND pg_sleep(3)=0--",
        "/warehouses?size=20 UNION SELECT pg_sleep(5),NULL--"
    );
}
```

---

## Checklist de mitigação — revisão de código

O agente deve varrer o codebase e verificar todos os itens abaixo:

### Repositórios e queries

- [ ] Nenhuma concatenação de string em SQL: `"WHERE id = " + id` → **FAIL imediato**
- [ ] `@Query` do Spring Data usa `:param` ou `?1`, nunca `String.format` ou `+`
- [ ] `JdbcTemplate` usa `?` com array de parâmetros, nunca string formatada
- [ ] Queries nativas (`nativeQuery = true`) são parametrizadas da mesma forma

### Validação de entrada

- [ ] Path variables numéricas anotadas com `@PathVariable Long id` — Spring rejeita não numérico automaticamente
- [ ] Query params de enum (`status`) validados com `@RequestParam ProductStatus status` — rejeita valores inválidos
- [ ] Query params booleanos (`active`) com `@RequestParam Boolean active`
- [ ] Parâmetros `page` e `size` com `@RequestParam @Min(0) int page` e `@RequestParam @Max(100) int size`

### Tratamento de erros

- [ ] `@ControllerAdvice` global captura todas as exceções
- [ ] Nenhum `e.getMessage()` ou `e.getStackTrace()` exposto no body da resposta
- [ ] `spring.jpa.show-sql=false` e `spring.jpa.properties.hibernate.format_sql=false` em produção
- [ ] `server.error.include-stacktrace=never` e `server.error.include-message=never`

### Privilégios do banco

- [ ] Usuário do banco usado pela aplicação **não tem** permissões de DROP, CREATE, ALTER
- [ ] Usuário do banco **não tem** acesso a `information_schema` além do necessário
- [ ] Conexão usa usuário com apenas SELECT, INSERT, UPDATE, DELETE nas tabelas da aplicação

---

## Output esperado

Ao finalizar todos os casos, gerar `security-report.md`:

```markdown
# Security report — SQL Injection

**Data**: <data>
**Ambiente**: <dev/staging>
**Executado por**: OpenCode Agent

## Sumário

| Caso | Endpoint | Tipo | Resultado | Correção aplicada |
|------|----------|------|-----------|-------------------|
| TC-SQLi-P01 | GET /products/{id} | Path variable | PASS/FAIL | — |
| TC-SQLi-P02 | GET /products?category= | Query param | PASS/FAIL | — |
| TC-SQLi-P03 | GET /products?status= | Enum param | PASS/FAIL | — |
| TC-SQLi-P04 | GET /products?category=&status= | Multi-param | PASS/FAIL | — |
| TC-SQLi-P05 | POST /products | Body / second-order | PASS/FAIL | — |
| TC-SQLi-P06 | PATCH /products/{id} | Update | PASS/FAIL | — |
| TC-SQLi-P07 | DELETE /products/{id} | Mass delete | PASS/FAIL | — |
| TC-SQLi-W01 | GET /warehouses/{id} | Path variable | PASS/FAIL | — |
| TC-SQLi-W02 | GET /warehouses?active= | Boolean param | PASS/FAIL | — |
| TC-SQLi-W03 | POST /warehouses | Body / second-order | PASS/FAIL | — |
| TC-SQLi-W04 | PATCH /warehouses/{id}/status | Status update | PASS/FAIL | — |
| TC-SQLi-W05 | Paginação (ambos) | Time-based | PASS/FAIL | — |

## Vulnerabilidades encontradas

<arquivo> : <linha> — <descrição> — <payload que triggou> — <correção aplicada>

## Checklist de mitigação

<lista dos itens verificados com status>

## Conclusão

[ ] Todos os casos PASS — liberado para staging
[ ] FAILs encontrados — corrigidos e re-testados
```