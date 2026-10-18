# API RESTful de Creación y Gestión de Usuarios

Este proyecto implementa una API RESTful para el registro, autenticación y gestión de usuarios, desarrollada sobre **Spring Boot** utilizando los principios de **Arquitectura Limpia (Clean Architecture)** sin sobrecarga de adaptadores, **Java 17**, persistencia en memoria con **H2**, autenticación y validación basada en **Spring Security** y **JWT**, observabilidad y logging estructurado para auditoría y debugging, pruebas unitarias con **Spock Framework** (Groovy) y pruebas de integración BDD con **Cucumber** (Gherkin).

---

## 1. Diagramas de la Solución

### 1.1. Arquitectura Limpia por Capas y Seguridad
La solución se organiza en 3 capas desacopladas con flujo unidireccional de dependencias hacia el dominio central, integrando la cadena de filtros de seguridad en la capa de infraestructura:

```mermaid
graph TD
    subgraph Infrastructure ["Capa de Infraestructura (infrastructure)"]
        SEC_CFG["SecurityConfig (FilterChain)"]
        JWT_FLT["JwtAuthenticationFilter"]
        JWT_ENTRY["JwtAuthenticationEntryPoint"]
        JWT_SRV["JwtTokenService"]
        UC["UserController (REST)"]
        AC["AuthController (REST)"]
        UDM["UserDtoMapper"]
        GEH["GlobalExceptionHandler"]
        JPA_REPO["UserJpaRepository (Spring Data)"]
        USER_REPO_IMPL["UserRepositoryImpl"]
        UEM["UserEntityMapper"]
        CFG["Configuraciones (OpenAPI, AppConfig)"]
    end

    subgraph Application ["Capa de Aplicación (application)"]
        CUS["CreateUserService"]
        GUS["GetUserService"]
        LUS["LoginUserService"]
        UCM["UserCommandMapper"]
        TS["TokenService (Contrato)"]
        CMD["DTOs / Commands (CreateUserCommand, LoginCommand, UserResult, etc.)"]
    end

    subgraph Domain ["Capa de Dominio (domain)"]
        USER["User (Modelo)"]
        PHONE["Phone (Modelo)"]
        UREPO["UserRepository (Contrato de Persistencia)"]
        EXC["Excepciones de Negocio (EmailAlreadyRegisteredException, InvalidCredentialsException, UserNotFoundException, etc.)"]
    end

    SEC_CFG --> JWT_FLT
    SEC_CFG --> JWT_ENTRY
    JWT_FLT --> TS
    JWT_SRV -.->|Implementa| TS
    JWT_FLT --> UC
    JWT_FLT --> AC

    UC --> UDM
    UC --> CUS
    UC --> GUS
    AC --> UDM
    AC --> LUS
    LUS --> UREPO
    LUS --> TS
    LUS --> UCM
    CUS --> UCM
    CUS --> UREPO
    CUS --> TS
    GUS --> UREPO
    GUS --> USER
    CUS --> USER
    CUS --> PHONE
    USER_REPO_IMPL -.->|Implementa| UREPO
    USER_REPO_IMPL --> UEM
    USER_REPO_IMPL --> JPA_REPO
    GEH --> EXC
```

### 1.2. Flujo de Secuencia: Registro de Usuario (`POST /api/v1/users`)

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Cliente HTTP
    participant Controller as UserController
    participant DtoMapper as UserDtoMapper
    participant Service as CreateUserService
    participant CmdMapper as UserCommandMapper
    participant Repo as UserRepository
    participant JWT as JwtTokenService
    participant DB as H2 Database

    Cliente->>Controller: POST /api/v1/users (JSON)
    Controller->>DtoMapper: toCommand(UserRequestDto)
    DtoMapper-->>Controller: CreateUserCommand
    Controller->>Service: execute(CreateUserCommand)
    
    Service->>Service: Validar Regex Correo
    Service->>Service: Validar Regex Contraseña (Configurable)
    
    Service->>Repo: existsByEmail(email)
    Repo->>DB: SELECT COUNT(*) FROM users WHERE email = ?
    DB-->>Repo: Resultado
    Repo-->>Service: true / false
    
    alt Correo ya registrado
        Service-->>Controller: Lanza EmailAlreadyRegisteredException
        Controller-->>Cliente: HTTP 409 Conflict {"mensaje": "El correo ya se ha registrado previamente"}
    else Datos válidos y correo libre
        Service->>Service: Hash de contraseña (BCrypt)
        Service->>JWT: generateToken(email)
        JWT-->>Service: Token JWT firmado (HMAC-SHA256)
        Service->>CmdMapper: toDomain(command, hash, token)
        CmdMapper-->>Service: User (dominio con teléfonos)
        Service->>Repo: save(User)
        Repo->>DB: INSERT INTO users y phones
        DB-->>Repo: Confirmación
        Repo-->>Service: User persistido
        Service->>CmdMapper: toResult(savedUser)
        CmdMapper-->>Service: UserResult
        Service-->>Controller: UserResult
        Controller->>DtoMapper: toResponse(UserResult)
        DtoMapper-->>Controller: UserResponseDto
        Controller-->>Cliente: HTTP 201 Created (JSON con id, fechas, token, isactive)
    end
