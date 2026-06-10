# Invoice Microservice (RetailFlow)

Runs on port **8092**. Calls AuditLog via Feign. Called by Sale and Payment.

## Setup
```sql
CREATE DATABASE invoicedb;
```
Edit `application.properties`: set DB password and `auditlog.service.url`.

## Start order
Eureka (8761) -> AuditLog (8082) -> Invoice (8092)

## Endpoints
| Method | Path | Purpose |
|--------|------|---------|
| POST   | /api/invoices | Create invoice |
| PUT    | /api/invoices/{id} | Update amount/status |
| DELETE | /api/invoices/{id} | Cancel invoice |
| DELETE | /api/invoices/sale/{saleId} | Cancel by sale (used by Sale svc) |
| GET    | /api/invoices/sale/{saleId} | Get by sale (used by Sale svc) |
| PATCH  | /api/invoices/{id}/status?status=PAID | Update status (used by Payment svc) |
| GET    | /api/invoices/{id} | Get one |
| GET    | /api/invoices | Get all |
| GET    | /api/invoices/status/{status} | By status |
| GET    | /api/invoices/date-range?start=&end= | By date range |
| GET    | /api/invoices/paginated?page=0&size=5 | Paginated |

## Quick test (no other service needed except AuditLog)
POST http://localhost:8092/api/invoices
{ "saleId": 1, "amount": 175.0 }
