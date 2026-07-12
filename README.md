# safe-zone

`safe-zone` is a microservices e-commerce project. It contains an Angular frontend, a Spring Cloud Gateway, Eureka service discovery, Spring Boot services for users/products/media, MongoDB databases, Kafka event flow, MinIO media storage, and Jenkins pipeline support.

The app currently supports:

- user registration and login with JWT authentication
- profile and public seller profile pages
- product listing and product details
- seller-only product management
- image upload/update/delete for products and avatars
- product/media cleanup events through Kafka

## Project Structure

- `frontend` - Angular 21 application served by Nginx in Docker
- `gateway` - Spring Cloud Gateway, JWT validation, CORS, service routing
- `eureka-server` - Eureka registry for service discovery
- `user-service` - authentication, user profile, public user profile
- `product-service` - product CRUD and seller ownership checks
- `media-service` - image APIs, MinIO storage, orphan cleanup, Kafka consumer
- `docker-compose.yml` - full application stack
- `docker-compose.infra.yml` - Jenkins container stack
- `Jenkinsfile` - CI/CD pipeline
- `scripts/run-https.sh` - local HTTPS startup helper
- `run-all.sh` - local Spring service runner backed by Docker infrastructure

## Architecture

The frontend talks to the gateway. Backend services register with Eureka, and the gateway routes requests to services by logical service name.

```text
Browser
  -> Frontend
  -> Gateway
  -> USER-SERVICE / PRODUCT-SERVICE / MEDIA-SERVICE
  -> MongoDB, Kafka, MinIO
```

Docker Compose runs three separate MongoDB containers:

- `mongodb_users` for `user-service`
- `mongodb_products` for `product-service`
- `mongodb_media` for `media-service`

Other infrastructure:

- `kafka` for product/media events
- `minio` for uploaded images
- `traefik` for the HTTPS edge route
- `eureka-server` for service discovery

## Prerequisites

For the Docker path:

- Docker
- Docker Compose
- OpenSSL

Optional:

- `mkcert` for trusted local HTTPS certificates

For local service/frontend development:

- Java 17
- Node.js 20+
- npm

## Environment

Create a local environment file:

```bash
cp .env.example .env
```

Important values to review:

- `JWT_SECRET`
- `MINIO_ROOT_USER`
- `MINIO_ROOT_PASSWORD`
- `MINIO_ACCESS_NAME`
- `MINIO_ACCESS_SECRET`
- `FRONTEND_API_BASE_URL`
- `CORS_ALLOWED_ORIGINS`

The HTTPS helper updates `CORS_ALLOWED_ORIGINS` and `FRONTEND_API_BASE_URL` to `https://localhost:8443`.

## Run the Full App With HTTPS

Recommended local startup:

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

- creates local certificates in `certs/`
- creates `.env` from `.env.example` if needed
- writes local Traefik override files
- creates the external Docker network `shared-net` if needed
- runs `docker compose up --build`

After startup:

- App: `https://localhost:8443`
- Gateway API base: `https://localhost:8443/api`
- Eureka: `http://localhost:8761`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- Direct frontend container port: `http://localhost:4200`

If OpenSSL generated a self-signed certificate, the browser will show a local certificate warning.

## Run With Docker Compose Directly

Create the shared network first:

```bash
docker network inspect shared-net >/dev/null 2>&1 || docker network create shared-net
```

Then run:

```bash
docker compose up --build
```

For the HTTPS route to use local certificates, prefer `./scripts/run-https.sh` because it generates `certs/`, `local-traefik-config.yml`, and `docker-compose.override.yml`.

Stop the stack:

```bash
docker compose down
```

## Run Services From Local Source

`run-all.sh` starts Docker Compose and then launches the Spring services with their Maven wrappers:

```bash
./run-all.sh
```

Logs are written to `logs/`.

There are also helper scripts:

- `run-services.sh`
- `rerun-services.sh`
- `stop-services.sh`
- `stop-all.sh`
- `scripts/db/db-start.sh`
- `scripts/db/db-stop.sh`
- `scripts/kafka/kafka_init.sh`
- `scripts/kafka/kafka_stop.sh`

## Frontend Development

Run the Angular app locally:

