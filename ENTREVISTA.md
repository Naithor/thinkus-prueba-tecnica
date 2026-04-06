# Guia de Estudio - Sustencion del Proyecto BTG Pactual

## 1. Desglose del Enunciado del Problema

### Parte 1 - Fondos (80%)

**Lo que pidio el cliente:**
| Requisito | Como lo interprete | Como lo implemente |
|-----------|-------------------|-------------------|
| Suscribirse a un fondo | POST con validacion de saldo | `SubscribeFundService` con chequeo de balance |
| Cancelar suscripcion | DELETE que retorna dinero | `CancelSubscriptionService` que hace credit al cliente |
| Ver historial de transacciones | GET de todas las operaciones | `GetTransactionHistoryService` ordenado por fecha |
| Notificacion email/SMS | Preferencia configurable | Strategy pattern + `@Async` |
| Monto inicial $500.000 | Saldo inicial del cliente | `Client.newDefaultClient()` |
| Identificador unico por transaccion | UUID en cada operacion | `UUID.randomUUID().toString()` |
| Monto minimo por fondo | Validacion antes de suscribir | `client.hasSufficientBalance(fund.getMinAmount())` |
| Retorno al cancelar | Credit del minAmount | `client.credit(fund.getMinAmount())` |
| Mensaje sin saldo | Excepcion con mensaje especifico | `InsufficientBalanceException` |

**Clave de la evaluacion:** "Integridad de los datos ante peticiones simultaneas"
- Esto fue lo que mas me preocupe. Implemente 3 capas de proteccion contra race conditions.

### Parte 2 - SQL (20%)

Consulta relacional con JOINs, NOT EXISTS y NOT IN para encontrar clientes con productos disponibles solo en sucursales que visitan.

---

## 2. Decisiones Tecnologicas

### Por que Java 25?
- **Respuesta:** "Java 25 es la ultima version LTS disponible en mi entorno de desarrollo. Ofrece mejoras de rendimiento en el garbage collector, pattern matching mejorado para switch, y records estables. Es la version mas moderna y estable que tengo instalada."
- **Si preguntan por Java 26:** "Java 26 aun no es LTS. Para un proyecto de produccion en el sector financiero, siempre se prefiere una version LTS por el soporte a largo plazo."

### Por que Spring Boot 4.0.5?
- "Es la ultima version estable de Spring Boot. Incluye mejoras de seguridad, rendimiento y soporte nativo para las ultimas versiones de Java. Para un proyecto financiero, estar actualizado es importante por los parches de seguridad."

### Por que MongoDB (NoSQL)?
- **El enunciado pedia modelo NoSQL.** MongoDB fue la eleccion natural porque:
  1. **Spring Data MongoDB** tiene integracion nativa con Spring Boot
  2. Soporta **transacciones ACID** (desde MongoDB 4.0)
  3. **Optimistic Locking** nativo con `@Version`
  4. Esquema flexible: si en el futuro necesitamos agregar campos a las transacciones o clientes, no requiere migraciones de esquema
  5. **Escalabilidad horizontal**天然 - importante para una plataforma financiera que puede crecer

### Por que no DynamoDB u otro NoSQL?
- "DynamoDB es excelente pero requiere mas boilerplate con Spring Boot. MongoDB tiene un ecosistema mas maduro en el mundo Java y permite transacciones multi-documento, que es exactamente lo que necesitabamos para garantizar atomicidad entre el debito del cliente y la creacion de la transaccion."

### Por que Spring Retry?
- "Para manejar conflictos de concurrencia de forma elegante. Cuando dos hilos intentan modificar el mismo cliente simultaneamente, el optimistic locking lanza `OptimisticLockingFailureException`. En lugar de fallar, Spring Retry reintenta automaticamente la operacion hasta 3 veces con backoff de 100ms."

### Por que Lombok?
- "Reduce boilerplate significativamente. En lugar de escribir getters, setters, constructores y builders manualmente (que son propensos a errores), Lombok los genera en tiempo de compilacion. Esto hace el codigo mas limpio y mantenible."

### Por que Swagger/OpenAPI?
- "Documentacion automatica de la API. Cualquier desarrollador puede ver los endpoints, los DTOs requeridos y probar la API directamente desde el navegador. Es un estandar de la industria."

---

## 3. Arquitectura - Clean Architecture / Hexagonal

### Que es y por que la elegi?

**Clean Architecture** (tambien llamada Arquitectura Hexagonal o Ports & Adapters) separa el codigo en capas segun su responsabilidad:

