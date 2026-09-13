# Banking System

A microservices-based banking system architecture.

## Services Overview

- **Account Service:** Manages user accounts, balances, and status (block, deduct, etc.).
- **Transaction Service:** Handles money transfers and payment transactions.
- **Fraud Detection Service:** Monitors and blocks suspicious activities.
- **Notification Service:** Sends alerts and updates to users.
- **API Gateway:** Routes external requests to the appropriate internal services.

## Architecture & Patterns

The system utilizes several architectural patterns common in modern distributed systems, including:
- **Microservices Architecture:** Services are independently deployable and decoupled.
- **Saga Pattern:** To maintain data consistency across distributed transactions without relying on a single distributed database lock. (See `docs/saga-pattern.md` for details).

## License

If you copy or use this code in any way, you must provide clear credit and attribution to the original author.

## How to Run Locally

### Prerequisites
- [Docker](https://www.docker.com/) and Docker Compose installed.
- Java 17+ installed.

### 1. Start Infrastructure (Docker)
This project relies on several backing services (MySQL, Redis, Kafka, Zookeeper) which are pre-configured in `docker-compose.yml`.

Run the following command in the root directory to start all required infrastructure:
```bash
docker-compose up -d
```
You can verify the containers are running with `docker ps`.

### 2. Run the Microservices
Each microservice is a standard Spring Boot application. To run them locally, navigate into the respective service directory (e.g., `account-service`, `transection-service`) and use the Maven wrapper:

```bash
cd account-service
./mvnw spring-boot:run
```

Open a new terminal for each service you wish to run. Ensure the Docker infrastructure is fully running before starting the services, as they will attempt to connect to MySQL and Kafka on startup.

### 3. Stop Infrastructure
When you are done developing, you can stop the background containers by running:
```bash
docker-compose down
```
