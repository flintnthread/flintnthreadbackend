# Flint & Thread Platform — Unified Backend

Single Maven monorepo containing all three backends with **one shared `application.properties`**:

| Module | Profile | Port | Package | Frontend |
|--------|---------|------|---------|----------|
| `user-service` | `user` | **8080** | `com.ecommerce.authdemo` | Customer / shopper app |
| `seller-service` | `seller` | **8080** | `com.ecommerce.sellerbackend` | Seller app |
| `admin-service` | `admin` | **8082** | `com.ecommerce.adminbackend` | Admin panel |

All original source code is preserved. API paths are unchanged so your frontend integrations keep working.

## Project structure

```
flintnthread-platform/
├── platform-config/          ← SINGLE application.properties (all API keys)
├── user-service/             ← from flintnthread-backend
├── seller-service/           ← from seller-backend/seller-service
├── admin-service/            ← from seller-backend/admin-backend
├── ai-service/               ← Python CLIP image search (port 5000)
├── application-local.properties.example
├── pom.xml
└── run.bat
```

## Configuration

**All settings live in `platform-config/src/main/resources/`:**

| File | Purpose |
|------|---------|
| `flint-platform-common.properties` | Shared keys (Twilio, Razorpay, Shiprocket, mail, JPA) |
| `flint-platform-user.properties` | User service — port 8080, `flintdb` |
| `flint-platform-seller.properties` | Seller service — port 8080, `flintnthread` |
| `flint-platform-admin.properties` | Admin service — port 8082, `flintandthread` |

Each service module imports its files via `spring.config.import` in its bootstrap `application.properties`.

Optional local overrides (not committed):

```bash
cp application-local.properties.example application-local.properties
# Edit passwords, SENDGRID_API_KEY, etc.
```

### API keys included

- Twilio (SMS OTP)
- SendGrid (email SMTP)
- Razorpay (payments)
- Shiprocket (logistics)
- AppyFlow GST lookup (seller)
- JWT secrets (seller + admin)
- Cloudinary (hardcoded in user-service `CloudinaryConfig.java` — unchanged)

## Prerequisites

- Java 17+
- Maven 3.9+ (or use included `mvnw`)
- MySQL 8.x with databases configured per profile:
  - **user:** `flintdb`
  - **seller:** `flintnthread`
  - **admin:** `flintandthread`

## Build

```bash
./mvnw clean install -DskipTests
```

## Run all 3 on one PC (local dev)

User and seller both used port **8080** by default — only one could run at a time. **Seller now defaults to profile `local` → port 8083** so all three can run together:

| Service | Local port | URL |
|---------|------------|-----|
| User (customer) | **8080** | `http://localhost:8080` |
| Seller | **8083** | `http://localhost:8083` |
| Admin | **8082** | `http://localhost:8082` |

```bash
start-all.bat    # opens 3 terminal windows
stop-all.bat     # frees ports 8080, 8082, 8083
```

**From IntelliJ / IDE:** run each main class in its module — seller starts on **8083** automatically (no extra VM args).

**Production seller** (alone on port 8080): set `SPRING_PROFILES_ACTIVE=prod`.

**Seller frontend** auto-detects ports 8080 and 8083 — no config change needed.

## Run services individually

```bash
# User API (customer app) — port 8080
./mvnw -pl user-service -am spring-boot:run

# Seller API — port 8080 (stop user first) OR port 8083 (default local profile):
./mvnw -pl seller-service -am spring-boot:run

# Admin API — port 8082
./mvnw -pl admin-service -am spring-boot:run
```

## AI image search (optional)

```bash
cd ai-service
pip install -r requirements.txt
python app.py
# Runs on http://localhost:5000 — used by user-service
```

Or run `start-ai-service.bat` from the project root.

## Health checks

| Service | URL |
|---------|-----|
| User | `GET http://localhost:8080/` |
| Seller | `GET http://localhost:8080/api/public/marketplace-stats` |
| Admin | `GET http://localhost:8082/api/admin/health` |

## Why three services, not one JAR?

User and seller APIs share paths like `/api/products`, `/api/orders`, `/api/colors` with different controllers. Merging into one Spring context would cause route conflicts and break the frontend. This monorepo gives you one folder, one config file, and three independently deployable services — matching how your frontends already integrate.
