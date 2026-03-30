**Nota:** Este arquivo deve ser lido automaticamente no início de cada sessão para entender os padrões do projeto.

# Arquitetura e Boas Práticas — Kotlin

## Visão Geral

Este documento descreve a arquitetura, padrões e boas práticas para projetos Kotlin seguindo os mesmos princípios da versão TypeScript/Fastify, adaptados para o ecossistema JVM com Spring Boot.

---

## Stack Tecnológica

| Categoria    | Tecnologia                              |
|--------------|-----------------------------------------|
| Runtime      | JVM 21 (Eclipse Temurin)                |
| Linguagem    | Kotlin (strict null safety)             |
| API          | Spring Boot 4 + Spring Web MVC          |
| ORM          | Spring Data JPA + Hibernate             |
| Validação    | Jakarta Bean Validation + Spring        |
| Database     | PostgreSQL                              |
| Cache        | Redis (Spring Cache / Lettuce)          |
| Testes       | JUnit 5 + Mockk + SpringBootTest        |
| Linting      | Ktlint / Detekt                         |
| Build        | Gradle (Kotlin DSL)                     |

---

## 1. Arquitetura Geral

O projeto segue os princípios da **Clean Architecture**, organizada em camadas de baixo acoplamento e alta coesão:

Leia '@docs/specs', para construção dos módulos.


```
src/main/kotlin/com/seuapp/
├── domain/           # Regras de negócio (núcleo)
├── application/      # Casos de uso
├── infrastructure/   # Implementações externas
└── shared/           # Código compartilhado
```

### Princípios Fundamentais

- **Domain Layer**: Entidades, value objects e interfaces de repositórios. Zero dependências externas.
- **Application Layer**: Casos de uso que orquestram o domínio. Depende apenas do domain.
- **Infrastructure Layer**: Repositórios JPA, controllers REST e adapters. Depende do domain e application.
- **Shared Layer**: Código reutilizável — erros, utils, constantes, DI simples.

---

## 2. Estrutura de Diretórios

### Domain Layer

```
domain/
├── entities/
│   ├── user/
│   │   ├── UserEntity.kt
│   │   ├── UserRepository.kt       # Interface (contrato)
│   │   ├── Permission.kt           # Enum de permissões
│   │   └── value_objects/
│   │       ├── Password.kt
│   │       ├── Username.kt
│   │       └── Email.kt
│   ├── customer/
│   ├── product/
│   ├── sale/
│   └── stock/
└── shared/
    ├── Entity.kt                   # Classe base para entidades
    └── ValueObject.kt              # Classe base para value objects
```

### Application Layer

```
application/
└── use_cases/
    ├── user/
    │   ├── CreateUser.kt
    │   ├── GetUser.kt
    │   ├── ListUsers.kt
    │   └── ...
    ├── customer/
    ├── product/
    └── ...
```

### Infrastructure Layer

```
infrastructure/
├── database/
│   ├── jpa/
│   │   ├── entities/               # @Entity JPA (mapeamento DB)
│   │   │   ├── UserJpaEntity.kt
│   │   │   └── ...
│   │   └── repositories/          # Spring Data JPA interfaces
│   │       └── UserJpaRepository.kt
│   ├── repositories/
│   │   └── UserRepositoryImpl.kt  # Implementação do contrato de domínio
│   └── seeds/
└── http/
    └── spring/
        ├── controllers/
        ├── routes/                 # @RestController com @RequestMapping
        ├── middlewares/            # Filtros / interceptors
        └── config/                 # Beans de configuração Spring
```

### Shared Layer

```
shared/
├── constants/
│   └── HttpStatus.kt
├── container/
│   └── Repositories.kt            # DI simples (singletons manuais ou @Bean)
├── errors/
│   ├── BaseError.kt
│   └── DomainErrors.kt            # NotFoundError, ValidationError, etc.
├── types/
├── utils/
│   └── Env.kt
└── validators/
    └── CommonValidators.kt
```

