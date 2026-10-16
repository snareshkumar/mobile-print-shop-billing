# Deployment handoff

## Current status

No Vercel deployment exists for this project yet. The GitHub repository is `snareshkumar/mobile-print-shop-billing`, created and made accessible after the initial build. A Vercel integration was available but not connected during this build. No hosted MySQL database was supplied or provisioned.

GitHub authentication alone does not grant a Vercel session or expose GitHub credentials for reuse. Connect Vercel and grant repository access through their normal authorization flows. Do not paste passwords or tokens into source or chat.

## Vercel container deployment

Vercel's documentation retrieved during this task describes `Dockerfile.vercel` support for OCI containers. The root Dockerfile builds React, bundles it in the Spring Boot JAR and runs Java as a non-root user. Spring reads the platform's `PORT` variable; the container defaults to 8080.

Sources checked:
- https://vercel.com/kb/guide/does-vercel-support-docker-deployments
- https://vercel.com/blog/dockerfile-on-vercel

1. Complete CI/acceptance checks before using real shop records.
2. Connect an accessible GitHub repository containing the project, or use an authorized Vercel CLI upload.
3. Import the repository root into Vercel using its container deployment workflow, with `Dockerfile.vercel`.
4. Provision a persistent MySQL 8.4-compatible database outside the stateless application container. Vercel does not run the Compose database volume as durable storage.
5. Set secrets in Vercel's environment settings:
   - `DB_URL`: `jdbc:mysql://YOUR_HOST:3306/shop?sslMode=VERIFY_IDENTITY&connectionTimeZone=UTC`
   - `DB_USER`: database user with schema migration permissions
   - `DB_PASSWORD`: database password
   - `SHOP_USERNAME`: owner username
   - `SHOP_PASSWORD`: strong owner password (16+ characters, at most 72 UTF-8 bytes)
   - `SHOP_NAME`, `SHOP_ADDRESS`, `SHOP_PHONE`
   - `PORT`: use the port assigned by Vercel; if configuration requires an explicit value, use 8080 consistently.
6. Use the database provider's CA/trust configuration if its certificate is not trusted by the JVM. Do not disable TLS certificate validation.
7. Deploy, inspect build/runtime logs, and verify `/api/health`, owner login, product receipt, issue a bill, reload persistence, reports and cancellation.
8. Record the literal successful deployment URL. A frontend-only page without a working API/database is not a completed deployment.

Use a separate preview database for preview deployments. Do not run unreviewed preview code against the production shop database. Container availability, account limits and billing must be checked in the connected Vercel account.

## Backup and recovery

For local Compose, create a consistent logical backup (protect the output because it contains customer details):

```bash
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --single-transaction --no-tablespaces -u shop shop' > shop-backup.sql
```

For hosted MySQL enable provider-managed daily backups and test restoration into a separate database. Avoid storing backups inside the application image or Git repository.
