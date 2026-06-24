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

Stop containers:

```powershell
docker compose --env-file .env.docker down
```

Reset the Docker database completely:

```powershell
docker compose --env-file .env.docker down -v
```

`down -v` deletes the PostgreSQL Docker volume and all data stored in it.

## Render Hosting

The recommended hosting target is Render with:

- Docker web service built from `Dockerfile`.
- Render Postgres managed database.
- Render-managed HTTPS on the `onrender.com` URL or a custom domain.
- `render.yaml` as the repeatable service/database blueprint.

Render sets `DATABASE_URL` for the managed Postgres database. EventFlow converts that value to the JDBC datasource format automatically, so you do not need to hand-convert it.

### Deploy With Blueprint

1. Push the latest `main` branch to GitHub.
2. In Render, choose `New` -> `Blueprint`.
3. Connect the `NureSofiiaShapovalova/EventFlow` repository.
4. Select `render.yaml`.
5. Confirm the `nure-eventflow` web service and `eventflow-db` Postgres database.
6. Fill prompted secret values:

```properties
EVENTFLOW_MAIL_HOST=smtp provider host
EVENTFLOW_MAIL_USERNAME=smtp username
EVENTFLOW_MAIL_PASSWORD=smtp password
EVENTFLOW_MAIL_FROM=no-reply@your-domain.example
APP_PUBLIC_BASE_URL=https://nure-eventflow.onrender.com
```

For the first deploy, leave payments in local mode:

```properties
EVENTFLOW_PAYMENT_PROVIDER=local
PAYPAL_MODE=sandbox
PAYPAL_CURRENCY=EUR
```

After Render creates the service, open:

```text
https://nure-eventflow.onrender.com/actuator/health
https://nure-eventflow.onrender.com/
```

If Render assigned a different public URL than the one entered during setup, update `APP_PUBLIC_BASE_URL` in the web service environment variables and redeploy.

### Enable PayPal After The App Is Online

Create a PayPal webhook using the final public HTTPS URL:

```text
https://nure-eventflow.onrender.com/payments/paypal/webhook
```

Required PayPal sandbox events:

```text
Payment capture completed
Payment capture declined
Payment capture reversed
Checkout order approved
```

Then update Render environment variables:

```properties
EVENTFLOW_PAYMENT_PROVIDER=paypal
PAYPAL_MODE=sandbox
PAYPAL_CLIENT_ID=your_sandbox_client_id
PAYPAL_CLIENT_SECRET=your_sandbox_secret
PAYPAL_WEBHOOK_ID=your_webhook_id
PAYPAL_CURRENCY=EUR
APP_PUBLIC_BASE_URL=https://nure-eventflow.onrender.com
```

Redeploy the web service and test a sandbox checkout.

## Production Notes

- Keep `.env.docker`, `.env.prod`, and real secrets out of Git.
- Use Render service logs to inspect startup, Flyway migrations, payment callbacks, and mail failures.
- Render Postgres should use internal/private connectivity through the Blueprint-managed database.
- For a custom domain, update `APP_PUBLIC_BASE_URL` and the PayPal webhook URL to the final HTTPS domain.

## VPS Compose Template

The repository also includes `docker-compose.prod.yml` and `.env.prod.example` as a VPS/container-host alternative. Render is preferred for this project stage because it removes manual reverse proxy, TLS, firewall, and server maintenance work.