---

## 3. Padrões de Código

### 3.1 Entities

```kotlin
// domain/shared/Entity.kt
abstract class Entity<T>(
    protected val props: T,
    id: String? = null
) {
    val id: String = id ?: java.util.UUID.randomUUID().toString()
    var createdAt: java.time.Instant = java.time.Instant.now()
        protected set
    var updatedAt: java.time.Instant = java.time.Instant.now()
        protected set

    protected fun touch() {
        updatedAt = java.time.Instant.now()
    }

    fun equals(entity: Entity<T>?): Boolean {
        if (entity == null) return false
        return this.id == entity.id
    }
}
```

**Implementação de uma Entity:**

```kotlin
// domain/entities/user/UserEntity.kt
data class UserProps(
    val name: String,
    val username: Username,
    val email: Email,
    val password: Password,
    val role: UserRole,
    var status: UserStatus = UserStatus.ACTIVE,
)

class User private constructor(
    props: UserProps,
    id: String? = null
) : Entity<UserProps>(props, id) {

    val name: String get() = props.name
    val email: Email get() = props.email
    val username: Username get() = props.username
    val password: Password get() = props.password
    val role: UserRole get() = props.role
    val status: UserStatus get() = props.status

    fun activate() {
        props.status = UserStatus.ACTIVE
        touch()
    }

    fun isActive(): Boolean = props.status == UserStatus.ACTIVE

    companion object {
        fun create(props: UserProps, id: String? = null): User {
            require(props.name.trim().isNotEmpty()) { "User name is required" }
            require(props.name.trim().length >= 3) { "User name must be at least 3 characters" }
            return User(
                props = props.copy(name = props.name.trim()),
                id = id
            )
        }
    }
}

enum class UserStatus { ACTIVE, INACTIVE, BLOCKED }
enum class UserRole { ADMIN, MANAGER, SALESPERSON, STOCK_MANAGER, VIEWER }
```

### 3.2 Value Objects

```kotlin
// domain/shared/ValueObject.kt
abstract class ValueObject<T>(protected val props: T) {
    fun equals(vo: ValueObject<T>?): Boolean {
        if (vo == null) return false
        return this.props == vo.props
    }
}
```

**Implementação de um Value Object:**

```kotlin
// domain/entities/user/value_objects/Email.kt
class Email private constructor(private val value: String) : ValueObject<String>(value) {

    fun getValue(): String = value

    fun getDomain(): String = value.split("@")[1]

    companion object {
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        fun create(raw: String): Email {
            val trimmed = raw.trim().lowercase()
            require(trimmed.isNotEmpty()) { "Email cannot be empty" }
            require(EMAIL_REGEX.matches(trimmed)) { "Invalid email format" }
            return Email(trimmed)
        }
    }
}
```

### 3.3 Interfaces de Repositório (Domain)

```kotlin
// domain/entities/user/UserRepository.kt
interface IUserRepository {
    fun save(user: User)
    fun findById(id: String): User?
    fun findByUsername(username: String): User?
    fun findByEmail(email: String): User?
    fun findAll(filters: UserFilters? = null): List<User>
    fun update(user: User)
    fun delete(id: String)
    fun exists(id: String): Boolean
}

data class UserFilters(
    val status: String? = null,
    val role: String? = null,
    val searchTerm: String? = null,
)
```

### 3.4 Implementação de Repositório (Infrastructure)

```kotlin
// infrastructure/database/repositories/UserRepositoryImpl.kt
@Component
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository
) : IUserRepository {

    override fun save(user: User) {
        jpaRepository.save(user.toJpaEntity())
    }

    override fun findById(id: String): User? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? =
        jpaRepository.findByEmail(email)?.toDomain()

    private fun UserJpaEntity.toDomain(): User =
        User.create(
            UserProps(
                name = this.name,
                username = Username.create(this.username),
                email = Email.create(this.email),
                password = Password.fromHash(this.password),
                role = UserRole.valueOf(this.role),
                status = UserStatus.valueOf(this.status),
            ),
            id = this.id
        )

    private fun User.toJpaEntity(): UserJpaEntity =
        UserJpaEntity(
            id = this.id,
            name = this.name,
            username = this.username.getValue(),
            email = this.email.getValue(),
            password = this.password.hash,
            role = this.role.name,
            status = this.status.name,
        )
}
```

