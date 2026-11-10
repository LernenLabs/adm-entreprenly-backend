# US-60 · Recuperación de contraseña

Desde Iniciar sesión, el usuario puede solicitar un enlace de recuperación. La respuesta es la misma para correos registrados y desconocidos. El enlace vence en 10 minutos y se puede usar una sola vez. Guardar una nueva contraseña invalida todas las sesiones anteriores.

## AV2 local

Ejecutar el backend con el perfil av2 e instalar el APK Entreprenly AV2 local. Solicitar la recuperación desde la app con una cuenta de prueba. Abrir el archivo password-reset-*.txt de la bandeja local de Entreprenly AV2 y pasar el enlace de la última línea al teléfono conectado. Desde PowerShell, con la redirección USB de AV2 ya configurada:

```powershell
$adb = "$env:LOCALAPPDATA\Android\sdk\platform-tools\adb.exe"
$inbox = Join-Path $env:LOCALAPPDATA 'Entreprenly-AV2/inbox'
$message = Get-ChildItem $inbox -Filter 'password-reset-*.txt' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$link = Get-Content $message.FullName | Select-Object -Last 1
& $adb shell am start -a android.intent.action.VIEW -d $link -p online.entreprenly.entreprenlyapp.av2
```

## Render y Resend

Verificar entreprenly.online en Resend. En el servicio de backend de Render, configurar:

| Variable | Valor |
| --- | --- |
| RESEND_API_KEY | Clave de solo envío restringida a entreprenly.online. |
| RESEND_FROM_EMAIL | Dirección de envío del dominio, por ejemplo cuentas@entreprenly.online. |
| PUBLIC_API_URL | Origen HTTPS público del backend. |

Mantener el perfil cloud. El enlace de Resend abre un formulario HTTPS en el navegador. Tras guardar, el usuario vuelve a Android e inicia sesión con la nueva contraseña. Desactivar el seguimiento de clics del dominio emisor para los enlaces de recuperación. Para abrir directamente la app de producción se necesita un Android App Link verificado con el certificado de distribución.
