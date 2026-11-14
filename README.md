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
