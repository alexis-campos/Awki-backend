# Awki: Plataforma de Telemonitorización y Triaje Obstétrico con IA

**Awki** es una solución SaaS B2B2C diseñada para erradicar el "vacío de datos" clínico entre los controles prenatales. Al descentralizar el monitoreo mediante una arquitectura *Offline-First* y el uso de Procesamiento de Lenguaje Natural (NLP), Awki permite la identificación temprana de complicaciones obstétricas (como la preeclampsia) en zonas con alta saturación del sistema de salud.

## Stack Tecnológico

- **Backend:** Spring Boot 3.5 · Java 21 Corretto
- **Base de datos:** PostgreSQL 15
- **Caché / Blacklist JWT:** Redis 7
- **Frontend:** React / TypeScript / PWA
- **IA:** API LLM (NLP)

---

## Levantando el entorno de desarrollo

### Prerrequisitos

- Java 21 (Amazon Corretto recomendado)
- Maven (o usar `./mvnw` incluido en el proyecto)
- Docker y Docker Compose

### 1. Levantar la infraestructura (PostgreSQL + Redis)

```bash
docker compose up -d
```

Esto levanta:
- **PostgreSQL 15** en `localhost:5432` — base de datos `awki_db`, usuario `postgres`, contraseña `postgres`
- **Redis 7** en `localhost:6379`

Verificar que los contenedores están corriendo:

```bash
docker compose ps
```

Para detener los servicios:

```bash
docker compose down
```

Para detener y eliminar los datos (reset completo):

```bash
docker compose down -v
```

### 2. Correr la aplicación

**Desde IntelliJ IDEA:**
1. Abre el proyecto
2. Ve a `Settings → Build, Execution, Deployment → Build Tools → Maven`
3. En "Maven home path" selecciona **Use Maven wrapper (mvnw)**
4. Run → AwkiApplication

**Desde terminal:**

```bash
./mvnw spring-boot:run
```

La app levanta en `http://localhost:8080`

### 3. Swagger UI

Una vez corriendo, accede a la documentación interactiva de la API:

```
http://localhost:8080/swagger-ui.html
```

### 4. Health check

```
http://localhost:8080/actuator/health
```

---

## Estructura del proyecto

```
src/main/java/com/awki/
├── config/          # Seguridad, CORS, Swagger, Redis, JPA
├── common/          # BaseEntity, ApiResponse, enums (RolUsuario)
├── exception/       # Excepciones custom + GlobalExceptionHandler
├── validation/      # Validadores custom (@ValidCMP, @ValidFUM…)
├── auth/            # Módulo 1: Autenticación y JWT
├── vinculacion/     # Módulo 2: Vinculación médico-paciente
├── embarazo/        # Módulo 3: Embarazos y antecedentes
├── control/         # Módulo 4: Controles prenatales
├── chat/            # Módulo 5: Chat e integración IA
├── riesgo/          # Módulo 6: Motor de riesgo (líder)
├── alerta/          # Módulo 7: Alertas y notificaciones
├── documento/       # Módulo 8: Gestión documental
├── epicrisis/       # Módulo 9: Generación de epicrisis
├── clinica/         # Módulo 10: Admin de clínica y SaaS
├── websocket/       # Módulo 11: WebSocket (líder)
└── sync/            # Módulo 12: Sincronización offline
```

## Roles del sistema

| Rol | Descripción |
|---|---|
| `PACIENTE` | Mujer gestante registrada en la PWA |
| `MEDICO` | Especialista vinculado a una clínica |
| `ADMIN_CLINICA` | Administrador del establecimiento |
| `SUPERADMIN` | Equipo técnico del sistema |

## Convenciones del equipo

- Cada integrante trabaja en su módulo (`com.awki.<modulo>/`) sin modificar paquetes ajenos
- DTOs: `XxxRequest.java` / `XxxResponse.java`
- Mapeos Entity ↔ DTO: solo con **MapStruct**
- Services nunca retornan entidades JPA, siempre DTOs
- Migraciones SQL en `src/main/resources/db/changelog/modulos/MODULO_XX.sql`
- Documentar endpoints con `@Operation` y `@ApiResponse` de Springdoc

## Características principales

- Motor de Extracción NLP para análisis semántico de conversaciones
- Triaje predictivo automatizado con semaforización de riesgo
- Caché semántica con Redis (estrategia FinOps)
- Arquitectura Offline-First con sincronización asíncrona
- Alertas en tiempo real vía WebSocket + botón SOS con geolocalización