```
                    ┌─────────────────────┐
                    │   Adapters IN       │  ← Controllers, DTOs
                    │   (Framework)       │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Domain (Core)     │  ← Reglas de negocio PURAS
                    │   - Models          │  - Sin dependencias externas
                    │   - Ports (interfaces)│
                    │   - Services        │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Adapters OUT      │  ← MongoDB, Email, SMS
                    │   (Infraestructura) │
                    └─────────────────────┘
```

**Por que la elegi:**
1. **Independencia del framework:** El dominio no sabe que existe Spring Boot ni MongoDB. Si manana quiero cambiar a PostgreSQL o a otro framework web, solo cambio los adapters, no toco la logica de negocio.
2. **Testeabilidad:** Los servicios de dominio se pueden testear con mocks de las interfaces (ports), sin necesidad de base de datos real.
3. **Mantenibilidad:** Cada capa tiene una responsabilidad clara. Un desarrollador nuevo sabe donde buscar.
4. **Sector financiero:** En banca, las reglas de negocio cambian menos que la infraestructura. Tenerlas aisladas protege el core del sistema.

### Que son los Ports?

Son **interfaces** que definen contratos:
- **Ports IN (Use Cases):** Lo que el sistema puede hacer. Ej: `SubscribeFundUseCase.execute(clientId, fundId)`
- **Ports OUT:** Lo que el sistema necesita del exterior. Ej: `ClientPort.findById(id)`, `TransactionPort.save(tx)`

El dominio **depende de interfaces**, no de implementaciones concretas (DIP - Dependency Inversion Principle).

### Que son los Adapters?

Son las **implementaciones concretas** de los ports:
- **Adapters IN:** Controllers REST que reciben HTTP calls y llaman a los use cases
- **Adapters OUT:** Repositorios MongoDB que implementan los ports de salida

---

## 4. Patrones de Diseño Aplicados

### Ports & Adapters (Hexagonal)
- **Que es:** Separar dominio de infraestructura usando interfaces como contratos
- **Donde:** Toda la arquitectura del proyecto
- **Por que:** Permite cambiar MongoDB por otra DB sin tocar la logica de negocio

### Strategy Pattern
- **Que es:** Definir una familia de algoritmos intercambiables
- **Donde:** `NotificationPort` con `EmailNotificationService` y `SmsNotificationService`
- **Por que:** Si manana queremos agregar notificacion por WhatsApp, solo creamos una nueva clase que implemente `NotificationPort` sin modificar el dominio
- **Ejemplo de como explicarlo:**
  ```
  "En vez de tener un if/else gigante para decidir como notificar,
  defino una interfaz NotificationPort y cada canal (email, SMS)
  es una implementacion. El cliente elige su preferencia y el
  sistema usa la implementacion correcta automaticamente."
  ```

### Factory Method
- **Que es:** Un metodo que encapsula la creacion de objetos complejos
- **Donde:** `Client.newDefaultClient()` y `Transaction.newSubscribe()` / `Transaction.newCancel()`
- **Por que:** Garantiza que los objetos se creen siempre con las reglas de negocio correctas (saldo inicial, UUID, timestamp)

### Retry Pattern
- **Que es:** Reintentar una operacion fallida automaticamente
- **Donde:** `@Retryable` en `SubscribeFundService` y `CancelSubscriptionService`
- **Por que:** En concurrencia, el optimistic locking puede fallar. En lugar de devolver error al usuario, reintentamos automaticamente

### Repository Pattern
- **Que es:** Abstraer el acceso a datos detras de una interfaz
- **Donde:** `ClientPort`, `FundPort`, `TransactionPort`
- **Por que:** El dominio no sabe que estamos usando MongoDB. Podriamos cambiar a JDBC, DynamoDB, o un API REST sin tocar el dominio

---

## 5. Principios SOLID - Como los aplique

### S - Single Responsibility Principle
**Cada clase tiene una sola razon para cambiar:**
- `SubscribeFundService` → solo sabe suscribir
- `CancelSubscriptionService` → solo sabe cancelar
- `GetTransactionHistoryService` → solo sabe consultar historial
- `EmailNotificationService` → solo envia emails
- `FundController` → solo expone endpoints de fondos

### O - Open/Closed Principle
**Abierto a extension, cerrado a modificacion:**
- `NotificationPort` es una interfaz. Si quiero agregar WhatsApp, creo `WhatsAppNotificationService` sin tocar `SubscribeFundService`
- Los DTOs son records inmutables. Si necesito un nuevo campo, creo un nuevo record

