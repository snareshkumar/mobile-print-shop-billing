# Countercraft — mobile accessories & printing shop

React frontend + Spring Boot 3.5.7 / Java 17 + Gradle 8.14.3 + MySQL 8.4.
The React build is served by Spring Boot, so the complete application uses one origin, one URL and one application container. MySQL runs separately with persistent storage.

## Delivery status

Source implementation completed. NOT deployed and NOT yet verified end to end.

- Passed: 6 Node tests for frontend arithmetic, dates, CSV safety and invoice formatting.
- Passed: standalone Java money checks for tax, discount, printing, weighted cost, rounding and invalid input.
- Passed: Java syntax parsing (does not validate Spring dependency types).
- Not run: frontend production build, complete Gradle compilation, MySQL integration tests, Docker image build or browser acceptance checks. The creation environment has restricted dependency downloads and no Docker/Gradle; offline dependency installation could not complete its policy verification.
- Deployment pending: Vercel connection and hosted MySQL credentials. Repository: https://github.com/snareshkumar/mobile-print-shop-billing . No live URL has been created.

## Features

- Products and printing services: create, list/search, edit, delete unused items, archive items with history.
- Product stock: receipts, negative adjustments, reorder alerts and movement history. Receipts can update weighted average cost. Service quantity represents pages/jobs and does not hold stock.
- Bills: editable/deletable drafts, customer name/phone, cash/UPI/card payment records, per-line discounts and configurable tax, issue paid bills, print/save PDF.
- Issued bills preserve name, SKU, selling price, tax, discount and cost snapshots. Current purchase costs are captured when issuing the bill.
- Full bill cancellation restores stocked products and retains the original invoice. Actual customer refunds happen outside this application.
- Daily, Monday-based weekly, monthly and custom-range sales/profit reports. Monthly reports can span several months. CSV downloads for reports, inventory and bills.
- Expense CRUD and net profit after recorded expenses.
- Owner authentication, database migrations, validation, transactional inventory, deterministic product lock order, duplicate-issuance protection and optimistic edit checks.

## Start locally with Docker

Requirements: Docker Engine/Desktop with Compose, access to npm/Maven and container registries.

1. Copy `.env.example` to `.env`.
2. Set three different strong passwords: `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `SHOP_PASSWORD`. Owner password must be 16 or more characters and no more than 72 UTF-8 bytes.
3. Set shop name/address/phone as needed.
4. Run:

```bash
docker compose up --build -d
docker compose logs -f app
```

Open http://localhost:8080 and sign in with `SHOP_USERNAME` (default `owner`) and your `SHOP_PASSWORD`. No default owner password or demo business data is shipped.

The initial database migrations run automatically. First startup can take a few minutes. MySQL is not exposed to the host network; the app binds to localhost. For remote access put it behind an HTTPS reverse proxy.

Stop without deleting data:

```bash
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to delete the shop database.

## First-shop setup

1. Products → Add item → create a USB cable: SKU `USB-C-1`, type Product, cost ₹60, price ₹100, tax 0.
2. Stock → receive 10 units; specify cost if different from current cost.
3. Create B&W A4 print as Service, SKU `PRINT-BW`, category Printing, cost ₹0.70, price ₹2.50. No stock receipt is needed.
4. New bill → add products and print pages → verify payment → issue bill.
5. Invoices → view/print a bill. Drafts appear regardless of report date filters.
6. Expenses → add rent/electricity to include them in net profit.
7. Reports → set date range and Daily/Weekly/Monthly grouping → export CSV.

## Accounting rules and scope

- Net sales = quantity × unit selling price − line discounts, excluding tax.
- Gross profit = net sales − item costs recorded at issuance.
- Net profit = gross profit − expenses recorded for the selected period.
- Tax is rounded per line to 2 decimals using HALF_UP. Tax is not revenue.
- Stock receipts recalculate weighted average unit cost, rounded to 2 decimals.
- Full cancellations exclude the bill from its ORIGINAL sale date, rather than recording a refund on the cancellation date. This is a simple shop sales model, not a general ledger or cash-flow statement.
- No credit/partial payments or payment gateway processing; Cash/UPI/Card records a payment already received.
- Printing cost is configured per page/job; paper/toner consumption is not deducted from another product automatically.
- Stock losses/write-offs are logged but do not automatically reduce report profit. Enter a corresponding expense if you want it reflected; avoid double counting purchases as operating expenses because cost of sales is already deducted.
- This prints a simple shop receipt, not a complete Indian statutory GST invoice. GSTIN, HSN/SAC, CGST/SGST/IGST split, place of supply and e-invoicing are not implemented.
- Inventory and report calculations use server data. Cart totals are estimates until server confirmation.
- Reports use Asia/Kolkata business dates, inclusive ranges up to 3 years. Invoice register displays up to 1,000 bills; stock history displays the latest 500 movements. Narrow date ranges when necessary.
- One owner account; no multi-store or employee roles. Production credentials are held in memory by the UI, never localStorage. Reloading requires sign-in. Use HTTPS and configure platform rate limiting for authentication endpoints.

## Verification

```bash
cd frontend
npm install --ignore-scripts
npm test
npm run build
cd ../backend
gradle test bootJar
```

`gradle test` uses Testcontainers and needs a running Docker daemon. It runs against MySQL 8.4, not an in-memory substitute. `.github/workflows/ci.yml` runs frontend tests/build, Gradle tests and a Docker build on GitHub Actions.

No Gradle wrapper binary is included; use installed Gradle 8.14.3 or the Gradle Docker build image. Direct frontend dependency versions are pinned; generate and commit an npm lockfile after a successful approved installation for repeatable transitive dependencies.

See `docs/ACCEPTANCE.md` for manual verification and `docs/DEPLOYMENT.md` for Vercel and database configuration.

## Project structure

- `frontend/src/main.jsx` — React views and API client
- `frontend/src/style.css` — responsive counter UI and print layout
- `frontend/src/utils.mjs` — display arithmetic/date/CSV helpers
- `backend/src/main/java/com/naresh/shop/` — API, business transactions, auth and validation
- `backend/src/main/resources/db/migration/` — MySQL schema
- `backend/src/test/` — unit and real-MySQL integration tests
- `Dockerfile` / `Dockerfile.vercel` — full-stack application container
- `compose.yaml` — app plus persistent MySQL
- Separate `frontend/Dockerfile` and `backend/Dockerfile` are also supplied; frontend nginx expects a backend service named `backend` on the same container network.