```

### 1.3. Flujo de Secuencia: Inicio de Sesión (`POST /api/v1/auth/login`)

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Cliente HTTP
    participant Controller as AuthController
    participant DtoMapper as UserDtoMapper
    participant Service as LoginUserService
    participant Repo as UserRepository
    participant Encoder as PasswordEncoder (BCrypt)
    participant JWT as JwtTokenService
    participant DB as H2 Database

    Cliente->>Controller: POST /api/v1/auth/login (email, password)
    Controller->>DtoMapper: toLoginCommand(LoginRequestDto)
    DtoMapper-->>Controller: LoginCommand
    Controller->>Service: execute(LoginCommand)

    Service->>Repo: findByEmail(email)
    Repo->>DB: SELECT * FROM users WHERE email = ?
    DB-->>Repo: User
    
    alt Usuario no existe o contraseña incorrecta
        Service-->>Controller: Lanza InvalidCredentialsException
        Controller-->>Cliente: HTTP 401 Unauthorized {"mensaje": "Credenciales inválidas"}
    else Credenciales válidas
        Service->>Encoder: matches(rawPassword, encodedPassword)
        Service->>JWT: generateToken(email)
        JWT-->>Service: Nuevo token JWT firmado
        Service->>Service: Actualizar lastLogin y nuevo token en User
        Service->>Repo: save(User)
        Repo->>DB: UPDATE users SET last_login = ?, token = ? WHERE id = ?
        DB-->>Repo: OK
        Service-->>Controller: UserResult
        Controller->>DtoMapper: toDetailResponse(UserResult)
        DtoMapper-->>Controller: UserResponseDto (completo)
        Controller-->>Cliente: HTTP 200 OK (datos completos, nuevo token, last_login actualizado)
    end
```

### 1.4. Flujo de Secuencia: Consulta Autenticada con JWT (`GET /api/v1/users/{id}`)

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Cliente HTTP
    participant Filter as JwtAuthenticationFilter
    participant TokenSrv as JwtTokenService
    participant EntryPoint as JwtAuthenticationEntryPoint
    participant Controller as UserController
    participant Service as GetUserService
    participant Repo as UserRepository

    Cliente->>Filter: GET /api/v1/users/{id} (Header Authorization: Bearer <token>)

    alt Sin cabecera Authorization
        Filter->>Filter: setAttribute("auth_error_message", "Token de autenticación no proporcionado")
        Filter->>EntryPoint: commence(request, response, authException)
        EntryPoint-->>Cliente: HTTP 401 Unauthorized {"mensaje": "Token de autenticación no proporcionado"}
    else Token inválido, corrupto o expirado
        Filter->>TokenSrv: validateToken(token)
        TokenSrv-->>Filter: false (Log de causa específica)
        Filter->>Filter: setAttribute("auth_error_message", "Token JWT inválido o expirado")
        Filter->>EntryPoint: commence(request, response, authException)
        EntryPoint-->>Cliente: HTTP 401 Unauthorized {"mensaje": "Token JWT inválido o expirado"}
    else Token JWT Válido
        Filter->>TokenSrv: validateToken(token)
        TokenSrv-->>Filter: true
        Filter->>TokenSrv: extractSubject(token)
        TokenSrv-->>Filter: email (subject)
        Filter->>Filter: Establecer Authentication en SecurityContextHolder
        Filter->>Controller: Invocar controlador seguro
        Controller->>Service: execute(id)
        Service->>Repo: findById(id)
        alt Usuario encontrado
            Repo-->>Service: User
            Service-->>Controller: UserResult
            Controller-->>Cliente: HTTP 200 OK (UserResponseDto con teléfonos)
        else Usuario no existe
            Repo-->>Service: Optional.empty()
            Service-->>Controller: Lanza UserNotFoundException
            Controller-->>Cliente: HTTP 404 Not Found {"mensaje": "Usuario no encontrado con id: ..."}
        end
    end