### L - Liskov Substitution Principle
**Las implementaciones deben ser sustituibles por su interfaz:**
- `EmailNotificationService` y `SmsNotificationService` son intercambiables porque ambos implementan `NotificationPort`
- El dominio no distingue cual esta usando

### I - Interface Segregation Principle
**Interfaces pequenas y especificas:**
- `ClientPort` solo tiene `findById` y `save`
- `FundPort` solo tiene `findById` y `findAll`
- No tengo una mega-interfaz `Repository` con 20 metodos

### D - Dependency Inversion Principle
**El dominio depende de abstracciones, no de concreciones:**
- `SubscribeFundService` depende de `ClientPort` (interfaz), no de `ClientMongoAdapter` (implementacion)
- Los adapters IN dependen de los use cases (interfaces), no de los servicios directamente

---

## 6. Concurrencia Segura - El Punto Critico

**El enunciado dice:** "El equipo tecnico evalua con lupa la integridad de los datos ante peticiones simultaneas"

### El problema
Si dos hilos intentan suscribir al mismo cliente al mismo tiempo:
```
Hilo A: Lee saldo = 500,000
Hilo B: Lee saldo = 500,000
Hilo A: Resta 75,000 → saldo = 425,000 → guarda
Hilo B: Resta 75,000 → saldo = 425,000 → guarda (DEBERIA ser 350,000!)
```

### Mi solucion en 3 capas

**Capa 1: Optimistic Locking (`@Version`)**
```java
@Document(collection = "clients")
public class Client {
    @Version
    private Long version;
    // ...
}
```
- MongoDB guarda un numero de version en cada documento
- Si Hilo A lee version=0 y Hilo B lee version=0
- Hilo A guarda → version se convierte en 1
- Hilo B intenta guardar con version=0 → MongoDB rechaza → `OptimisticLockingFailureException`

**Capa 2: MongoDB Transactions (`@Transactional`)**
```java
@Transactional
public Transaction execute(String clientId, Integer fundId) {
    // Todo esto es atomico:
    client.debit(amount);     // 1. Debitar
    clientPort.save(client);  // 2. Guardar cliente
    transactionPort.save(tx); // 3. Guardar transaccion
}
```
- Si algo falla en el medio, todo se revierte
- No puede quedar el cliente debitado sin la transaccion registrada

**Capa 3: Retry Automatico (`@Retryable`)**
```java
@Retryable(retryFor = OptimisticLockingFailureException.class, 
           maxAttempts = 3, 
           backoff = @Backoff(delay = 100))
```
- Si hay conflicto de version, reintentamos automaticamente
- El segundo intento leera el saldo actualizado y procedera correctamente
- Backoff de 100ms para dar tiempo al otro hilo de terminar

**Capa 4: Validacion dentro de la transaccion**
- La validacion de saldo (`hasSufficientBalance`) se hace DENTRO del metodo `execute`, no antes
- Esto evita el problema TOCTOU (Time Of Check to Time Of Use)

### Test de concurrencia
```java
@Test
void shouldHandleConcurrentSubscriptionsSafely() {
    // 10 hilos simultaneos
    // Todos intentan suscribirse
    // Verificamos que el saldo final sea correcto
}
```

---

## 7. Modelo de Datos NoSQL

### Cliente
```json
{
  "_id": "uuid-generado",
  "version": 0,
  "balance": 500000,
  "notificationPreference": "EMAIL",
  "contactInfo": "cliente@email.com"
}
```

### Fondo
```json
{
  "_id": 1,
  "name": "FPV_BTG_PACTUAL_RECAUDADORA",
  "minAmount": 75000,
  "category": "FPV"
}
```

### Transaccion
```json
{
  "_id": "uuid-generado",
  "clientId": "uuid-del-cliente",
  "fundId": 1,
  "type": "SUBSCRIBE",
  "amount": 75000,
  "timestamp": "2025-06-20T10:00:00Z"
}
```

### Por que este modelo?
- **Cliente separado de Transacciones:** Permite consultar historial sin cargar el documento del cliente
- **Transacciones como documentos independientes:** Cada operacion tiene su propio ID unico (requisito del enunciado)
- **Fondos como catalogo:** Se cargan una vez al inicio, no cambian frecuentemente
- **`@Version` en Client:** Esencial para optimistic locking

---

## 8. Flujo de Implementacion (Como empece)