### 3.5 Entidade JPA (Infrastructure)

```kotlin
// infrastructure/database/jpa/entities/UserJpaEntity.kt
@Entity
@Table(name = "users")
data class UserJpaEntity(
    @Id
    val id: String,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(nullable = false, unique = true, length = 50)
    val username: String,

    @Column(nullable = false, unique = true, length = 200)
    val email: String,

    @Column(nullable = false)
    val password: String,

    @Column(nullable = false)
    val role: String,

    @Column(nullable = false)
    val status: String = "ACTIVE",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: java.time.Instant = java.time.Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: java.time.Instant = java.time.Instant.now(),
)
```

### 3.6 Casos de Uso (Application)

```kotlin
// application/use_cases/user/CreateUser.kt
data class CreateUserInput(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val role: UserRole,
)

data class CreateUserOutput(
    val id: String,
    val username: String,
    val email: String,
)

class CreateUserUseCase(
    private val userRepo: IUserRepository
) {
    fun execute(input: CreateUserInput): CreateUserOutput {
        val existingByEmail = userRepo.findByEmail(input.email)
        if (existingByEmail != null) throw ConflictError("Email already exists")

        val existingByUsername = userRepo.findByUsername(input.username)
        if (existingByUsername != null) throw ConflictError("Username already exists")

        val user = User.create(
            UserProps(
                name = input.name,
                username = Username.create(input.username),
                email = Email.create(input.email),
                password = Password.create(input.password),
                role = input.role,
            )
        )

        userRepo.save(user)

        return CreateUserOutput(
            id = user.id,
            username = user.username.getValue(),
            email = user.email.getValue(),
        )
    }
}
```

---

## 4. Padrões de API (Spring Web MVC)

### 4.1 Controller

```kotlin
// infrastructure/http/spring/controllers/UserController.kt
@RestController
@RequestMapping("/users")
class UserController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody @Valid body: CreateUserRequest): CreateUserResponse {
        val useCase = CreateUserUseCase(getUserRepository())
        val result = useCase.execute(body.toInput())
        return CreateUserResponse(
            id = result.id,
            username = result.username,
            email = result.email,
        )
    }
}
```

### 4.2 Request / Response DTOs

```kotlin
// infrastructure/http/spring/controllers/UserDTOs.kt
data class CreateUserRequest(
    @field:NotBlank val name: String,
    @field:NotBlank @field:Size(min = 3, max = 50) val username: String,
    @field:Email val email: String,
    @field:NotBlank val password: String,
    val role: UserRole,
) {
    fun toInput() = CreateUserInput(
        name = name,
        username = username,
        email = email,
        password = password,
        role = role,
    )
}

data class CreateUserResponse(
    val id: String,
    val username: String,
    val email: String,
)
```

---

## 5. Sistema de Permissões

```kotlin
// domain/entities/user/Permission.kt
enum class Permission(val value: String) {
    MANAGE_USERS("manage_users"),
    MANAGE_PRODUCTS("manage_products"),
    MANAGE_CUSTOMERS("manage_customers"),
    MANAGE_STOCK("manage_stock"),
    CREATE_SALE("create_sale"),
    APPROVE_SALE("approve_sale"),
    VIEW_REPORTS("view_reports"),
}
```

### Roles e Permissões

