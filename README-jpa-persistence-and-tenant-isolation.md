# [AV2][US-93] Consolidar evidencia de persistencia y aislamiento JPA

> **Sprint 2 · S2-T26.1 y S2-T26.2**  
> **Coordinador transversal:** Anderson (Sales)  
> **Participantes:** Líderes de contexto de IAM, Profile, Inventory, Sales, Chatbot, Notification y Subscription  
> **Rama:** `feature/sales-iot-scale-av2`  
> **Entorno de ejecución:** AV2 Local (H2 modo PostgreSQL) y Cloud (PostgreSQL en Supabase / Render)  
> **PR sugerido:** `https://github.com/LernenLabs/adm-entreprenly-backend/pull/new/feature/sales-iot-scale-av2` (base: `develop`)

---

## 1. Alcance y Objetivos de la Historia Transversal

Esta historia transversal consolida la evidencia formal de que la arquitectura backend de Entreprenly cumple de extremo a extremo con los dos principios fundamentales de datos de la plataforma:
1. **Persistencia JPA confiable e íntegra (S2-T26.1):** Los datos y sus relaciones (mapeos en cascada, colecciones embebidas `@ElementCollection`, campos de auditoría) persisten de manera duradera tras reiniciar la sesión o limpiar el contexto de persistencia (`flush` y `clear`).
2. **Aislamiento estricto por comerciante (S2-T26.2):** Garantía multitenant a nivel de modelo de dominio y repositorios Spring Data JPA. Ningún comerciante puede leer, consultar, modificar ni eliminar datos pertenecientes a otro comerciante.

---

## 2. Matriz de Commits y PRs por Bounded Context

A continuación se registra la contribución de cada contexto para el Sprint 2:

| Bounded Context | Responsable / Líder | PR / Commit en `develop` | Tarea / Historia | Evidencia de Aislamiento y Persistencia |
|---|---|---|---|---|
| **Sales** | Anderson (Coordinador) | `07c0d14`<br>`a1c621c` | [US-30] Balanza IoT (S2-T13)<br>[US-35] Acumulado Caja (S2-T15)<br>[US-93] Evidencia JPA (S2-T26) | Entidades `SalePersistenceEntity` y `CashRegisterPersistenceEntity`. Consultas aisladas por `ownerEmail`. Pruebas en `CashRegisterTest`, `SaleCommandServiceImplTest`, `CashRegisterQueryServiceImplTest` y `JpaPersistenceAndTenantIsolationTest`. |
| **Inventory** | Equipo Inventory | Base DDD | Gestión de productos y lotes FIFO | Entidades `UnitProductPersistenceEntity`, `WeightProductPersistenceEntity`, `UnitLotPersistenceEntity`, `WeightLotPersistenceEntity`. Tablas particionadas lógicamente con `owner_email`. |
| **Profile** | Equipo Profile | PR #19 (`df39931`, `09f016d`)<br>PR #5 (`59426d2`, `d29a736`)<br>PR #3 (`e231d6d`, `32fa21f`) | Verificación de cuenta y reglas AV2 | `ProfilePersistenceEntity` con preferencias (`PreferencesEmbeddable`) y avatar (`ProfileAvatarEntity`). Moneda por defecto `PEN`. Aislamiento por `user_id` / `email`. |
| **IAM** | Equipo IAM | PR #25 (`1d517fc`, `08baeb8`)<br>PR #21 (`6aba874`, `bb66882`) | Recuperación de clave y sesiones | `UserPersistenceEntity`, `RolePersistenceEntity`. Tokens JWT stateless con `ownerEmail` en el claim de autenticación. Pruebas en `PasswordResetTest`. |
| **Chatbot** | Equipo Chatbot | PR #12 (`aa2099f`, `294edf2`)<br>PR #10 (`703cbb2`, `6949ff2`) | Flujo conversacional y pedidos | `ConversationPersistenceEntity`, `ChatMessagePersistenceEntity`, `ChatOrderPersistenceEntity`. Pruebas en `ChatbotConversationServiceImplTest` y `ChatOrderCommandServiceImplTest`. |
| **Notification** | Equipo Notification | PR #24 (`63e79eb`, `35025ef`) | [US-14] Avisos de caducidad de lotes | `DeviceTokenPersistenceEntity`. Envíos push segmentados estrictamente por `ownerEmail`. Pruebas en `LotExpiryNoticeCommandServiceImplTest`. |
| **Subscription** | Equipo Subscription | Commit `53352cd` | Control de planes y cancelación | `SubscriptionPersistenceEntity`, `PaymentPersistenceEntity`. Pruebas en `SubscriptionTests` y `ScheduledCancellationTests`. |

---

## 3. Arquitectura del Aislamiento Multitenant JPA

El aislamiento por comerciante se asegura en 3 capas complementarias:

