# EventFlow

EventFlow is a Spring Boot event-management and ticketing application with PostgreSQL, Thymeleaf, Mailpit-ready email delivery, and PayPal-ready payment configuration.

## Local Docker Run

Prerequisites:

- Docker Desktop is installed and running.
- Port `8082` is free for the Docker web app, or set another `APP_HOST_PORT`.
- Local IntelliJ/Spring Boot runs use `8081` by default, so Docker uses `8082` to avoid a port conflict.
- Port `5433` is free for the Docker PostgreSQL host mapping. The app container still connects to PostgreSQL through `db:5432`.

Create a local Docker environment file:

```powershell
Copy-Item .env.docker.example .env.docker
```

Start the application stack:

```powershell
docker compose --env-file .env.docker up --build
```

Open:

- EventFlow: http://localhost:8082
- Mailpit: http://localhost:8025
- Health: http://localhost:8082/actuator/health

Run the optional pgAdmin container:

```powershell
docker compose --env-file .env.docker --profile tools up --build
```

Stop containers:

```powershell
docker compose --env-file .env.docker down
```

Compose uses project-scoped container names such as `eventflow-app-1`. If older fixed-name containers like `eventflow-app` exist from a previous run, `docker compose --env-file .env.docker down` removes them without deleting the database volume.

Reset the Docker database completely:

```powershell
docker compose --env-file .env.docker down -v
```

`down -v` deletes the PostgreSQL Docker volume and all data stored in it.

## PayPal In Docker

For local manual-payment mode, keep:

```properties
EVENTFLOW_PAYMENT_PROVIDER=local
```

For PayPal sandbox testing, set these values in `.env.docker`:

```properties
EVENTFLOW_PAYMENT_PROVIDER=paypal
PAYPAL_MODE=sandbox
PAYPAL_CLIENT_ID=your_sandbox_client_id
PAYPAL_CLIENT_SECRET=your_sandbox_secret
PAYPAL_WEBHOOK_ID=your_sandbox_webhook_id
PAYPAL_CURRENCY=EUR
APP_PUBLIC_BASE_URL=https://your-ngrok-url
```

When using PayPal webhooks, `APP_PUBLIC_BASE_URL` must be the public HTTPS URL that PayPal can reach.

## Production Compose Template

Copy the production example:

```powershell
Copy-Item .env.prod.example .env.prod
```

Fill all secrets in `.env.prod`, then start:

```powershell
docker compose --env-file .env.prod -f docker-compose.prod.yml up --build -d
```

The production compose file is a starting point for VPS/container-host deployment. For a real public deployment, configure HTTPS, domain DNS, backups, monitoring, and secret storage outside source control.

Before production deployment:

- Create a real `.env.prod` from `.env.prod.example`.
- Set `APP_PUBLIC_BASE_URL` to the public HTTPS domain.
- Configure PayPal live or sandbox credentials that match the same public domain.
- Configure a real SMTP provider instead of Mailpit.
- Create the first admin user and grant `ROLE_ADMIN`.
- Configure PostgreSQL backups and log retention.
