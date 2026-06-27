# Flint & Thread Platform — Backend

Three Spring Boot services, **properties only**, one shared MySQL database.

| Module | Port (local) | Frontend |
|--------|--------------|----------|
| `user-service` | **8080** | ecommerce-mobile |
| `seller-service` | **8083** | seller-app-new |
| `admin-service` | **8082** | Admin |

## Properties layout

Each service is self-contained — database, shared integrations, and service settings live in one file:

```
user-service/src/main/resources/
├── application.properties              ← imports user + optional config/
├── application-user.properties         ← DB + mail/Razorpay/etc + user (port 8080)
├── application-local.properties
└── application-prod.properties

seller-service/src/main/resources/
├── application.properties
├── application-seller.properties       ← DB + shared + seller (port 8083 local)
├── application-local.properties
└── application-prod.properties

admin-service/src/main/resources/
├── application.properties
├── application-admin.properties        ← DB + shared + admin (port 8082)
├── application-local.properties
└── application-prod.properties

config/                                 ← your secrets (gitignored)
├── application.properties.example      ← VPS: DB password once
└── application-local.properties.example
```

## Local dev

```bash
cp config/application-local.properties.example config/application-local.properties
# Edit DB_PASSWORD if needed

start-all.bat    # Windows
stop-all.bat
```

## VPS

```bash
sudo cp config/application.properties.example /etc/flintnthread/application.properties
# Edit DB_PASSWORD, SENDGRID_API_KEY, etc.

export FLINT_CONFIG_DIR=/etc/flintnthread
export SPRING_PROFILES_ACTIVE=prod
bash scripts/deploy-vps.sh
```

## Build

```bash
./mvnw clean install -DskipTests
```

## Health checks

| Service | URL |
|---------|-----|
| User | `GET http://localhost:8080/api/categories/main` |
| Seller | `GET http://localhost:8083/api/public/marketplace-stats` |
| Admin | `GET http://localhost:8082/api/admin/health` |

## Prerequisites

Java 17+, Maven 3.9+, MySQL 8.x with database `flintandthread`