1. **Entendi el problema:** Lei el enunciado completo, identifique las 4 funcionalidades principales y las reglas de negocio
2. **Diseñe el modelo de datos:** Defini las 3 colecciones (Client, Fund, Transaction) y sus relaciones
3. **Elegi la arquitectura:** Clean Architecture para separar responsabilidades
4. **Cree el esqueleto:** pom.xml con dependencias, estructura de paquetes
5. **Implemente el dominio primero:** Models → Ports → Services (sin pensar en MongoDB ni HTTP)
6. **Implemente adapters:** Controllers REST → Repositorios MongoDB → Notificaciones
7. **Manejo de excepciones:** GlobalExceptionHandler con mensajes claros
8. **Tests:** Unitarios con mocks → Integracion con MongoDB embebido → Concurrencia
9. **Infraestructura:** application.yml, CloudFormation, README

**Por que este orden?** Porque en Clean Architecture, el dominio es lo mas importante. Si el dominio esta bien, los adapters son triviales.

---

## 9. Parte 2 - Solucion SQL

### La consulta
```sql
SELECT DISTINCT c.nombre
FROM Cliente c
JOIN Inscripcion i ON c.id = i.idCliente
WHERE NOT EXISTS (
    SELECT 1
    FROM Disponibilidad d
    WHERE d.idProducto = i.idProducto
      AND d.idSucursal NOT IN (
          SELECT v.idSucursal
          FROM Visitan v
          WHERE v.idCliente = c.id
      )
);
```

### Como explicarla paso a paso

**Pregunta:** "Obtener clientes con productos disponibles SOLO en sucursales que visitan"

**Logica:**
1. **JOIN Inscripcion:** Obtenemos que productos tiene cada cliente
2. **NOT EXISTS + NOT IN:** Esta es la clave. Buscamos clientes donde NO EXISTA ninguna sucursal donde el producto este disponible que el cliente NO visite
3. **DISTINCT:** Evitamos duplicados si un cliente tiene multiples productos que cumplen

**Ejemplo concreto:**
```
Producto P1 disponible en: S1, S2
Cliente C1 visita: S1, S2 → CUMPLE (visita TODAS donde P1 esta disponible)
Cliente C2 visita: S1     → NO CUMPLE (P1 tambien esta en S2, que C2 no visita)
Cliente C3 visita: S1, S2, S3 → CUMPLE (visita al menos todas donde P1 esta)
```

**Por que NOT EXISTS y no JOIN simple?**
- Un JOIN simple nos daria productos donde el cliente visita AL MENOS UNA sucursal
- NOT EXISTS nos asegura que el cliente visita TODAS las sucursales donde el producto esta disponible

---

## 10. Testing Strategy

### Piramide de tests aplicada

```
         /\
        /  \  ← 1 Integration Test (MongoDB embebido)
       /----\
      /      \ ← 7 Controller Tests (MockMvc)
     /--------\
    /          \ ← 10 Service Tests (unitarios con mocks)
   /------------\
  /  1 Concurrency Test (ExecutorService)
 /________________\
```

### Por que este enfoque?
- **Tests unitarios de servicios:** Son rapidos, no necesitan base de datos. Cubren la logica de negocio
- **Tests de controllers:** Verifican que los endpoints HTTP funcionan correctamente
- **Test de integracion:** Verifica que todo el contexto de Spring arranca y MongoDB funciona
- **Test de concurrencia:** Especifico para el requisito critico del enunciado

### Patron AAA
Todos los tests siguen **Arrange-Act-Assert**:
```java
@Test
void shouldSubscribeSuccessfully() {
    // Arrange: Preparar datos y mocks
    when(fundPort.findById(1)).thenReturn(Optional.of(fund));
    
    // Act: Ejecutar la operacion
    Transaction result = subscribeFundService.execute("client-1", 1);
    
    // Assert: Verificar resultados
    assertThat(result.getType()).isEqualTo(TransactionType.SUBSCRIBE);
}
```

---

## 11. Preguntas Frecuentes de Entrevista

### P: Por que no usaste una base de datos relacional?
**R:** "El enunciado especificamente pedia un modelo de datos NoSQL. MongoDB fue la eleccion natural por su integracion con Spring Boot y soporte para transacciones ACID. Sin embargo, para un sistema financiero real, consideraria PostgreSQL con JSONB para tener lo mejor de ambos mundos."

### P: Como manejas la seguridad?
**R:** "En esta implementacion, el enfoque fue la integridad de datos ante concurrencia. Para produccion agregaria: autenticacion JWT, validacion de entrada con `@Valid`, rate limiting, y encriptacion de datos sensibles. El CloudFormation template incluye Security Groups para aislamiento de red."