```

---

## 2. Enfoque de Arquitectura Limpia

Se adoptó una **Arquitectura Limpia directa y pragmática** que elimina la sobrecarga de paquetes redundantes (`adapter.input`, `adapter.output`, `port.in`, `port.out`):

- **`domain`**: El núcleo de la lógica de negocio. Contiene los modelos puros (`User`, `Phone`), el contrato de repositorio (`UserRepository`) y las excepciones de dominio (`EmailAlreadyRegisteredException`, `InvalidCredentialsException`, `UserNotFoundException`, `InvalidUserDataException`). Es 100% independiente de frameworks de presentación y persistencia.
- **`application`**: Orquesta los casos de uso. Contiene `CreateUserService`, `GetUserService`, `LoginUserService`, mappers de aplicación ([`UserCommandMapper`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/application/mapper/UserCommandMapper.java)), contratos de servicios auxiliares como `TokenService` y los DTOs internos (`CreateUserCommand`, `LoginCommand`, `UserResult`, `PhoneResult`).
- **`infrastructure`**: Concreta las interacciones con el exterior:
  - `controller`: Controladores REST ([`UserController`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/controller/UserController.java), [`AuthController`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/controller/AuthController.java)), DTOs de petición/respuesta, mappers desacoplados ([`UserDtoMapper`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/controller/mapper/UserDtoMapper.java)) y el manejador global de excepciones ([`GlobalExceptionHandler`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/controller/GlobalExceptionHandler.java)).
  - `persistence`: Entidades JPA (`UserEntity`, `PhoneEntity`), interfaz Spring Data JPA (`UserJpaRepository`), mappers entidad-dominio ([`UserEntityMapper`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/persistence/mapper/UserEntityMapper.java)) y la implementación [`UserRepositoryImpl`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/persistence/UserRepositoryImpl.java) que satisface el contrato del dominio.
  - `security`: Filtro de seguridad (`JwtAuthenticationFilter`), punto de entrada de errores 401 (`JwtAuthenticationEntryPoint`), e implementador de tokens JWT ([`JwtTokenService`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/security/JwtTokenService.java)).
  - `config`: Configuración de Spring Security (`SecurityConfig`), Swagger/OpenAPI y beans del sistema.

---

## 3. Arquitectura de Seguridad (Spring Security 6 + JWT)

La capa de seguridad está diseñada específicamente para APIs REST modernas y sin estado:

### 3.1. Componentes Clave
1. **[`SecurityConfig`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/config/SecurityConfig.java)**:
   - **Sesiones sin estado (*Stateless*)**: `SessionCreationPolicy.STATELESS`. El servidor no guarda sesiones HTTP ni cookies de sesión (`JSESSIONID`).
   - **CSRF Deshabilitado**: `.csrf(AbstractHttpConfigurer::disable)`. No aplica en APIs REST basadas en tokens enviados por cabecera HTTP.
   - **Filtro JWT Personalizado**: `JwtAuthenticationFilter` se ubica antes del filtro estándar `UsernamePasswordAuthenticationFilter`.
   - **Consola H2 y Swagger**: Excluidos del filtro de autenticación y con `frameOptions` configuradas para permitir la consola de desarrollo de H2.

2. **[`JwtAuthenticationFilter`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/security/JwtAuthenticationFilter.java)**:
   - Intercepta cada solicitud entrante (`OncePerRequestFilter`).
   - Extrae el token de la cabecera `Authorization: Bearer <token>`.
   - Valida la integridad y vigencia del token.
   - En caso exitoso, inyecta un `UsernamePasswordAuthenticationToken` en el `SecurityContextHolder`.
   - Si no se provee cabecera o el token es inválido, almacena el motivo en el atributo de petición `auth_error_message` para el formateo de la respuesta.

3. **[`JwtAuthenticationEntryPoint`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/security/JwtAuthenticationEntryPoint.java)**:
   - Manejador centralizado para respuestas **HTTP 401 Unauthorized**.
   - Garantiza que la respuesta de error siempre cumpla con el formato JSON estándar del proyecto:
     ```json
     {
       "mensaje": "Token de autenticación no proporcionado"
     }
     ```
     o
     ```json
     {
       "mensaje": "Token JWT inválido o expirado"
     }
     ```

4. **[`JwtTokenService`](file:///D:/Documentos/Repos/demo/src/main/java/com/user/demo/infrastructure/security/JwtTokenService.java)**:
   - Implementa el contrato `TokenService` del paquete `application`.
   - Firma tokens utilizando algoritmos seguros **HMAC-SHA256** mediante `io.jsonwebtoken` (JJWT 0.12.x).
   - Valida expiración, formato y firmas.

### 3.2. Matriz de Autorización de Endpoints

| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/users` | **Público** | Registro de nuevos usuarios y emisión del token JWT inicial. |
| `POST` | `/api/v1/auth/login` | **Público** | Inicio de sesión con correo y contraseña, emisión de nuevo token y actualización de `last_login`. |
| `GET` | `/api/v1/users/{id}` | **Autenticado (JWT)** | Consulta de información de usuario por ID mediante `Bearer <token>`. |
| `GET` | `/swagger-ui/**`, `/v3/api-docs/**` | **Público** | Documentación OpenAPI interactiva. |
| `GET` / `POST` | `/h2-console/**` | **Público** | Consola de administración de base de datos en memoria para desarrollo. |
| *Cualquier otro* | `/**` | **Autenticado (JWT)** | Protegido por defecto (`anyRequest().authenticated()`). |

