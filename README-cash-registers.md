# [AV2][US-35] Persistir el acumulado de caja por medio de pago

> **Sprint 2 · S2-T15.1 y S2-T15.2**  
> **Rama:** `feature/sales-iot-scale-av2`  
> **Bounded Context:** `Sales`  
> **PR sugerido:** `https://github.com/LernenLabs/adm-entreprenly-backend/pull/new/feature/sales-iot-scale-av2` (base: `develop`)

---

## 1. Alcance

Completar el contrato de `/api/v1/cash-registers` para proveer los totales del día acumulados y persistidos por comerciante y medio de pago:
- Registro de ventas en efectivo (`CASH`) y canales digitales (`YAPE`, `PLIN`, `CARD`).
- Acumulación automática y persistencia inmediata en la tabla `cash_registers` al confirmar una venta (`POST /api/v1/sales` o confirmación de pedidos vía chatbot).
- Consulta y reconsulta del día vía `GET /api/v1/cash-registers` (con soporte opcional de fecha `?date=YYYY-MM-DD` y `?sellerId=...`).
- Aislamiento estricto multitenant por comerciante (`ownerEmail` autenticado en JWT).
- Coordinación con el cliente Android para actualización inmediata de la UI tras cada venta y formateo según la moneda del perfil del usuario (`Preferences.currency`).

---

## 2. Contrato de API

Endpoint protegido mediante JWT: `Authorization: Bearer <token>`

### `GET /api/v1/cash-registers`
**Parámetros query (opcionales):**
- `date`: Fecha en formato ISO (`YYYY-MM-DD`). Por defecto es el día de hoy en la zona de negocio (`America/Lima`).
- `sellerId`: Identificador del vendedor/cajero (opcional).

**Respuesta 200 OK:**
```json
{
  "id": 1,
  "sellerId": 1,
  "ownerEmail": "merchant@shop.com",
  "date": "2026-11-11",
  "cashTotal": 150.00,
  "yapeTotal": 50.00,
  "plinTotal": 25.00,
  "cardTotal": 30.00,
  "digitalTotal": 105.00,
  "total": 255.00,
  "salesCount": 7,
  "cashSalesCount": 4,
  "digitalSalesCount": 3,
  "cash": 150.00,
  "digital": 105.00,
  "totalAmount": 255.00,
  "totalSales": 7
}
```

*Nota:* Se incluyen alias en el JSON (`cash`, `digital`, `totalAmount`, `totalSales`) para máxima compatibilidad con las propiedades del DTO en la app Android.

---

## 3. Arquitectura y Persistencia DDD

```
sales/
├── domain/
│   ├── model/
│   │   ├── aggregates/CashRegister.java        ← Aggregate Root (acumula ventas y redondea a 2 decimales)
│   │   └── queries/GetCashRegisterByDateQuery.java
│   └── repositories/CashRegisterRepository.java ← Port del repositorio
├── application/
│   ├── queryservices/CashRegisterQueryService.java
│   ├── internal/queryservices/CashRegisterQueryServiceImpl.java
│   └── internal/commandservices/SaleCommandServiceImpl.java ← Actualiza CashRegister al registrar venta
└── infrastructure/persistence/jpa/
    ├── entities/CashRegisterPersistenceEntity.java  ← Tabla 'cash_registers'
    ├── repositories/CashRegisterPersistenceRepository.java
    ├── adapters/CashRegisterRepositoryImpl.java
    └── assemblers/CashRegisterPersistenceAssembler.java
```

---

## 4. Matriz de criterios de aceptación y evidencia

| # | Criterio | Datos de prueba | Resultado esperado | Evidencia |
|---|---|---|---|:---:|
| 1 | **Ventas en efectivo** | Venta de S/ 51.00 (`CASH`) | `cashTotal = 51.00`, `digitalTotal = 0.00`, `total = 51.00`, `salesCount = 1`, `cashSalesCount = 1` | `SaleCommandServiceImplTest` |
| 2 | **Ventas digitales multicanal** | Venta Yape S/ 45.00, Plin S/ 20.00, Card S/ 50.00 | `yapeTotal = 45.00`, `plinTotal = 20.00`, `cardTotal = 50.00`, `digitalTotal = 115.00`, `digitalSalesCount = 3` | `SaleCommandServiceImplTest` |
| 3 | **Total general contrastado** | Efectivo S/ 30.00 + Digitales S/ 115.00 | `total = 145.00`, comprobando `cashTotal + digitalTotal == total` | `CashRegisterTest` |
| 4 | **Reconsulta del día** | Consulta reiterada de `GET /api/v1/cash-registers` | Devuelve el registro persistido sin reprocesar; si el día no tiene ventas, inicializa y devuelve ceros (`0.00`) | `CashRegisterQueryServiceImplTest` |
| 5 | **Aislamiento entre comerciantes** | Comerciante A registra S/ 100 (`CASH`), Comerciante B registra S/ 75 (`YAPE`) | Comerciante A solo visualiza `S/ 100.00`; Comerciante B solo visualiza `S/ 75.00` | `CashRegisterQueryServiceImplTest` |

---

## 5. Pruebas unitarias ejecutadas

- `CashRegisterTest`:
  - `emptyRegisterHasZeroTotals`
  - `recordsCashSaleProperly`
  - `recordsDigitalSalesByChannelAndAggregatesProperly`
  - `contrastsCashAndDigitalChannelsAccurately`
  - `buildsFromExistingSalesIgnoringCancelledSales`
- `CashRegisterQueryServiceImplTest`:
  - `returnsZeroedRegisterWhenNoSalesAndNoRegisterExistYet`
  - `returnsExistingPersistedRegisterOnReconsult`
  - `enforcesStrictTenantIsolationBetweenMerchants`
- `SaleCommandServiceImplTest`:
  - `updatesCashRegisterUponRegisteringCashSale`
  - `accumulatesCashAndDigitalSalesAcrossChannelsAndMaintainsTotals`
  - `isolatesCashRegistersBetweenMerchants`
