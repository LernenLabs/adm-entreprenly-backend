# [AV2][US-30] Integrar lectura de balanza IoT con Ventas (Backend)

> **Sprint 2 · S2-T13.1 y S2-T13.2**  
> **Rama:** `feature/sales-iot-scale-av2`  
> **Commit:** `07c0d14` (feat(sales): add IoT scale REST contract (US-30, S2-T13))  
> **PR sugerido:** `https://github.com/LernenLabs/adm-entreprenly-backend/pull/new/feature/sales-iot-scale-av2` (base: `develop`)

---

## 1. Alcance

Definición e integración del contrato REST para periférico de balanza IoT en el bounded context de **Sales**:
- `GET /api/v1/iot-scale`: Retorna el estado de conexión de la balanza (`connected: true/false`).
- `GET /api/v1/iot-scale/weight`: Retorna la lectura de peso actual en kilogramos con precisión de 3 decimales (`weightKg`, `unit: "kg"`).
- Coordinación con el cliente Android (Kotlin) para:
  - Apertura del diálogo de pesaje con polling/consulta de estado.
  - Confirmación automática del peso tras **800 ms** de lectura estable.
  - Alternativa manual de ingreso de peso en caso de desconexión o fallo del bridge (degradación graceful con error `422` y código `BUSINESS_RULE_VIOLATION`).
  - Control de stock acumulado para validar que la cantidad pesada no supere el inventario disponible antes de registrar la línea de venta.

---

## 2. Contrato de API

Ambos endpoints requieren cabecera `Authorization: Bearer <JWT>`.

### 2.1. Estado de conexión
`GET /api/v1/iot-scale`

**Respuesta 200 OK:**
```json
{
  "connected": true
}
```

### 2.2. Lectura de peso
`GET /api/v1/iot-scale/weight`

**Respuesta 200 OK:**
```json
{
  "weightKg": 1.250,
  "unit": "kg"
}
```

**Respuesta 422 Unprocessable Entity (Balanza desconectada / error de hardware):**
```json
{
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "Business rule violation: scale-disconnected",
  "details": "Scale is not reachable; use manual weight entry"
}
```

---

## 3. Variables de entorno

| Variable | Defecto | Descripción |
|---|---|---|
| `IOT_SCALE_BASE_URL` | *(vacío)* | URL base del bridge IoT (ej. `http://scale-bridge:8090`). Si está vacío (modo stub en desarrollo/AV2), retorna `connected: false` y `0.0 kg` permitiendo operar la suite sin hardware. |

---

## 4. Matriz de escenarios y criterios de aceptación

| # | Escenario | Datos de prueba | Resultado esperado | ¿Ejecutado con Hardware? |
|---|---|---|---|:---:|
| 1 | **Balanza calibrada y conectada** | Bridge activo con peso `1.450 kg` | `GET /api/v1/iot-scale` → `200 { "connected": true }`<br>`GET /api/v1/iot-scale/weight` → `200 { "weightKg": 1.45, "unit": "kg" }` | Simulado / Bridge local |
| 2 | **Desconexión de balanza** | Bridge apagado o desconectado | `GET /api/v1/iot-scale` → `200 { "connected": false }`<br>`GET /api/v1/iot-scale/weight` → `422 BUSINESS_RULE_VIOLATION` (activa fallback manual en Android) | Simulado / Bridge local |
| 3 | **Peso superior al stock** | Producto con stock disponible = `2.0 kg`, peso medido = `3.5 kg` | La app Android valida contra el stock del catálogo (`CatalogItem.stock`) e impide añadir la línea de venta hasta ajustar el peso. | Software / Test de integración |
| 4 | **Unidad en kg** | Respuesta de lectura | Campo `unit: "kg"` y redondeo a 3 decimales (`Math.round(weight * 1000.0) / 1000.0`). | Validado por contrato |
| 5 | **Cálculo de subtotal** | `weightKg: 2.500`, precio unitario por kg = `S/ 8.00` | Subtotal calculado = `S/ 20.00` (recalculado por `SaleItem.computeSubtotal`). | Validado por backend `SaleItem` |

---

## 5. Ejecución de pruebas unitarias

```bash
./mvnw test -Dtest=IotScaleQueryServiceImplTest
```

Cubre 6 casos de prueba unitarios:
1. `statusReturnsDisconnectedWhenNoBridgeUrlIsConfigured`
2. `statusReturnsValidationErrorWhenOwnerEmailIsBlank`
3. `statusReturnsValidationErrorWhenOwnerEmailIsNull`
4. `weightReturnsZeroKgWhenNoBridgeUrlIsConfigured`
5. `weightReturnsValidationErrorWhenOwnerEmailIsBlank`
6. `weightReturnsValidationErrorWhenOwnerEmailIsNull`