| Role          | Permissões                                                           |
|---------------|----------------------------------------------------------------------|
| ADMIN         | Todas                                                                |
| MANAGER       | manage_products, manage_customers, create_sale, approve_sale, view_reports |
| SALESPERSON   | manage_customers, create_sale                                        |
| STOCK_MANAGER | manage_products, manage_stock, view_reports                          |
| VIEWER        | Nenhuma                                                              |

---

## 6. Inversão de Dependência

DI simples via `@Bean` no Spring — sem container IoC adicional:

```kotlin
// shared/container/Repositories.kt
@Configuration
class RepositoriesConfig {

    @Bean
    fun userRepository(jpaRepository: UserJpaRepository): IUserRepository =
        UserRepositoryImpl(jpaRepository)
}
```

Ou injeção direta nos casos de uso via construtor (sem Spring no application layer):

```kotlin
// Use cases recebem a interface, não a implementação
val useCase = CreateUserUseCase(getUserRepository())
```

---

## 7. Tratamento de Erros

```kotlin
// shared/errors/BaseError.kt
open class BaseError(
    message: String,
    val statusCode: Int,
    val details: Any? = null,
) : RuntimeException(message)
```

```kotlin
// shared/errors/DomainErrors.kt
class NotFoundError(entity: String, id: String? = null) : BaseError(
    message = "$entity${if (id != null) " com id $id" else ""} não encontrado",
    statusCode = 404,
)

class ValidationError(message: String, details: Any? = null) : BaseError(
    message = message,
    statusCode = 400,
    details = details,
)

class UnauthorizedError(message: String = "Acesso não autorizado") : BaseError(
    message = message,
    statusCode = 401,
)

class ForbiddenError(message: String = "Acesso proibido") : BaseError(
    message = message,
    statusCode = 403,
)

class ConflictError(message: String) : BaseError(
    message = message,
    statusCode = 409,
)
```

### Global Exception Handler

```kotlin
// infrastructure/http/spring/config/GlobalExceptionHandler.kt
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BaseError::class)
    fun handleDomainError(ex: BaseError): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ex.statusCode)
            .body(ErrorResponse(error = ex.message ?: "Erro interno", details = ex.details))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(400)
            .body(ErrorResponse(error = "Erro de validação", details = details))
    }
}

data class ErrorResponse(val error: String, val details: Any? = null)
```

---

## 8. Testes

### Framework: JUnit 5 + Mockk

Os testes ficam no **mesmo pacote** espelhado em `src/test/kotlin/`:

```
src/
├── main/kotlin/com/seuapp/domain/entities/user/UserEntity.kt
└── test/kotlin/com/seuapp/domain/entities/user/UserEntityTest.kt
```

### Padrão de Teste de Entidade

```kotlin
// UserEntityTest.kt
class UserEntityTest {

    private lateinit var validProps: UserProps

    @BeforeEach
    fun setup() {
        validProps = UserProps(
            name = "John Doe",
            username = Username.create("johndoe"),
            email = Email.create("john@example.com"),
            password = Password.fromHash("hash123"),
            role = UserRole.ADMIN,
        )
    }

    @Test
    fun `should create user with default ACTIVE status`() {
        val user = User.create(validProps)
        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `should throw error if name is empty`() {
        val ex = assertThrows<IllegalArgumentException> {
            User.create(validProps.copy(name = ""))
        }
        assertEquals("User name is required", ex.message)
    }
}
```

### Padrão de Teste de Caso de Uso

```kotlin
// CreateUserTest.kt
class CreateUserTest {

    private val userRepo: IUserRepository = mockk()

    @Test
    fun `should create user successfully`() {
        every { userRepo.findByEmail(any()) } returns null
        every { userRepo.findByUsername(any()) } returns null
        every { userRepo.save(any()) } just Runs

        val useCase = CreateUserUseCase(userRepo)
        val result = useCase.execute(
            CreateUserInput("John", "johndoe", "john@example.com", "pass123", UserRole.ADMIN)
        )

        assertNotNull(result.id)
        assertEquals("johndoe", result.username)
        verify(exactly = 1) { userRepo.save(any()) }
    }

    @Test
    fun `should throw ConflictError if email already exists`() {
        every { userRepo.findByEmail("john@example.com") } returns mockk()

        val useCase = CreateUserUseCase(userRepo)
        assertThrows<ConflictError> {
            useCase.execute(
                CreateUserInput("John", "johndoe", "john@example.com", "pass123", UserRole.ADMIN)
            )
        }
    }
}
```

