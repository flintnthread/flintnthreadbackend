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
# Edit DB_PASSWORD, SENDGRID_API_KEY, SELLER_SERVICE_PORT=8083, etc.

export FLINT_CONFIG_DIR=/etc/flintnthread
export SPRING_PROFILES_ACTIVE=prod
bash scripts/deploy-vps.sh

# Route all 3 services on https://flintnthread.online
bash scripts/apply-nginx-flintnthread-online.sh
# Add inside nginx server { } BEFORE catch-all to :8080:
#   include snippets/flintnthread-api.conf;
sudo nginx -t && sudo systemctl reload nginx
```

### One domain routing

| Path prefix | Backend | Port |
|-------------|---------|------|
| `/api/admin/` | admin-service | 8082 |
| `/api/seller/`, `/api/auth/`, `/api/public/`, `/api/sellers/` | seller-service | 8083 |
| everything else `/api/...` | user-service | 8080 |

Frontends (all use `https://flintnthread.online`):

| App | Env |
|-----|-----|
| ecommerce-mobile | user API base URL |
| seller-app-new | `EXPO_PUBLIC_API_BASE_URL=https://flintnthread.online` |
| Admin | `EXPO_PUBLIC_ADMIN_API_BASE_URL=https://flintnthread.online` |

Test seller APIs from your PC:

```powershell
powershell -File flintnthread-platform/scripts/test-seller-production.ps1
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
