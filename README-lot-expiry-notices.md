# US-14 · Avisos de caducidad de lotes (backend)

Cada día el backend busca los lotes con stock que vencen entre hoy y los próximos 5 días (fechas UTC, igual que la alerta de inventario) y envía una notificación push al Android de su dueño. El aviso incluye producto, días restantes y fecha de vencimiento. Cada lote se avisa una sola vez por día. Los lotes sin stock no se avisan (se muestran como Agotado).

## Flujo

1. La app Android obtiene su token FCM y, con el permiso de notificaciones concedido, lo registra con `POST /api/v1/notifications/device-tokens` (`{"token": "...", "platform": "android"}`).
2. A las 08:00 (America/Lima) corre el job diario. Para cada lote por vencer revisa:
   - la preferencia `notificationSettings.stockAlerts` del perfil: si está apagada no se envía;
   - que el dueño tenga al menos un dispositivo registrado: sin dispositivo no se envía;
   - que el lote no se haya avisado hoy.
3. Se envía el mensaje por FCM HTTP v1. Si FCM responde que el token ya no existe, se borra. Si el envío falla o FCM no está configurado, el lote se reintenta en la siguiente ejecución.

Permiso denegado en Android = la app no registra el token (o llama a `DELETE /api/v1/notifications/device-tokens?token=...` si lo revoca después, o al cerrar sesión). Sin token, el backend no envía nada.

## Contrato para Android

Canal de notificación: `lot_expiry`. El mensaje trae `notification` (título y texto ya localizados según el idioma del perfil) y estas claves en `data`:

| Clave | Ejemplo | Uso |
| --- | --- | --- |
| type | lot_expiring | Identifica el aviso. |
| lotType | unit | Tipo de lote para abrir el detalle. |
| lotId | 7 | Id del lote para abrir el detalle. |
| lotCode | LOT-A | Código del lote. |
| productId | 3 | Id del producto. |
| productName | Leche Gloria | Nombre del producto. |
| daysLeft | 3 | Días restantes (0 = hoy). |
| expiryDate | 2026-11-15 | Fecha de vencimiento ISO. |

Al tocar la notificación, la app abre el detalle del lote con `lotType` y `lotId`.

## Endpoints

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | /api/v1/notifications/device-tokens | Registra el token del dispositivo (o lo reasigna a la cuenta actual). |
| DELETE | /api/v1/notifications/device-tokens?token=... | Elimina el token (idempotente, solo los propios). |
| POST | /api/v1/notifications/lot-expiry/dispatch?force=false | Ejecuta el chequeo ahora solo para la cuenta autenticada y devuelve un resumen. Con `force=true` reenvía aunque ya se haya avisado hoy. |

## Variables de entorno

| Variable | Defecto | Descripción |
| --- | --- | --- |
| LOT_EXPIRY_NOTICES_ENABLED | true | Activa el job diario. |
| LOT_EXPIRY_NOTICES_CRON | 0 0 8 * * * | Horario del job (cron de Spring). |
| LOT_EXPIRY_NOTICES_ZONE | America/Lima | Zona horaria del cron. |
| FCM_ENABLED | false | Activa el envío real por FCM. |
| FCM_PROJECT_ID | | Id del proyecto de Firebase. |
| FCM_CLIENT_EMAIL | | `client_email` de la cuenta de servicio. |
| FCM_PRIVATE_KEY | | `private_key` de la cuenta de servicio (con `\n` literales). |
| FCM_CHANNEL_ID | lot_expiry | Canal de Android. |

Sin FCM configurado el backend funciona igual: registra en el log que no entregó el aviso y no marca el lote como avisado. Las tablas `notification_device_tokens` y `notification_lot_expiry_logs` se crean solas (ddl-auto=update).

## Cómo probar

```bash
./mvnw test
```

Prueba manual con datos de prueba:

1. Crear un producto y un lote con stock cuya fecha de vencimiento sea dentro de 3 días.
2. Registrar un token de prueba: `POST /api/v1/notifications/device-tokens`.
3. Llamar `POST /api/v1/notifications/lot-expiry/dispatch?force=true` y revisar el resumen (`lotsNotified`, `deliveries`, `skippedByPreference`, `skippedWithoutDevice`).
4. Repetir con `stockAlerts` apagado en el perfil (debe salir `skippedByPreference`) y sin token registrado (debe salir `skippedWithoutDevice`).

## Evidencia (plantilla)

- Commit / PR:
- Entorno (perfil, FCM sí/no, dispositivo):
- Datos de prueba (producto, lote, vencimiento):
- Resultado con permiso concedido (aviso en segundo plano):
- Resultado con permiso o preferencia desactivados:
- Resultado al tocar el aviso (detalle del lote):