---

## 4. Observabilidad y Logs de Seguridad para Debugging

Se incorporaron logs estructurados con **SLF4J** en cada etapa del pipeline de seguridad para facilitar la depuración de problemas de autenticación tanto en desarrollo como en producción:

### 4.1. Eventos Registrados

* **`JwtTokenService`**:
  * Emisión de token (`DEBUG`): Informa el subject y fecha/hora exacta de expiración calculada.
  * Validación exitosa (`DEBUG`): Confirma la validez de la firma y vigencia.
  * Fallas de validación (`WARN`): Captura y describe la causa exacta sin ocultar la excepción:
    * `ExpiredJwtException`: Token expirado (incluye timestamp de vencimiento).
    * `MalformedJwtException`: Token mal formado o estructura alterada.
    * `SecurityException`: Clave secreta no coincide o firma corrupta.
    * `UnsupportedJwtException`: Formato de token no soportado.
    * `IllegalArgumentException`: Argumentos o claims nulos o vacíos.
  * Extracción de subject (`DEBUG` / `WARN`): Registra el usuario identificado o el motivo del fallo.

* **`JwtAuthenticationFilter`**:
  * Evaluación de la petición (`DEBUG`): Registra método HTTP y URI entrante.
  * Cabecera `Authorization` (`DEBUG`): Informa si la cabecera está ausente o si carece del prefijo `Bearer `.
  * Resultado de la autenticación (`DEBUG` / `WARN`): Registra la inyección en el contexto de seguridad o la asignación del motivo de error.

* **`JwtAuthenticationEntryPoint`**:
  * Rechazo 401 (`WARN`): Registra el acceso denegado con método HTTP, URI solicitada, motivo entregado al cliente y mensaje interno de Spring Security.

* **`SecurityConfig`**:
  * Inicialización (`INFO`): Registra las políticas activas de la cadena de filtros en el arranque.

