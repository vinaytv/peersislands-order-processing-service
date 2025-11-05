# Order Processing (Spring Boot + H2 in-memory)

## Run

```
mvn spring-boot:run
```

## Endpoints

- POST `/api/orders` create
- GET `/api/orders/{id}` fetch
- GET `/api/orders?status=PROCESSING` list w/ filter
- PATCH `/api/orders/{id}/cancel` cancel (only PENDING)

## Tests

```
mvn test
```
