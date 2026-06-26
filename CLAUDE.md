# Awki Backend — Guía de arquitectura y estándares obligatorios

Sistema de monitoreo prenatal SaaS multi-tenant. Stack: Spring Boot 3.5, Java 21, PostgreSQL, Redis, Liquibase.  
Paquete base: `com.awki`. Perfil activo por defecto: `dev`.

---

## Arquitectura: Monolito Modular

El proyecto es un **Modular Monolith**: una sola unidad desplegable con límites de módulo definidos por paquetes de dominio. Cada módulo es autónomo internamente y se comunica con otros solo a través de sus servicios públicos, nunca accediendo directamente a repositorios ajenos.

### Estructura de paquetes

```
com.awki/
├── common/          # Contratos compartidos: BaseEntity, ApiResponse, ApiMeta, enums
├── config/          # Configuración transversal: Security, CORS, Redis, JPA, OpenAPI
├── exception/       # Excepciones de dominio + GlobalExceptionHandler
│
├── auth/            # Módulo: autenticación, JWT, filtro HTTP
├── websocket/       # Módulo: WebSocket STOMP, notificaciones en tiempo real
├── vinculacion/     # Módulo: vínculo médico-paciente
├── embarazo/        # Módulo: gestión del embarazo
├── control/         # Módulo: controles prenatales
├── chat/            # Módulo: chat IA
├── riesgo/          # Módulo: cálculo de riesgo
├── alerta/          # Módulo: alertas clínicas
├── documento/       # Módulo: documentos clínicos
├── epicrisis/       # Módulo: epicrisis
├── clinica/         # Módulo: gestión de clínicas
└── sync/            # Módulo: sincronización offline
```

### Estructura interna de cada módulo

Cada módulo **debe** tener esta estructura de sub-paquetes:

```
{modulo}/
├── controller/   # Endpoints REST o STOMP — solo orquestación, sin lógica
├── service/      # Lógica de negocio — única capa que aplica reglas
├── repository/   # Acceso a datos — solo Spring Data JPA, sin lógica
├── entity/       # Entidades JPA — extienden BaseEntity
└── dto/          # Records inmutables para entrada/salida — nunca exponer entidades
```

### Regla de comunicación entre módulos

Un módulo **solo puede invocar el `Service` público de otro módulo**. Queda prohibido:
- Inyectar un `Repository` de otro módulo
- Acceder directamente a una `Entity` de otro módulo desde un `Service` externo
- Crear dependencias circulares entre módulos

---

## Principios SOLID — Obligatorios

### S — Single Responsibility

Cada clase tiene una sola razón para cambiar.

- `JwtService` → solo operaciones JWT (generación, validación, extracción de claims)
- `JwtAuthenticationFilter` → solo autenticar requests HTTP vía Bearer token
- `WebSocketAuthHandshakeInterceptor` → solo validar el token en el handshake WebSocket
- `WebSocketNotificationService` → solo despachar mensajes STOMP a topics de médicos
- `GlobalExceptionHandler` → solo traducir excepciones a respuestas HTTP estructuradas
- Cada `Controller` → solo recibir requests, delegar al `Service`, devolver respuesta
- Cada `Service` → solo lógica de negocio del módulo, delegar persistencia al `Repository`

**Señal de violación:** una clase hace llamadas a la BD Y aplica reglas de negocio Y formatea respuestas.

### O — Open/Closed

Las clases están abiertas a extensión, cerradas a modificación.

- `BaseEntity` → todos los `Entity` la extienden; se agrega comportamiento de auditoría sin modificarla
- `GlobalExceptionHandler` → agregar soporte a una nueva excepción = agregar un `@ExceptionHandler` nuevo, sin tocar los existentes
- `ApiResponse<T>` → genérico, sirve para cualquier tipo `T` sin modificarse
- Los módulos nuevos se agregan como nuevos paquetes; la infraestructura base no se modifica

**Señal de violación:** para soportar un nuevo módulo se modifica código ya estable de otro módulo.

### L — Liskov Substitution

Las implementaciones son sustituibles por sus abstracciones.

- `WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor` → utilizable en cualquier registro de endpoint WebSocket
- `WebSocketPrincipalHandshakeHandler extends DefaultHandshakeHandler` → sobreescribe solo `determineUser`, el resto del protocolo de handshake funciona intacto
- `JwtAuthenticationFilter extends OncePerRequestFilter` → ejecutado exactamente una vez por request, sin alterar el contrato del filtro
- Toda la jerarquía de excepciones (`BusinessRuleException`, `ResourceNotFoundException`, etc.) extiende `RuntimeException` y se comporta como tal

**Señal de violación:** sobreescribir un método y cambiar su contrato o lanzar excepciones inesperadas.

### I — Interface Segregation

No se implementan interfaces con métodos innecesarios.

- `WebSocketMessageBrokerConfigurer` → solo se implementan los dos métodos requeridos; el resto tiene defaults
- `HandshakeInterceptor` → `afterHandshake` se implementa vacío intencionalmente; no se añade lógica que no corresponde a ese momento del ciclo
- Los módulos no dependen de contratos más amplios de lo que necesitan

**Señal de violación:** implementar una interfaz grande y dejar varios métodos vacíos o lanzando `UnsupportedOperationException`.

### D — Dependency Inversion

Los módulos de alto nivel dependen de abstracciones, no de implementaciones concretas.

- Toda dependencia se inyecta por constructor usando `@RequiredArgsConstructor`. Prohibido el field injection (`@Autowired` en campo)
- `WebSocketNotificationService` depende de `SimpMessagingTemplate` (abstracción del broker), no del broker concreto
- `SecurityConfig` depende del `JwtAuthenticationFilter` inyectado, no lo instancia directamente
- `PasswordEncoder` se define como `@Bean BCryptPasswordEncoder`; los consumidores dependen de la interfaz `PasswordEncoder`