1. **Capa de Seguridad (IAM):**
   - El token Bearer JWT decodificado en `BearerAuthorizationRequestFilter` extrae el `email` del comerciante en `UserDetails.getUsername()`.
2. **Capa de Interfaces y Aplicación:**
   - Todo controlador REST inyecta `@AuthenticationPrincipal UserDetails userDetails`.
   - El servicio de aplicación recibe `query.ownerEmail()` o `command.ownerEmail()` directamente de la identidad verificada, impidiendo spoofing desde el payload JSON.
3. **Capa de Repositorios Spring Data JPA:**
   - Todos los métodos de consulta declaran filtros explícitos por `ownerEmail`:
     - `findAllByOwnerEmail(String ownerEmail)`
     - `findByIdAndOwnerEmail(Long id, String ownerEmail)`
     - `findByOwnerEmailAndRegisterDate(String ownerEmail, LocalDate date)`
     - `findByOwnerEmailAndSellerIdAndRegisterDate(String ownerEmail, Long sellerId, LocalDate date)`
   - Si un comerciante malicioso intenta enviar un `id` perteneciente a otro comerciante, la consulta retorna `Optional.empty()` o lista vacía.

---

## 4. Casos de Prueba y Criterios de Aceptación (S2-T26)

La suite consolidada en [`JpaPersistenceAndTenantIsolationTest.java`](file:///d:/202602/adm-entreprenly-backend/src/test/java/online/entreprenly/platform/JpaPersistenceAndTenantIsolationTest.java) ejecuta y valida los siguientes escenarios:

### Caso 1: Reconsulta tras nueva sesión / reinicio (S2-T26.1)
- **Acción:**
  1. Comerciante A registra un producto en inventario (`Queso Paria 500g`), una venta con líneas (`SaleItemEmbeddable`) por S/ 37.00 vía Yape, y su arqueo de caja diario.
  2. Se invoca `entityManager.flush()` y `entityManager.clear()`, vaciando la memoria caché de primer nivel (L1) para forzar lectura real desde disco/base de datos.
  3. Se ejecuta la reconsulta de la venta, arqueo de caja y catálogo.
- **Resultado:**
  - `Sale` se recupera con su total íntegro (`37.00`).
  - La colección `@ElementCollection` (`sale_items`) carga todas sus líneas y subtotales asociados.
  - El arqueo de caja `CashRegister` preserva los totales por canal (`yapeTotal = 37.00`, `digitalTotal = 37.00`, `salesCount = 1`).
  - Auditoría JPA (`createdAt`, `updatedAt` de `AuditableAbstractPersistenceEntity`) está poblada automáticamente.

### Caso 2: Rechazo de lectura de datos ajenos (S2-T26.2)
- **Acción:**
  1. Comerciante A (`merchant.alpha@shop.com`) tiene registrados S/ 150.00 en efectivo, catálogo y caja.
  2. Comerciante B (`merchant.beta@shop.com`) autentica su sesión y ejecuta:
     - `saleRepository.findAllByOwnerEmail("merchant.beta@shop.com")`
     - `cashRegisterRepository.findByOwnerEmailAndRegisterDate("merchant.beta@shop.com", today)`
     - `unitProductRepository.findAllByOwnerEmail("merchant.beta@shop.com")`
- **Resultado:**
  - Todas las consultas retornan colecciones vacías o `Optional.empty()`.
  - Comerciante B no puede ver ni acceder a ninguna venta, producto o registro de caja de Comerciante A.

### Caso 3: Rechazo de acceso directo por ID ajeno (S2-T26.2)
- **Acción:**
  - Comerciante B intenta consultar la venta del Comerciante A especificando directamente su ID: `saleRepository.findByIdAndOwnerEmail(saleIdA, "merchant.beta@shop.com")`.
- **Resultado:**
  - Retorna `Optional.empty()` (HTTP 404 a nivel de API), impidiendo lectura o modificación de entidades ajenas.

### Caso 4: Coexistencia simultánea e independiente
- **Acción:**
  - Comerciante A registra S/ 80.00 (`CASH`).
  - Comerciante B registra S/ 120.00 (`YAPE`).
- **Resultado:**
  - Consulta A: 1 venta de S/ 80.00 en efectivo.
  - Consulta B: 1 venta de S/ 120.00 en Yape.
  - No existe interferencia cruzada en los acumulados de caja ni en las transacciones.

---

## 5. Instrucciones de Ejecución de Pruebas

Para reproducir la suite consolidada de persistencia y aislamiento:

```bash
./mvnw test -Dtest=JpaPersistenceAndTenantIsolationTest
```

Y para ejecutar todas las pruebas del bounded context de Sales:

```bash
./mvnw test -Dtest=CashRegisterTest,CashRegisterQueryServiceImplTest,SaleCommandServiceImplTest,IotScaleQueryServiceImplTest,JpaPersistenceAndTenantIsolationTest
```

