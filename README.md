# 🌿 Sistema de Reserva de Espacios Compartidos

Aplicación web (Java 21 + Spring Boot) para reservar salas, auditorios, canchas y áreas recreativas sin conflictos de horario.
Proyecto de Ingeniería del Software — Universidad Industrial de Santander (UIS), Grupo 4.

## Cómo ejecutarlo

### Opción A — IntelliJ IDEA Community (recomendada, no instala nada más)
1. Instala **IntelliJ IDEA Community** (jetbrains.com/idea/download). Trae Maven incluido.
2. `File > Open` y elige la carpeta del proyecto (la que contiene `pom.xml`). Espera a que descargue dependencias (barra inferior).
3. Si pide un JDK: `File > Project Structure > SDK > Download JDK` y elige **21**.
4. Abre `src/main/java/co/edu/uis/reservas/ReservasApplication.java` y pulsa el botón verde ▶ junto a `main`.
5. Entra a http://localhost:8080

### Opción B — Terminal (si tienes JDK 21 y Maven)
```
mvn spring-boot:run
```

## Notificaciones por mensaje de texto (SMS)
Los avisos (reserva confirmada, editada o cancelada, recordatorio 24 h antes y recordatorio a los 30 min del inicio si el
espacio no se marcó como ocupado) se envían por SMS al teléfono registrado; si no hay teléfono válido, por correo.

- **Por defecto es simulado**: el mensaje solo aparece en la consola (`[SMS simulado] a +57...`). No necesitas configurar nada para probar.
- **Para enviar SMS reales** (Twilio), define estas variables de entorno antes de ejecutar:
  `SMS_MODO=real`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_NUMERO` (número de Twilio con `+`).
  Los teléfonos escritos sin código de país se envían con `+57` (cámbialo con `SMS_CODIGO_PAIS`).
- Las reglas de tiempo (30 min del recordatorio, 45 min para marcar inasistencia) están en `config/ReglasNegocio.java`.

### Elegir cómo recibir los avisos
En **Mi perfil → Notificaciones** cada persona elige: mensaje de texto (SMS), correo electrónico, o no recibir notificaciones.
Si elige SMS pero no tiene un teléfono válido, no se le deja guardar esa opción.

## Recuperar la contraseña
En el login, "¿Olvidaste tu contraseña?" pide el correo y envía un enlace de un solo uso que vence en 1 hora.
Antes de enviar, se verifica que el correo esté asociado a una cuenta activa; si no, la pantalla lo avisa y no se envía nada.

- **Por defecto es simulado**: el enlace aparece en la consola (`[CORREO simulado] ... Enlace: http://localhost:8080/restablecer?token=...`).
- **Para enviar correos reales** define: `NOTIFICACIONES_MODO=real`, `MAIL_USUARIO`, `MAIL_CLAVE` (en Gmail, una "contraseña de aplicación"),
  y `APP_URL` con la dirección pública de la app (para que el enlace del correo funcione fuera de tu computador).
- Este correo se envía siempre, aunque la persona haya desactivado las notificaciones de reservas.

## Horas de reserva
Solo se pueden elegir horas en punto o y media (13:00, 13:30, 14:00…). Se valida también en el servidor.

## Usuarios de prueba
- Administrador: `admin@reservas.co` / `Admin1234`
- Usuario regular: créalo en la pantalla "Regístrate".

## Estructura
Ver `docs/CONTEXTO.md`. Pruebas: `mvn test`.

## Flujo de trabajo con git
```
git init
git add .
git commit -m "feat: estructura base del proyecto"
git branch -M main
git remote add origin <URL-del-repo>
git push -u origin main
git checkout -b develop
git checkout -b feature/nombre-de-la-funcionalidad
```
Commits: `feat:` nueva función · `fix:` arreglo · `docs:` documentación · `test:` pruebas.