---

## 9. Schema do Banco de Dados (Flyway)

Migrações versionadas em `src/main/resources/db/migration/`:

```sql
-- V1__create_users_table.sql
CREATE TYPE user_role AS ENUM ('ADMIN', 'MANAGER', 'SALESPERSON', 'STOCK_MANAGER', 'VIEWER');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED');

CREATE TABLE users (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(200) NOT NULL UNIQUE,
    password    TEXT         NOT NULL,
    role        user_role    NOT NULL,
    status      user_status  NOT NULL DEFAULT 'ACTIVE',
    phone       VARCHAR(20),
    last_login_at TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

---

## 10. Segurança

- **Rate Limiting**: Bucket4j ou Spring Cloud Gateway
- **CORS**: `@CrossOrigin` / `CorsConfigurationSource` via variável de ambiente
- **JWT**: Spring Security + `jjwt` library
- **Password Hashing**: BCrypt via `spring-security-crypto`
- **Permissions**: Sistema de permissões granulares por role
- **Headers de segurança**: Spring Security padrão

---

## 11. Nomenclatura

| Categoria               | Padrão                           |
|-------------------------|----------------------------------|
| Código                  | Inglês                           |
| Mensagens de erro       | Português                        |
| Arquivos / Classes      | PascalCase (`UserEntity.kt`)     |
| Interfaces de repo      | `IUserRepository`                |
| Implementações          | `UserRepositoryImpl`             |
| Entidades JPA           | `UserJpaEntity`                  |
| DTOs HTTP               | `CreateUserRequest/Response`     |
| Pacotes                 | snake_case / lowercase           |

---

## 12. Scripts Disponíveis (Gradle)

| Comando                          | Descrição                          |
|----------------------------------|------------------------------------|
| `./gradlew bootRun`              | Servidor em modo desenvolvimento   |
| `./gradlew build`                | Build de produção                  |
| `./gradlew test`                 | Executar testes                    |
| `./gradlew test --info`          | Testes com output detalhado        |
| `./gradlew ktlintCheck`          | Verificar código                   |
| `./gradlew ktlintFormat`         | Formatar código                    |
| `./gradlew flywayMigrate`        | Aplicar migrações                  |
| `./gradlew flywayInfo`           | Status das migrações               |
| `docker compose up -d`           | Subir PostgreSQL + Redis           |

---

## 13. Variáveis de Ambiente

```env
SPRING_PROFILES_ACTIVE=development
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/dbname
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
JWT_SECRET=sua_chave_secreta
```

---

## 14. Boas Práticas Gerais

1. **Single Responsibility**: Cada arquivo/classe tem uma única responsabilidade
2. **Dependency Inversion**: Depender de interfaces, não de implementações concretas
3. **Immutability**: Preferir `val` e `data class` imutáveis nos Value Objects
4. **Fail Fast**: Validar inputs no `companion object { fun create(...) }` das entidades
5. **Tests**: Escrever testes para entidades e casos de uso (domínio puro, sem Spring)
6. **Kotlin Idiomático**: Usar `require()`, `checkNotNull()`, extension functions e `when`
7. **Error Messages**: Usar português para mensagens de erro (UX)
8. **UUIDs**: Usar UUIDs para IDs de entidades (`java.util.UUID.randomUUID().toString()`)
9. **Null Safety**: Explorar o sistema de tipos do Kotlin — evitar `!!` a todo custo
10. **No Spring no Domain**: O domain layer não pode ter nenhuma anotação Spring