```bash
cd frontend
npm install
npm start
```

Run frontend tests:

```bash
cd frontend
npm test
```

Main frontend routes:

- `/products`
- `/products/:id`
- `/login`
- `/register`
- `/profile`
- `/users/:id`
- `/seller`

## Backend Development

Run all backend unit tests from the repository root:

```bash
./mvnw clean test
```

Build backend modules:

```bash
./mvnw package -DskipTests
```

Spring Boot modules are listed in the root `pom.xml`:

- `user-service`
- `product-service`
- `media-service`
- `gateway`
- `eureka-server`

## API Routes

Gateway routes:

- `/api/auth/**` -> `user-service`
- `/api/users/**` -> `user-service`
- `/api/products/**` -> `product-service`
- `/api/media/**` -> `media-service`

Current service endpoints include:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`
- `PUT /api/users/me`
- `GET /api/users/public/{id}`
- `GET /api/products`
- `GET /api/products/{id}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `POST /api/media/images`
- `POST /api/media/images/profile`
- `PUT /api/media/images/{id}`
- `DELETE /api/media/images/{id}`

OpenAPI docs are proxied through the gateway:

- `/v3/api-docs/user-service`
- `/v3/api-docs/product-service`
- `/v3/api-docs/media-service`

## Ports

- `8443` - Traefik HTTPS entrypoint
- `8000` - Traefik HTTP entrypoint mapped to container port `80`
- `4200` - direct frontend container port
- `8080` - gateway service port
- `8081` - user service port
- `8082` - product service port
- `8083` - media service port
- `8761` - Eureka
- `9000` - MinIO API
- `9001` - MinIO console
- `9092` - Kafka inside Docker network
- `8085` - Jenkins web UI from `docker-compose.infra.yml`
- `50000` - Jenkins agent port
- `9002` - SonarQube web UI from `sonar-infra/docker-compose.yaml`

## Jenkins

The Jenkins image is built from the root `dockerfile` and started with:

```bash
docker compose -f docker-compose.infra.yml up --build -d
```

Jenkins is available at:

```text
http://localhost:8085
```

## SonarQube

SonarQube is started from the `sonar-infra` Docker Compose file:

```bash
docker compose -f sonar-infra/docker-compose.yaml up -d
```

SonarQube is available at:

```text
http://localhost:9002
```

The host port can be changed with `SONARQUBE_PORT`; the container always listens on port `9000`.

The pipeline in `Jenkinsfile` currently:

- polls SCM every minute
- runs backend tests with `./mvnw clean test`
- runs frontend tests in a `node:20-alpine` Docker agent
- packages backend modules with `./mvnw package -DskipTests`
- runs SonarQube analysis through the configured Jenkins server `MySonarServer`
- enforces the SonarQube quality gate and aborts the pipeline if it fails
- deploys the Docker Compose stack
- archives JUnit reports
- sends success/failure email
- attempts an authenticated `git revert HEAD` and push on failure

## Useful Files

- [docker-compose.yml](/home/amazighi/Desktop/new/mr-jenk/docker-compose.yml)
- [docker-compose.infra.yml](/home/amazighi/Desktop/new/mr-jenk/docker-compose.infra.yml)
- [Jenkinsfile](/home/amazighi/Desktop/new/mr-jenk/Jenkinsfile)
- [scripts/run-https.sh](/home/amazighi/Desktop/new/mr-jenk/scripts/run-https.sh)
- [.env.example](/home/amazighi/Desktop/new/mr-jenk/.env.example)
- [frontend/API_MAPPING.md](/home/amazighi/Desktop/new/mr-jenk/frontend/API_MAPPING.md)

## Troubleshooting

- If `docker compose up` says `shared-net` is missing, run `docker network create shared-net` or use `./scripts/run-https.sh`.
- If the frontend cannot reach the backend, check `https://localhost:8443/api` and `CORS_ALLOWED_ORIGINS`.
- If the browser warns about certificates, install `mkcert` or accept the local self-signed certificate.
- If uploads fail, check the MinIO credentials and bucket settings in `.env`.
- If services do not appear in Eureka, check `http://localhost:8761` and the service logs.
- If Kafka-related media cleanup does not run, check that the `kafka` container is healthy.
