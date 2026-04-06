# ThinkUs Prueba Tecnica

Plataforma de gestion de fondos de inversion para BTG Pactual.

## Tecnologias

- Java 25
- Spring Boot 4.0.5
- MongoDB
- Spring Retry
- Lombok
- OpenAPI/Swagger
- JaCoCo
- JUnit 6 + Mockito

## Arquitectura

Clean Architecture con patron Hexagonal (Ports & Adapters):

```
domain/          <- Reglas de negocio
  model/         <- Entidades del dominio
  port/in/       <- Use cases
  port/out/      <- Puertos de salida
  service/       <- Implementacion de casos de uso

adapter/in/      <- Controllers REST, DTOs
adapter/out/     <- MongoDB repositories, Notificaciones

config/          <- Configuracion de Spring
exception/       <- Excepciones sealed + handler global
seeder/          <- Datos iniciales
```

## Patrones de Diseño

| Patron | Uso |
|--------|-----|
| Ports & Adapters | Separacion dominio-infraestructura |
| Strategy | Notificaciones |
| Factory Method | `Client.newDefaultClient()` |
| Retry | Reintento ante conflictos de concurrencia |
| Repository | Abstraccion de acceso a datos |

## Concurrencia Segura

El sistema protege contra race conditions con:
1. **Optimistic Locking** (`@Version` en Client)
2. **MongoDB Transactions** (`@Transactional`)
3. **Retry automatico** (`@Retryable`, 3 intentos, backoff 100ms)
4. **Validacion de saldo dentro de la transaccion** (no antes)

## API Endpoints

| Method | Endpoint | Descripcion |
|--------|----------|-------------|
| `GET` | `/api/v1/funds` | Listar fondos disponibles |
| `POST` | `/api/v1/funds/{fundId}/subscribe` | Suscribirse a un fondo |
| `DELETE` | `/api/v1/funds/{fundId}/subscribe?clientId=` | Cancelar suscripcion |
| `GET` | `/api/v1/clients/{clientId}/transactions` | Historial de transacciones |
| `PATCH` | `/api/v1/clients/{clientId}/preferences` | Configurar notificaciones |

### Ejemplos

```bash
# Listar fondos
curl http://localhost:8080/api/v1/funds

# Suscribirse
curl -X POST http://localhost:8080/api/v1/funds/1/subscribe \
  -H "Content-Type: application/json" \
  -d '{"clientId": "client-uuid"}'

# Cancelar
curl -X DELETE "http://localhost:8080/api/v1/funds/1/subscribe?clientId=client-uuid"

# Historial
curl http://localhost:8080/api/v1/clients/client-uuid/transactions
```

## Swagger UI

Acceder a: http://localhost:8080/swagger-ui.html

## Postman

Se incluye una coleccion de Postman lista para importar en `postman/ThinkUs_Prueba_Tecnica.json`.
Contiene todos los endpoints pre-configurados con variables de entorno.

## Requisitos

- Java 25
- MongoDB corriendo en `localhost:27017`
- Maven 3.9+

## Ejecucion

```bash
# Compilar y ejecutar tests
mvn clean verify

# Ejecutar
mvn spring-boot:run

# Solo tests
mvn test
```

## Pruebas

```bash
# Ver reporte de cobertura
# target/site/jacoco/index.html
```

## Despliegue AWS

El template CloudFormation en `deploy/cloudformation/btg-pactual-platform.yml` crea:
- VPC con subnets publicas/privadas
- Application Load Balancer
- ECS Fargate (2 tareas)
- Amazon DocumentDB (MongoDB-compatible)
- ECR Repository
- CloudWatch Logs

```bash
aws cloudformation create-stack \
  --stack-name btg-pactual-platform \
  --template-body file://deploy/cloudformation/btg-pactual-platform.yml \
  --parameters ParameterKey=Environment,Value=production \
               ParameterKey=DockerImageUri,Value=<tu-ecr-uri>
```

## Parte 2 - Solucion SQL

Ver `src/main/resources/sql/part2-solution.sql`

## Sugerencias para Futuras Implementaciones

- Autenticacion con JWT / OAuth2
- Auditoria de transacciones con eventos de dominio
- Integracion real con proveedores de email (SendGrid) y SMS (Twilio)
- Cache con Redis para consulta de fondos
- Rate limiting por cliente
- Webhooks para notificaciones en tiempo real
- Dashboard con metricas de suscripciones
- Soporte para multiples monedas
