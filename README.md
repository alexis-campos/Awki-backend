# Awki, Plataforma de Telemonitorización y Triaje Obstétrico

Awki es un sistema SaaS B2B2C de monitoreo prenatal diseñado para cerrar el vacío de datos clínicos que existe entre controles prenatales. Mediante una arquitectura *Offline-First*, NLP con IA generativa y alertas en tiempo real, permite la detección temprana de complicaciones obstétricas (preeclampsia, hemorragia, RCIU) incluso en zonas con conectividad limitada.

---

## Tabla de contenidos

- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Módulos del sistema](#módulos-del-sistema)
- [API REST](#api-rest)
- [Requisitos previos](#requisitos-previos)
- [Configuración del entorno](#configuración-del-entorno)
- [Levantar el entorno de desarrollo](#levantar-el-entorno-de-desarrollo)
- [Ejecutar tests](#ejecutar-tests)
- [Despliegue en producción](#despliegue-en-producción)
- [Variables de entorno](#variables-de-entorno)
- [Roles del sistema](#roles-del-sistema)

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 (Amazon Corretto) |
| Framework | Spring Boot 3.5 |
| Base de datos | PostgreSQL 15 |
| Caché / Blacklist JWT | Redis 7 |
| Migraciones | Liquibase |
| IA generativa | Google Gemini 2.5 Flash Lite |
| Notificaciones | Twilio SMS · WhatsApp Business API · Firebase FCM |
| Tiempo real | WebSocket STOMP |
| PDF | OpenPDF |
| Documentación API | Springdoc OpenAPI 3 (Swagger UI) |
| Contenedores | Docker + Docker Compose |

---

## Arquitectura

El proyecto sigue el patrón **Monolito Modular**: una sola unidad desplegable con límites de módulo definidos por paquetes de dominio. Cada módulo es autónomo internamente y se comunica con otros únicamente a través de sus servicios públicos.

```
com.awki/
├── common/          # BaseEntity, ApiResponse, enums compartidos
├── config/          # Seguridad, CORS, Redis, JPA, OpenAPI, WebSocket
├── exception/       # Excepciones de dominio + GlobalExceptionHandler
├── validation/      # Validadores custom (@ValidCMP, @ValidFUM, …)
│
├── auth/            # Módulo 01 — Autenticación y JWT
├── vinculacion/     # Módulo 02 — Vínculo médico-paciente
├── embarazo/        # Módulo 03 — Gestión del embarazo
├── control/         # Módulo 04 — Controles prenatales
├── chat/            # Módulo 05 — Chat IA con Gemini
├── riesgo/          # Módulo 06 — Motor de cálculo de riesgo
├── alerta/          # Módulo 07 — Alertas y notificaciones
├── documento/       # Módulo 08 — Gestión documental
├── epicrisis/       # Módulo 09 — Generación de epicrisis PDF
├── clinica/         # Módulo 10 — Administración de clínica
├── websocket/       # Módulo 11 — WebSocket STOMP tiempo real
└── sync/            # Módulo 12 — Sincronización offline
```

**Principios aplicados:** SOLID · Repository · DTO (Records inmutables) · Filter Chain · Observer/Pub-Sub (STOMP) · Strategy (PasswordEncoder) · Facade (WebSocketNotificationService).

**Seguridad multi-tenant:** el `clinicaId` viaja en el JWT. Cada servicio verifica que el recurso solicitado pertenezca al tenant del token antes de operar.

---

## Módulos del sistema

| # | Módulo | Responsabilidad |
|---|---|---|
| 01 | `auth` | Registro, login, refresh y logout de médicos y pacientes. JWT con blacklist en Redis. |
| 02 | `vinculacion` | Generación y consumo de códigos de vinculación médico-paciente. |
| 03 | `embarazo` | Gestión de embarazos activos, antecedentes obstétricos y resumen clínico generado por IA. |
| 04 | `control` | Registro de controles prenatales (peso, PA, FCF, síntomas, observaciones). |
| 05 | `chat` | Chat en lenguaje natural con Gemini; caché semántica en Redis para reducir costos de API. |
| 06 | `riesgo` | Motor de triaje que calcula nivel de riesgo (VERDE/AMARILLO/ROJO) a partir de signos clínicos. |
| 07 | `alerta` | Generación y despacho de alertas clínicas. Botón SOS con geolocalización. Notificaciones por SMS, WhatsApp y push (FCM). |
| 08 | `documento` | Carga y descarga de documentos clínicos adjuntos a controles. |
| 09 | `epicrisis` | Generación de epicrisis en PDF con OpenPDF; almacenamiento local o en S3. |
| 10 | `clinica` | Panel de administración: métricas de la clínica, listado de médicos y resumen de pacientes. |
| 11 | `websocket` | Broker STOMP en memoria; despacha alertas y notificaciones en tiempo real a médicos conectados. |
| 12 | `sync` | Sincronización batch para dispositivos que operaron offline (mensajes, controles, alertas). |

---

## API REST

Base URL: `http://localhost:8080/api/v1`

| Módulo | Prefijo |
|---|---|
| Autenticación | `/api/v1/auth` |
| Vinculación | `/api/v1/vinculacion` |
| Embarazos | `/api/v1/embarazos` |
| Controles prenatales | `/api/v1/controles` |
| Chat IA | `/api/v1/chat` |
| Alertas | `/api/v1/alertas` |
| Contactos de emergencia | `/api/v1/contactos-emergencia` |
| Documentos | `/api/v1/documentos` |
| Epicrisis | `/api/v1/epicrisis` |
| Clínica | `/api/v1/clinica` |
| Sincronización offline | `/api/v1/sync` |

La documentación interactiva completa (Swagger UI) está disponible en:

```
http://localhost:8080/swagger-ui.html
```

---

## Requisitos previos

- **Java 21** (Amazon Corretto recomendado)
- **Docker** y **Docker Compose**
- Maven no es necesario instalarlo; el proyecto incluye el wrapper `./mvnw`

---

## Configuración del entorno

### 1. Crear el archivo `.env`

```bash
cp .env.example .env
```

Edita `.env` y completa los valores reales. Las únicas variables obligatorias para desarrollo local son las de la base de datos, Redis y JWT, que ya tienen valores por defecto en el ejemplo.

Para usar el chat con IA en modo real (sin mock), añade tu API key de Gemini:

```
GEMINI_API_KEY=tu-api-key-aqui
GEMINI_MOCK_ENABLED=false
```

> `spring-dotenv` carga el `.env` automáticamente al arrancar la aplicación. No hace falta exportar las variables manualmente.

---

## Levantar el entorno de desarrollo

### 1. Infraestructura (PostgreSQL + Redis)

```bash
docker compose up -d
```

Esto levanta:
- **PostgreSQL 15** en `localhost:5432`, base de datos `awki_db`
- **Redis 7** en `localhost:6379`

Verificar que los contenedores están listos:

```bash
docker compose ps
```

### 2. Aplicación

```bash
./mvnw spring-boot:run
```

La app arranca en `http://localhost:8080` con el perfil `dev`. Liquibase aplica las migraciones automáticamente al iniciar.

**Desde IntelliJ IDEA:** abre el proyecto, configura el Maven wrapper en `Settings → Build Tools → Maven → Use Maven wrapper`, y ejecuta `AwkiApplication`.

### 3. Verificar que todo funciona

| Recurso | URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health check | `http://localhost:8080/actuator/health` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

### 4. Detener los servicios

```bash
# Solo detener
docker compose down

# Detener y borrar datos (reset completo)
docker compose down -v
```

---

## Ejecutar tests

```bash
./mvnw test
```

La suite incluye **85 tests** distribuidos en 12 suites que cubren servicios y controladores de todos los módulos. No requiere conexión a base de datos real ni credenciales externas; los tests usan mocks de todas las dependencias.

---

## Despliegue en producción

Activa el perfil `prod` y provee todas las variables de entorno sin valores por defecto (ver sección siguiente):

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

O con el JAR generado:

```bash
./mvnw package -DskipTests
java -jar target/awki-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

En producción, el perfil `prod` requiere que **todas** las variables de entorno estén definidas explícitamente en el entorno del servidor. No existe un `.env` en producción; las variables se inyectan desde el sistema operativo, el orquestador (Docker, Kubernetes) o el proveedor cloud.

---

## Variables de entorno

Copia `.env.example` como `.env` para desarrollo. El `.env` está en `.gitignore` y nunca debe subirse al repositorio.

| Variable | Descripción | Requerida en prod |
|---|---|---|
| `DB_URL` | URL JDBC de PostgreSQL | Sí |
| `DB_USERNAME` | Usuario de la base de datos | Sí |
| `DB_PASSWORD` | Contraseña de la base de datos | Sí |
| `REDIS_HOST` | Host de Redis | Sí |
| `REDIS_PORT` | Puerto de Redis (default: 6379) | No |
| `REDIS_PASSWORD` | Contraseña de Redis | No |
| `JWT_SECRET` | Secreto HS256 (mín. 64 caracteres). Generar con `openssl rand -base64 64` | Sí |
| `JWT_EXPIRATION` | Expiración del token en ms (default: 86400000 = 24h) | No |
| `PORT` | Puerto del servidor (default: 8080) | No |
| `CORS_ALLOWED_ORIGINS` | URL del frontend permitido | Sí |
| `GEMINI_API_KEY` | API key de Google Gemini | Sí (si mock=false) |
| `GEMINI_MOCK_ENABLED` | `true` usa respuestas simuladas sin llamar a la API | No |
| `NOTIFICATIONS_MOCK_ENABLED` | `true` desactiva el envío real de notificaciones | No |
| `TWILIO_ENABLED` | Activa el cliente SMS real de Twilio | No |
| `TWILIO_ACCOUNT_SID` | Account SID de Twilio | Si Twilio activo |
| `TWILIO_AUTH_TOKEN` | Auth token de Twilio | Si Twilio activo |
| `TWILIO_FROM_NUMBER` | Número origen de Twilio (formato E.164) | Si Twilio activo |
| `WHATSAPP_ENABLED` | Activa el cliente de WhatsApp Business API | No |
| `WHATSAPP_BUSINESS_TOKEN` | Token de WhatsApp Business | Si WA activo |
| `WHATSAPP_PHONE_NUMBER_ID` | Phone Number ID de WhatsApp Business | Si WA activo |
| `FCM_ENABLED` | Activa Firebase Cloud Messaging | No |
| `FCM_CREDENTIALS_PATH` | Ruta al `serviceAccountKey.json` de Firebase | Si FCM activo |
| `EPICRISIS_STORAGE_MODE` | `local` o `s3` | No |
| `EPICRISIS_STORAGE_PATH` | Directorio local para PDFs (si modo local) | No |

---

## Roles del sistema

| Rol | Descripción |
|---|---|
| `PACIENTE` | Mujer gestante registrada en la PWA |
| `MEDICO` | Especialista vinculado a una clínica |
| `ADMIN_CLINICA` | Administrador del establecimiento de salud |
| `SUPERADMIN` | Equipo técnico con acceso total al sistema |