### 4.2. Activación de Logs de Seguridad
En [`src/main/resources/application.yaml`](file:///D:/Documentos/Repos/demo/src/main/resources/application.yaml) se encuentra preconfigurado el nivel `DEBUG` para el paquete de seguridad:

```yaml
logging:
  level:
    com.user.demo.infrastructure.security: DEBUG
```

---

## 5. Requisitos Previos

- **JDK**: Java 17 o superior.
- **Gradle**: Se incluye el Gradle Wrapper (`gradlew` / `gradlew.bat`), no requiere instalación externa.

---

## 6. Compilación y Ejecución

### 6.1. Compilar el proyecto
```bash
./gradlew compileJava
```
*(En Windows PowerShell: `.\gradlew.bat compileJava`)*

### 6.2. Ejecutar la suite completa de pruebas (Spock + Cucumber)
```bash
./gradlew test
```
*(En Windows PowerShell: `.\gradlew.bat test`)*

### 6.3. Iniciar la aplicación
```bash
./gradlew bootRun
```
*(En Windows PowerShell: `.\gradlew.bat bootRun`)*

La aplicación iniciará en el puerto `8080`.

---

## 7. Documentación Interactiva (Swagger / OpenAPI)

Una vez iniciada la aplicación, se puede acceder a la interfaz Swagger UI en:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 8. Base de Datos en Memoria (H2)

- **Tipo**: En memoria (`jdbc:h2:mem:userdb`).
- **Script de creación**: [`src/main/resources/schema.sql`](file:///D:/Documentos/Repos/demo/src/main/resources/schema.sql) se ejecuta automáticamente en el arranque para crear las tablas `users` y `phones`.
- **Consola H2**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - **JDBC URL**: `jdbc:h2:mem:userdb`
  - **Usuario**: `sa`
  - **Contraseña**: *(en blanco)*

---

## 9. Configuración de Expresión Regular para Contraseñas y Correos

El requerimiento exige que **el valor de la expresión regular sea configurable**. Se gestiona directamente desde [`src/main/resources/application.yaml`](file:///D:/Documentos/Repos/demo/src/main/resources/application.yaml):

```yaml
app:
  security:
    jwt:
      secret: "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
      expiration-ms: 86400000 # 24 horas
    # Expresión regular configurable para validación de contraseñas:
    # Ejemplo por defecto: al menos 1 mayúscula, 1 minúscula, 1 dígito y longitud mínima de 6 caracteres.
    password-regex: "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$"
  validation:
    # Expresión regular para correo electrónico:
    email-regex: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
```

Cualquier cambio a la propiedad `app.security.password-regex` es absorbido automáticamente por el servicio de validación sin necesidad de modificar el código fuente.

---

## 10. Consideraciones de Seguridad y Gestión de Secretos

> [!NOTE]
> **Facilidad de Ejecución (Plug-and-Play)**:
> Por fines prácticos de la prueba técnica y para garantizar que la aplicación y sus pruebas automatizadas (Spock y Cucumber) se ejecuten de manera inmediata sin requerir configuración previa en el entorno local del evaluador, se han dejado valores por defecto predefinidos en [`src/main/resources/application.yaml`](file:///D:/Documentos/Repos/demo/src/main/resources/application.yaml) para la clave secreta de JWT (`app.security.jwt.secret`) y las credenciales de la base de datos H2 en memoria (`spring.datasource.username` / `password`).

> [!IMPORTANT]
> **Buenas Prácticas para Ambientes Productivos**:
> En arquitecturas empresariales y entornos de producción reales, **nunca se deben versionar credenciales ni secretos en texto plano dentro del repositorio**. Se recomienda implementar alguna de las siguientes alternativas estándar de la industria:
> 1. **Variables de Entorno (12-Factor App)**: Inyectar los valores dinámicamente mediante variables del sistema operativo o contenedores Docker (p. ej. `${JWT_SECRET}` y `${DB_PASSWORD}`).
> 2. **Gestores de Secretos Centralizados (Cloud Native)**: Utilizar servicios como **HashiCorp Vault**, **AWS Secrets Manager**, **Azure Key Vault** o **Google Cloud Secret Manager** mediante los conectores de Spring Cloud.
> 3. **Cifrado en Repositorio**: Emplear herramientas de cifrado como **Jasypt** (`ENC(...)`) donde la clave maestra de desencriptación se provee externamente al inicio de la JVM.

---

## 11. Formato de Endpoints y Ejemplos `curl`

### 11.1. Registro de Usuario Exitoso (`POST /api/v1/users`)

**Petición**:
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Juan Rodriguez",
    "email": "juan@rodriguez.org",
    "password": "Hunter2",
    "phones": [
      {
        "number": "1234567",
        "citycode": "1",
        "contrycode": "57"
      }
    ]
  }'
```

**Respuesta Exitosa (HTTP 201 Created)**:
```json
{
  "id": "e4a7a8d5-3ef3-40f0-8bf1-f18c645d9472",
  "created": "2026-10-08T22:15:00",
  "modified": "2026-10-08T22:15:00",
  "last_login": "2026-10-08T22:15:00",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqdWFuQHJvZHJpZ3Vlei5vcmciLCJpYXQiOjE3OTIxNjYxMDAsImV4cCI6MTc5MjI1MjUwMH0.m7rZ...",
  "isactive": true
}
```

### 11.2. Inicio de Sesión (`POST /api/v1/auth/login`)

**Petición**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@rodriguez.org",
    "password": "Hunter2"
  }'
```

**Respuesta Exitosa (HTTP 200 OK)**:
```json
{
  "id": "e4a7a8d5-3ef3-40f0-8bf1-f18c645d9472",
  "name": "Juan Rodriguez",
  "email": "juan@rodriguez.org",
  "created": "2026-10-08T22:15:00",
  "modified": "2026-10-08T22:15:00",
  "last_login": "2026-10-17T10:15:00",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqdWFuQHJvZHJpZ3Vlei5vcmciLCJpYXQiOjE3OTIxOTIwMDAsImV4cCI6MTc5MjI3ODQwMH0.k8sQ...",
  "isactive": true,
  "phones": [
    {
      "number": "1234567",
      "citycode": "1",
      "contrycode": "57"
    }
  ]
}
```

### 11.3. Consulta de Usuario por ID (`GET /api/v1/users/{id}`) - Endpoint Protegido con JWT

**Petición con token JWT válido**:
```bash
curl -X GET http://localhost:8080/api/v1/users/e4a7a8d5-3ef3-40f0-8bf1-f18c645d9472 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Respuesta Exitosa (HTTP 200 OK)**:
```json
{
  "id": "e4a7a8d5-3ef3-40f0-8bf1-f18c645d9472",
  "name": "Juan Rodriguez",
  "email": "juan@rodriguez.org",
  "created": "2026-10-08T22:15:00",
  "modified": "2026-10-08T22:15:00",
  "last_login": "2026-10-08T22:15:00",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "isactive": true,
  "phones": [
    {
      "number": "1234567",
      "citycode": "1",
      "contrycode": "57"
    }
  ]
}
```

### 11.4. Errores de Autenticación y Autorización

#### Error 401: Credenciales inválidas en login
```json
{
  "mensaje": "Credenciales inválidas"
}
```

#### Error 401: Petición sin cabecera Authorization
```bash
curl -X GET http://localhost:8080/api/v1/users/e4a7a8d5-3ef3-40f0-8bf1-f18c645d9472
```
**Respuesta (HTTP 401 Unauthorized)**:
```json
{
  "mensaje": "Token de autenticación no proporcionado"
}
```

#### Error 401: Petición con token JWT inválido o expirado
```bash
curl -X GET http://localhost:8080/api/v1/users/e4a7a8d5-3ef3-40f0-8bf1-f18c645d9472 \
  -H "Authorization: Bearer token-invalido-o-expirado"
```
**Respuesta (HTTP 401 Unauthorized)**:
```json
{
  "mensaje": "Token JWT inválido o expirado"
}
```

#### Error 404: Usuario no encontrado
```bash
curl -X GET http://localhost:8080/api/v1/users/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```
**Respuesta (HTTP 404 Not Found)**:
```json
{
  "mensaje": "Usuario no encontrado con id: 00000000-0000-0000-0000-000000000000"
}
```

### 11.5. Errores de Validación de Negocio

#### Error: Correo ya Registrado (HTTP 409 Conflict)
```json
{
  "mensaje": "El correo ya se ha registrado previamente"
}
```

#### Error: Contraseña que no cumple la Regex Configurable (HTTP 400 Bad Request)
```json
{
  "mensaje": "El formato de la contraseña es inválido"
}
```

#### Error: Formato de Correo Inválido (HTTP 400 Bad Request)
```json
{
  "mensaje": "El formato del correo es inválido"
}
```

---

## 12. Pruebas Automatizadas

El proyecto cuenta con dos suites de pruebas complementarias:

### 12.1. Pruebas Unitarias con Spock Framework (Groovy)
Ubicadas en `src/test/groovy`:
- [`JwtTokenServiceSpec.groovy`](file:///D:/Documentos/Repos/demo/src/test/groovy/com/user/demo/infrastructure/security/JwtTokenServiceSpec.groovy):
  - Verifica emisión de token JWT firmado.
  - Verifica validación de tokens válidos, nulos, en blanco, malformados, con secreto distinto y expirados.
  - Verifica extracción correcta de subject y manejo de errores.
- [`JwtAuthenticationFilterSpec.groovy`](file:///D:/Documentos/Repos/demo/src/test/groovy/com/user/demo/infrastructure/security/JwtAuthenticationFilterSpec.groovy):
  - Verifica autenticación exitosa e inyección en `SecurityContextHolder`.
  - Verifica manejo de token inválido y cabecera ausente con asignación de atributos de error.
- [`JwtAuthenticationEntryPointSpec.groovy`](file:///D:/Documentos/Repos/demo/src/test/groovy/com/user/demo/infrastructure/security/JwtAuthenticationEntryPointSpec.groovy):
  - Verifica emisión de respuesta HTTP 401 con código UTF-8 y payload JSON formateado.
- [`CreateUserServiceSpec.groovy`](file:///D:/Documentos/Repos/demo/src/test/groovy/com/user/demo/application/service/CreateUserServiceSpec.groovy):
  - Verifica creación exitosa de usuario y hashing de contraseña con BCrypt.
  - Verifica lanzamiento de `EmailAlreadyRegisteredException`.
  - Pruebas parametrizadas (`where:`) para validación de patrones regex de correo y contraseñas.
- [`LoginUserServiceSpec.groovy`](file:///D:/Documentos/Repos/demo/src/test/groovy/com/user/demo/application/service/LoginUserServiceSpec.groovy):
  - Verifica autenticación exitosa, renovación de token JWT y actualización de `lastLogin`.
  - Verifica lanzamiento de `InvalidCredentialsException` ante usuario inexistente, contraseña errónea o usuario inactivo.
  - Verifica validación de campos obligatorios.
- [`AuthControllerSpec.groovy`](file:///D:/Documentos/Repos/demo/src/test/groovy/com/user/demo/infrastructure/controller/AuthControllerSpec.groovy):
  - Pruebas unitarias para el controlador de autenticación `AuthController`.
- [`UserControllerSpec.groovy`](file:///D:/Documentos/Repos/demo/src/test/groovy/com/user/demo/infrastructure/controller/UserControllerSpec.groovy):
  - Pruebas de integración del controlador REST para los endpoints de registro y consulta por ID.
- [`UserCommandMapperSpec.groovy`](file:///D:/Documentos/Repos/demo/src/test/groovy/com/user/demo/application/mapper/UserCommandMapperSpec.groovy) y [`UserDtoMapperSpec.groovy`](file:///D:/Documentos/Repos/demo/src/test/groovy/com/user/demo/infrastructure/controller/mapper/UserDtoMapperSpec.groovy):
  - Verifican los mapeos entre capas desacopladas.

### 12.2. Pruebas de Integración BDD con Cucumber
Ubicadas en `src/test/resources/features` y `src/test/java/com/user/demo/cucumber`:
- [`user_registration.feature`](file:///D:/Documentos/Repos/demo/src/test/resources/features/user_registration.feature):
  - **Escenario 1**: Registro exitoso con validación de status 201, UUID, JWT, campos de auditoría e `isactive`.
  - **Escenario 2**: Intento de duplicidad con status 409 y mensaje exacto `"El correo ya se ha registrado previamente"`.
  - **Escenario 3**: Error 400 ante formato de correo inválido.
  - **Escenario 4**: Error 400 ante contraseña no conforme a la regex configurable.
- [`user_login.feature`](file:///D:/Documentos/Repos/demo/src/test/resources/features/user_login.feature):
  - **Escenario 1**: Inicio de sesión exitoso con credenciales válidas (status 200, JWT nuevo, last_login y respuesta completa del usuario).
  - **Escenario 2**: Error 401 ante contraseña incorrecta (`"Credenciales inválidas"`).
  - **Escenario 3**: Error 401 ante correo no registrado (`"Credenciales inválidas"`).
  - **Escenario 4**: Error 400 ante datos faltantes o vacíos.
- [`get_user_by_id.feature`](file:///D:/Documentos/Repos/demo/src/test/resources/features/get_user_by_id.feature):
  - **Escenario 1**: Obtención exitosa de usuario con token JWT válido (status 200).
  - **Escenario 2**: Error 401 ante consulta sin cabecera `Authorization` (`"Token de autenticación no proporcionado"`).
  - **Escenario 3**: Error 401 ante consulta con token JWT inválido (`"Token JWT inválido o expirado"`).
  - **Escenario 4**: Error 404 ante usuario con ID inexistente.
- Reporte HTML generado automáticamente en `build/reports/cucumber/cucumber.html`.
