# ZenTraffic

Smart Urban Traffic Management and Driver Assistance Platform built as Spring Boot microservices.

## Services

| Service | Port | Purpose |
| --- | ---: | --- |
| Eureka Server | 8761 | Service discovery |
| API Gateway | 8080 | Central routing and JWT validation |
| Auth Service | 8081 | Signup, login, JWT, roles |
| Traffic Service | 8082 | Reports, road status, congestion rules, Kafka events, Redis traffic cache |
| Route Service | 8083 | Dijkstra, A*, BFS, route alternatives, Redis route cache |
| Notification Service | 8084 | Manual alerts and Kafka-driven traffic notifications |
| WebSocket Service | 8085 | Live STOMP updates for dashboards and drivers |
| Analytics Service | 8086 | Heatmap, trends, peak-hour analytics, signal suggestions |

## Required Local Infrastructure

Docker is intentionally not used. Install and run these locally:

- PostgreSQL with database `zentraffic`
- Redis on `localhost:6379`
- Kafka on `localhost:9092`

Default PostgreSQL credentials are `postgres/postgres`. Override with:

```powershell
$env:POSTGRES_URL="jdbc:postgresql://localhost:5432/zentraffic"
$env:POSTGRES_USER="postgres"
$env:POSTGRES_PASSWORD="postgres"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
$env:JWT_SECRET="change-this-development-secret-with-32-chars"
```

## Run Order

From the repo root, start each service in a separate terminal:

```powershell
mvn -pl eureka-server spring-boot:run
mvn -pl api-gateway spring-boot:run
mvn -pl auth-service spring-boot:run
mvn -pl traffic-service spring-boot:run
mvn -pl route-service spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl websocket-service spring-boot:run
mvn -pl analytics-service spring-boot:run
```

Start the frontend in another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:3000
```

## API Flow

Create a user:

```http
POST http://localhost:8080/auth/signup
Content-Type: application/json

{
  "name": "Driver One",
  "email": "driver@example.com",
  "password": "secret123",
  "role": "DRIVER"
}
```

Use the returned token as `Authorization: Bearer <token>` for gateway routes.

Report traffic:

```http
POST http://localhost:8080/traffic/report
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": 1,
  "roadId": 3,
  "reportType": "ACCIDENT",
  "severity": 4,
  "description": "Accident near flyway entry",
  "vehicleCount": 85,
  "observedSpeed": 12
}
```

Calculate a route:

```http
POST http://localhost:8080/route/calculate
Authorization: Bearer <token>
Content-Type: application/json

{
  "source": "Sector 18",
  "destination": "Akshardham",
  "strategy": "DIJKSTRA",
  "emergencyVehicle": false
}
```

Useful endpoints:

- `GET /traffic/live`
- `GET /traffic/congestion`
- `GET /traffic/heatmap`
- `GET /route/alternatives?source=Sector 18&destination=Akshardham`
- `GET /route/locations`
- `GET /notify`
- `GET /analytics/summary`
- `GET /analytics/peak-hours`
- `GET /analytics/trends`
- `GET /analytics/signals`

## WebSocket

STOMP endpoint:

```text
ws://localhost:8085/ws/traffic
```

Subscribe to:

- `/topic/traffic`
- `/topic/alerts`
- `/topic/congestion`
- `/topic/roads/{roadId}`

## Rule-Based Intelligence

- Congestion score combines vehicle density, average speed, and report severity.
- Roads become `HEAVY`, `BLOCKED`, or `ACCIDENT` based on deterministic thresholds.
- Signal recommendations increase green time when congestion score crosses 45.
- Notifications are triggered by Kafka events for accidents and heavy congestion.
- Route optimization penalizes congested roads while keeping DSA algorithms visible for interviews.
