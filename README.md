# buy02

`buy02` is a microservices e-commerce project. It contains an Angular frontend, a Spring Cloud Gateway, Eureka service discovery, Spring Boot services for **users, products, orders, and media**, MongoDB databases, Kafka event flow, MinIO media storage, and a Jenkins CI/CD pipeline.

The application currently supports:

- User registration and login with JWT authentication
- User profile and public seller profile pages
- Product listing and product details
- Seller-only product management
- Shopping cart and checkout
- Order creation and order history
- Seller order management
- Order status tracking
- User and seller analytics dashboards
- Image upload, update, and deletion for products and avatars
- Product/media cleanup events through Kafka

---

# Project Structure

- `frontend` - Angular 21 application served by Nginx in Docker
- `gateway` - Spring Cloud Gateway, JWT validation, CORS configuration, and service routing
- `eureka-server` - Eureka service discovery
- `user-service` - Authentication and user management
- `product-service` - Product CRUD and seller ownership validation
- `order-service` - Shopping cart, checkout, orders, seller orders, analytics
- `media-service` - Image APIs, MinIO storage, Kafka consumer
- `docker-compose.yml` - Complete application stack
- `docker-compose.infra.yml` - Jenkins infrastructure
- `Jenkinsfile` - CI/CD pipeline
- `scripts/run-https.sh` - HTTPS startup helper
- `run-all.sh` - Starts all backend services locally

---

# Architecture

The frontend communicates only with the API Gateway. Backend services register themselves with Eureka, allowing the gateway to route requests using logical service names.

```text
Browser
   │
   ▼
Frontend (Angular)
   │
   ▼
Spring Cloud Gateway
   │
   ├────────► USER-SERVICE
   ├────────► PRODUCT-SERVICE
   ├────────► ORDER-SERVICE
   └────────► MEDIA-SERVICE
                │
                ▼
     MongoDB • Kafka • MinIO
```

---

# Infrastructure

Docker Compose runs **four** MongoDB databases:

- `mongodb_users` → user-service
- `mongodb_products` → product-service
- `mongodb_orders` → order-service
- `mongodb_media` → media-service

Other infrastructure:

- Kafka
- MinIO
- Traefik
- Eureka Server

---

# Prerequisites

Docker setup:

- Docker
- Docker Compose
- OpenSSL

Optional:

- mkcert (trusted HTTPS certificates)

For local development:

- Java 17
- Node.js 20+
- npm

---

# Environment

Create a local environment file:

```bash
cp .env.example .env
```

Review these variables:

- JWT_SECRET
- MINIO_ROOT_USER
- MINIO_ROOT_PASSWORD
- MINIO_ACCESS_NAME
- MINIO_ACCESS_SECRET
- FRONTEND_API_BASE_URL
- CORS_ALLOWED_ORIGINS

The HTTPS helper automatically updates the frontend URL and allowed origins.

---

# Running the Application

## Recommended

```bash
./scripts/run-https.sh
```

Detached mode:

```bash
./scripts/run-https.sh -d
```

Regenerate certificates:

```bash
./scripts/run-https.sh --force-certs
```

The script:

- creates HTTPS certificates
- creates `.env` if missing
- configures Traefik
- creates the Docker network
- builds and starts every service

After startup:

| Service | URL |
|---------|-----|
| Application | https://localhost:8443 |
| Gateway | https://localhost:8443/api |
| Eureka | http://localhost:8761 |
| MinIO API | http://localhost:9000 |
| MinIO Console | http://localhost:9001 |
| Frontend (direct) | http://localhost:4200 |

---

# Docker Compose

Create the shared network:

```bash
docker network inspect shared-net >/dev/null 2>&1 || docker network create shared-net
```

Run:

```bash
docker compose up --build
```

Stop:

```bash
docker compose down
```

---

# Local Development

Run every backend service:

```bash
./run-all.sh
```

Logs are written into:

```
logs/
```

Helper scripts:

- run-services.sh
- rerun-services.sh
- stop-services.sh
- stop-all.sh
- scripts/db/db-start.sh
- scripts/db/db-stop.sh
- scripts/kafka/kafka_init.sh
- scripts/kafka/kafka_stop.sh

---

# Frontend Development

```bash
cd frontend
npm install
npm start
```

Frontend tests:

```bash
cd frontend
npm test
```

Main routes:

- /products
- /products/:id
- /login
- /register
- /profile
- /users/:id
- /seller
- /cart
- /checkout
- /orders

# Backend Development

