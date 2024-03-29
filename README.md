# temporal-poc
Temporal.IO Proof of concept

## Requirements
To run this POC you will need:
* Temporal.io installed and running (https://github.com/temporalio/temporal) ([docker-compose option](https://github.com/temporalio/docker-compose))
* Maven
* Java 1.8 or later

Example workflow and activities will register when the application starts, no need to manual register anything.

## Workflow flow chart used as example:
```mermaid
graph LR
    A{{operation risk check}} -->B{score?}
    B -->|> 70|C
    B -->|> 30|D
    B -->|> 30|Z
    C{{authorise payment}} --> F
    D>require validation] --> E
    E{validation fulfilled?} -->|timeout| Z
    E -->|no|Z
    E -->|yes|C
    F{payment authorised?} -->|yes| I
    F -->|no| G
    G>Require new payment method] --> H
    H{new payment submitted?} -->|timeout| Z
    H -->|no| Z 
    H -->|yes| C 
    I[add payment compensation] -->J 
    J{{delivery product}} --> K
    K{product delivered?} -->|yes| L
    K -->|no| Y
    L[add product compensation] -->M
    M[capture payment with authorization] --> X
    Y{{compensate}}-->Z
    X{{confirm transaction}}
    Z{{reject transaction}}
```

## Starting poc
After that, you can start the spring-boot application by typing on command line:
```bash
mvn spring-boot:run
```

## Firing transactions
On another terminal, submit an transaction to be processed:
```bash
curl -H 'Content-Type:application/json' -H 'Accept:application/json' -X POST http://localhost:8081/transaction -d '{ "transactionId": "1" }'
```
If you prefer, you can submit several this way (transactionId will be created randomly):
```bash
for i in {1..10}; do 
  curl -H 'Content-Type:application/json' -H 'Accept:application/json' -X POST http://localhost:8081/transaction -d '{ }' 
done;
```
Now navigate to `http://localhost:8080` to check your workflow UI and the application logs.

if you want to check transaction progression you can use
```bash
curl -H 'Content-Type:application/json' -H 'Accept:application/json' -X GET http://localhost:8081/transaction/1'
```

if you need to complete risk validation for an transaction
```bash
curl -H 'Content-Type:application/json' -H 'Accept:application/json' -X PATCH http://localhost:8081/transaction/18 -d '{ "validateRisk": true }'
```
if you need to change the payment method for an transaction
```bash
curl -H 'Content-Type:application/json' -H 'Accept:application/json' -X PATCH http://localhost:8081/transaction/18 -d '{ "newPaymentMethod": true }'
```