### P: Que pasa si MongoDB se cae?
**R:** "Spring Boot tiene circuit breaker patterns. Podria agregar Resilience4j para implementar un circuit breaker que devuelva respuestas cacheadas o mensajes de error amigables cuando MongoDB no este disponible."

### P: Como escalaria este sistema?
**R:** "MongoDB escala horizontalmente con sharding. El CloudFormation template ya usa ECS Fargate con 2 tareas detras de un ALB. Para mas carga, aumentaria el `DesiredCount` en el servicio ECS y habilitaria auto-scaling basado en CPU/memory."

### P: Por que no usaste CQRS?
**R:** "Para el alcance del proyecto, CQRS seria over-engineering. Las operaciones de lectura y escritura son simples y no hay conflictos de modelo. Si el sistema creciera con queries complejas de reportes, CQRS con Event Sourcing seria una evolucion natural."

### P: Que es el Optimistic Locking y por que lo usaste?
**R:** "Es una estrategia de control de concurrencia donde en lugar de bloquear el registro (pessimistic locking), guardamos un numero de version. Al actualizar, verificamos que la version no haya cambiado. Si cambio, otro hilo modifico el registro y debemos reintentar. Es mas eficiente que los locks porque no bloquea la base de datos."

### P: Como verificas que la concurrencia funciona?
**R:** "Tengo un test especifico que lanza 10 hilos simultaneos intentando suscribir al mismo cliente. Cada hilo tiene su propio cliente con $500,000. El test verifica que todas las suscripciones se completen correctamente y que los saldos finales sean consistentes."

### P: Que harías diferente si tuvieras mas tiempo?
**R:** 
1. "Agregar autenticacion JWT con roles (admin, cliente)"
2. "Implementar eventos de dominio para auditoria"
3. "Integracion real con SendGrid/Twilio"
4. "Cache con Redis para el catalogo de fondos"
5. "Dashboard con metricas en tiempo real"
6. "Pruebas de carga con Gatling o JMeter"

### P: Explica el patron Strategy que usaste
**R:** "Defini una interfaz `NotificationPort` con un metodo `send()`. Luego cree dos implementaciones: `EmailNotificationService` y `SmsNotificationService`. El cliente elige su preferencia y el sistema usa la implementacion correspondiente. Si manana quiero agregar WhatsApp, solo creo `WhatsAppNotificationService` sin tocar la logica existente."

### P: Por que los DTOs son records?
**R:** "Los records de Java son inmutables por defecto, tienen equals/hashCode/toString automaticos, y su intencion es clara: son contenedores de datos. Para DTOs que solo transportan informacion entre capas, son perfectos porque no deben ser modificados despues de creados."

### P: Que es JaCoCo y por que lo incluiste?
**R:** "JaCoCo es una herramienta de medicion de cobertura de codigo. La inclui para asegurar que los tests cubran al menos el 80% del codigo. En el sector financiero, la cobertura de tests es critica porque un bug puede significar perdidas economicas reales."

---

## 12. Numeros Clave para Mencionar

| Metrica | Valor |
|---------|-------|
| Archivos de codigo | 62 |
| Lineas de codigo | ~3,747 |
| Tests | 19 (todos passing) |
| Cobertura | >= 80% (JaCoCo) |
| Endpoints API | 5 |
| Patrones de diseño | 5 |
| Principios SOLID | 5/5 aplicados |
| Capas de proteccion de concurrencia | 4 |

---

## 13. Diagrama Mental Rapido para la Entrevista

```
Problema → Gestion de fondos de inversion
Solucion → API REST con Spring Boot + MongoDB
Arquitectura → Clean Architecture (Hexagonal)
Seguridad → Optimistic Locking + Transactions + Retry
Tests → 19 tests, patron AAA, JaCoCo >= 80%
Deploy → AWS CloudFormation (ECS + DocumentDB + ALB)
Extras → Swagger, Postman, SQL Part 2
```

---

## 14. Frases Clave para la Entrevista

- "Aplique **Clean Architecture** para que el dominio fuera independiente de la infraestructura"
- "La **integridad de datos ante concurrencia** fue mi prioridad numero uno"
- "Cada servicio tiene **una sola responsabilidad** (SRP)"
- "Los **ports son interfaces** que definen contratos, no implementaciones"
- "Los **tests siguen el patron AAA** para claridad y mantenibilidad"
- "El **optimistic locking con @Version** previene race conditions sin bloquear la BD"
- "El **patrón Strategy** hace que el sistema sea extensible a nuevos canales de notificacion"
- "La **validacion de saldo se hace dentro de la transaccion**, no antes, para evitar TOCTOU"
