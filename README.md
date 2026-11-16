# PharmaSoft · servicio REST de PharmaMobil

Servicio Spring Boot que consume la aplicación móvil **PharmaMobil** (Kotlin
Multiplatform). Parte del código base del docente (`dreyna/pharmaSoft`); este
repositorio es la versión que se entrega con el Proyecto Final (Equipo 1,
Adiel Lujan).

- Aplicación móvil: repositorio de PharmaMobil (ver informe de la entrega).
- Etiqueta evaluada: `v1.0-final`.

## 1. Requisitos (versiones con las que se probó)

| Herramienta | Versión probada |
| --- | --- |
| JDK | 21 (Temurin o el JBR de Android Studio) |
| Spring Boot | 4.0.7 (definido en `pom.xml`) |
| Maven | 3.9.16 vía wrapper (`mvnw`, no hace falta instalarlo) |
| Docker Desktop | 4.x con el motor encendido (*Engine running*) |
| Base de datos | Oracle Database Free 23 (`gvenzl/oracle-free:23-slim`) |
| Sistema | Windows 11 + PowerShell |

> La plantilla del curso menciona PostgreSQL, pero este servicio usa
> **Oracle Free 23** en Docker.

## 2. Crear la base de datos

Se elige una contraseña para el administrador de Oracle y otra para el
usuario de la aplicación `PHARMADB`. **No se escriben en ningún archivo del
repositorio.**

```powershell
docker run -d --name oracle-pharma -p 1522:1521 `
  -e ORACLE_PASSWORD=<contraseña-admin-oracle> `
  -e APP_USER=PHARMADB `
  -e APP_USER_PASSWORD=<contraseña-de-PHARMADB> `
  gvenzl/oracle-free:23-slim
```

En Docker Desktop → *Containers* → `oracle-pharma` → *Logs*, esperar la línea
`DATABASE IS READY TO USE!` (1–2 minutos la primera vez). El servicio se
conecta a `localhost:1522/FREEPDB1`.

Si el contenedor ya existe y aparece como *Exited*, basta con pulsar ▶ (o
`docker start oracle-pharma`) y volver a esperar esa línea.

## 3. Variables de entorno

El perfil `dev` (activo por defecto) lee los secretos del entorno:

| Variable | Obligatoria | Valor |
| --- | --- | --- |
| `DB_PASSWORD` | Sí | La contraseña de `PHARMADB` elegida en el paso 2 |
| `JWT_SECRET` | Sí | Clave Base64 de al menos 256 bits |
| `DB_URL` | No | Por defecto `jdbc:oracle:thin:@//localhost:1522/FREEPDB1` |
| `DB_USER` | No | Por defecto `PHARMADB` |

En PowerShell (solo vale para esa ventana):

```powershell
$env:DB_PASSWORD = "<contraseña-de-PHARMADB>"
$env:JWT_SECRET  = [Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

Para IntelliJ: *Run → Edit Configurations → PharmaBackendApplication →
Environment variables* y agregar `DB_PASSWORD` y `JWT_SECRET`.

Si se cambia `JWT_SECRET`, los tokens emitidos antes dejan de valer: hay que
volver a iniciar sesión en la app.

## 4. Arrancar el servicio

```powershell
.\mvnw spring-boot:run
```

o el botón ▶ de `PharmaBackendApplication` en IntelliJ. Está listo cuando la
consola muestra `Started PharmaBackendApplication in … seconds`.

Al arrancar, **Flyway** aplica las migraciones de `src/main/resources/db/migration`:

| Migración | Contenido |
| --- | --- |
| `V1__esquema_inicial.sql` | Tablas del negocio (categorías, productos, clientes, ventas…) |
| `V2__seguridad_empleados_usuarios.sql` | Empleados, usuarios, roles |
| `V3__datos_iniciales_seguridad.sql` | Empleados y usuarios de prueba |

## 5. Comprobar que responde

```powershell
curl.exe -i http://localhost:8080/api/v1/productos
```

Sin token la respuesta correcta es **401** con un JSON
`"Debe iniciar sesión para acceder a este recurso"`: significa que el
servicio está arriba y la seguridad funciona.

Inicio de sesión:

```powershell
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"admin\",\"password\":\"<contraseña de prueba>\"}'
```

Devuelve `token` y `tipo`. La documentación interactiva está en
<http://localhost:8080/swagger-ui.html>.

## 6. Datos de prueba

`V3__datos_iniciales_seguridad.sql` crea tres usuarios de desarrollo:

| Usuario | Rol |
| --- | --- |
| `admin` | ADMIN |
| `vendedor` | VENDEDOR |
| `almacen` | ALMACENERO |

Sus contraseñas de prueba **no están en el repositorio**: se guardan cifradas
con BCrypt en la base y se entregan al evaluador por un canal aparte. Son datos
semilla de desarrollo: se cambian después del primer ingreso y no se usan en
producción.

> En el perfil `dev`, `FlywayDevConfig` ejecuta `repair()` antes de migrar.
> Así una base creada antes de editar el comentario de V3 actualiza su suma de
> verificación sin volver a ejecutar ninguna migración.

## 7. Acceso desde un celular real

1. Abrir el puerto 8080 en el firewall de Windows (PowerShell **como
   administrador**, una sola vez):

   ```powershell
   New-NetFirewallRule -DisplayName 'PharmaSoft 8080' -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
   ```

2. Misma Wi‑Fi: el celular usa `http://<IPv4-de-la-PC>:8080/api/v1/`
   (`ipconfig` → «Dirección IPv4»).
3. Wi‑Fi que aísla a los equipos (redes de universidad): usar depuración
   inalámbrica y `adb reverse tcp:8080 tcp:8080`; la app apunta a
   `http://localhost:8080/api/v1/`. Detalle en la guía de instalación de
   PharmaMobil.

## 8. Perfil de producción

`application-prod.yaml` toma todo del entorno: `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `JWT_SECRET` y `SERVER_PORT`. Se activa con
`SPRING_PROFILES_ACTIVE=prod`.