Run all backend tests:

```bash
./mvnw clean test
```

Build every backend module:

```bash
./mvnw package -DskipTests
```

Spring Boot modules:

- user-service
- product-service
- order-service
- media-service
- gateway
- eureka-server

---

# API Routes

Gateway routes:

- `/api/auth/**` → user-service
- `/api/users/**` → user-service
- `/api/products/**` → product-service
- `/api/orders/**` → order-service
- `/api/media/**` → media-service

---

# Main REST Endpoints

## Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`

## Users

- `GET /api/users/me`
- `PUT /api/users/me`
- `GET /api/users/public/{id}`

## Products

- `GET /api/products`
- `GET /api/products/{id}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`

## Orders

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{id}`
- `GET /api/orders/seller`
- `PUT /api/orders/{id}/cancel`
- `PUT /api/orders/{id}/remove`
- `PUT /api/orders/{id}/redo`

## Media

- `POST /api/media/images`
- `POST /api/media/images/profile`
- `PUT /api/media/images/{id}`
- `DELETE /api/media/images/{id}`

---

# OpenAPI Documentation

Swagger/OpenAPI documentation is available through the Gateway:

- `/v3/api-docs/user-service`
- `/v3/api-docs/product-service`
- `/v3/api-docs/order-service`
- `/v3/api-docs/media-service`

---

# Default Ports

| Service | Port |
|---------|------|
| Traefik HTTPS | **8443** |
| Traefik HTTP | **8000** |
| Angular Frontend | **4200** |
| Gateway | **8080** |
| User Service | **8081** |
| Product Service | **8082** |
| Media Service | **8083** |
| **Order Service** | **8084** |
| Eureka | **8761** |
| MinIO API | **9000** |
| MinIO Console | **9001** |
| Kafka | **9092** |
| Jenkins | **8085** |
| Jenkins Agent | **50000** |
| SonarQube | **9002** |

---

# Jenkins CI/CD

The Jenkins image is built from the root Dockerfile.

Start Jenkins:

```bash
docker compose -f docker-compose.infra.yml up --build -d
```

Jenkins UI:

```
http://localhost:8085
```

---

# SonarQube

Start SonarQube:

```bash
docker compose -f sonar-infra/docker-compose.yaml up -d
```

SonarQube Dashboard:

```
http://localhost:9002
```

The exposed port can be changed through:

```
SONARQUBE_PORT
```

---

# CI/CD Pipeline

The Jenkins pipeline automatically performs:

1. Checkout source code
2. Backend unit tests
3. Frontend tests
4. Maven package
5. SonarQube analysis
6. SonarQube Quality Gate verification
7. Docker deployment
8. Archive JUnit reports
9. Email notification
10. Automatic rollback (`git revert`) when deployment fails

---

# Technologies Used

## Backend

- Java 17
- Spring Boot
- Spring Security
- Spring Cloud Gateway
- Spring Cloud Eureka
- Spring Data MongoDB
- Kafka

## Frontend

- Angular 21
- Bootstrap
- TypeScript

## Infrastructure

- Docker
- Docker Compose
- Traefik
- MinIO
- MongoDB
- Kafka

## DevOps

- Jenkins
- SonarQube
- Maven

---

# Useful Files

- `docker-compose.yml`
- `docker-compose.infra.yml`
- `Jenkinsfile`
- `run-all.sh`
- `scripts/run-https.sh`
- `.env.example`
- `frontend/API_MAPPING.md`

---

# Troubleshooting

### Services do not register in Eureka

Open:

```
http://localhost:8761
```

Verify that every microservice is registered.

---

### Gateway cannot reach a service

Check that:

- Eureka is running.
- The target service is running.
- The service name matches the Gateway configuration.

---

### Frontend cannot call the API

Verify:

- `https://localhost:8443/api`
- `CORS_ALLOWED_ORIGINS`
- `FRONTEND_API_BASE_URL`

---

### HTTPS Certificate Warning

Install **mkcert** or manually trust the generated local certificate.

---

### Image Upload Issues

Verify:

- MinIO is running.
- Bucket exists.
- Credentials inside `.env` are correct.

---

### Kafka Problems

Check that:

- Kafka container is running.
- Kafka topics were created.
- Consumers are connected.

---

### MongoDB Connection Problems

Verify that the corresponding MongoDB container is running:

- mongodb_users
- mongodb_products
- mongodb_orders
- mongodb_media

---

# License

This project was developed as part of the **Buy02** educational project and is intended for learning purposes.