**Señal de violación:** usar `new MiServicio()` dentro de otra clase, o `@Autowired` en campo.

---

## Patrones de diseño en uso

### Patrones estructurales

| Patrón | Dónde | Por qué |
|--------|-------|---------|
| **Repository** | `*Repository extends JpaRepository` | Abstrae el acceso a datos; el servicio no sabe si es PostgreSQL u otro |
| **DTO** | Records en `*/dto/` | Desacopla el contrato HTTP/WebSocket de las entidades JPA |
| **Facade** | `WebSocketNotificationService` | Oculta la complejidad de `SimpMessagingTemplate` a los módulos que emiten eventos |

### Patrones de comportamiento

| Patrón | Dónde | Por qué |
|--------|-------|---------|
| **Filter Chain** | `JwtAuthenticationFilter extends OncePerRequestFilter` | Cada filtro hace una cosa y pasa el control al siguiente |
| **Interceptor** | `WebSocketAuthHandshakeInterceptor` | Intercepta el handshake antes de que se establezca la conexión |
| **Template Method** | `WebSocketPrincipalHandshakeHandler extends DefaultHandshakeHandler` | Sobreescribe solo el paso de determinar el principal; el flujo general queda en la clase base |
| **Chain of Responsibility** | Spring Security filter chain | Cada `SecurityFilterChain` decide si maneja o delega la request |
| **Observer / Pub-Sub** | STOMP broker en memoria + `/topic/medico/{id}/alertas` | Los servicios publican eventos; los clientes WebSocket suscritos los reciben sin acoplamiento |
| **Strategy** | `PasswordEncoder` bean (`BCryptPasswordEncoder`) | La estrategia de hashing es intercambiable sin tocar los consumidores |

### Patrones creacionales

| Patrón | Dónde |
|--------|-------|
| **Builder** | `Jwts.builder().claims(...).signWith(...).compact()` en `JwtService` |
| **Factory Method** | `ApiResponse.ok(data)` / `ApiResponse.error(code, msg, status)` |

---

## Convenciones obligatorias

### Entidades

- Toda entidad **debe** extender `BaseEntity` para heredar `id` (UUID), `createdAt` y `updatedAt`
- Nunca exponer una entidad JPA directamente en un controller; usar siempre un DTO (record)

### DTOs

- Usar **Java Records** para todos los DTOs de entrada y salida (son inmutables por diseño)
- Los DTOs de entrada llevan las anotaciones de Bean Validation (`@NotNull`, `@NotBlank`, etc.)
- Los DTOs de salida (payload) son records sin anotaciones

### Respuestas HTTP

- Toda respuesta REST envuelve el resultado en `ApiResponse<T>` usando sus factory methods
- Los errores de dominio se lanzan como excepciones de `com.awki.exception` — nunca construir `ResponseEntity` de error dentro de un `Service`

### Seguridad multi-tenant

- El `clinicaId` se extrae del JWT (claim `clinicaId`)  
- Cada `Service` que accede a recursos debe verificar que `clinicaId` del token coincide con el del recurso antes de operar
- La excepción para violaciones de tenant es `TenantViolationException`

### Inyección de dependencias

```java
// CORRECTO
@Service
@RequiredArgsConstructor
public class MiServicio {
    private final MiRepository repo;
}

// PROHIBIDO
@Service
public class MiServicio {
    @Autowired
    private MiRepository repo;
}
```

### Migraciones de base de datos

- Toda modificación al esquema va en un nuevo archivo SQL dentro de `db/changelog/modulos/`
- Formato: `{nn}_{nombre_modulo}.sql` con cabecera `--liquibase formatted sql`
- El archivo se registra en `db.changelog-master.xml` con `<include>`
- Nunca usar `ddl-auto: create` o `ddl-auto: update`; siempre `validate`

### WebSocket — eventos entre módulos

Los módulos que necesitan emitir eventos en tiempo real **inyectan `WebSocketNotificationService`** y llaman a sus métodos públicos. Nunca inyectar `SimpMessagingTemplate` directamente fuera del módulo `websocket`.

```java
// CORRECTO — desde cualquier Service de otro módulo
private final WebSocketNotificationService wsNotifications;

wsNotifications.enviarAlertaNueva(medicoId, payload);

// PROHIBIDO — fuera del módulo websocket
private final SimpMessagingTemplate messagingTemplate; // no
```

### Comentarios en código

No escribir comentarios que expliquen qué hace el código. Solo añadir comentario cuando el **por qué** no es obvio: una restricción oculta, un workaround a un bug externo, un invariante no evidente.

---

## Checklist antes de hacer merge de un módulo nuevo

- [ ] Estructura de sub-paquetes respetada (`controller`, `service`, `repository`, `entity`, `dto`)
- [ ] Toda entidad extiende `BaseEntity`
- [ ] DTOs son records; ninguna entidad JPA expuesta en controller
- [ ] Toda dependencia inyectada por constructor (`@RequiredArgsConstructor`)
- [ ] El módulo no accede a repositorios de otros módulos directamente
- [ ] Las reglas multi-tenant verificadas en la capa `Service`
- [ ] Migraciones de BD en un changeset nuevo en `db/changelog/modulos/`
- [ ] Los errores de dominio son subclases de `RuntimeException` registradas en `GlobalExceptionHandler`
- [ ] Si emite eventos WebSocket, usa `WebSocketNotificationService`, no `SimpMessagingTemplate` directo
