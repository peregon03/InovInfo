# Prompt: InovInfo, app Android nativa (Kotlin) para revisión de DVR móviles

## 0. Contexto y forma de trabajo

Eres un desarrollador Android senior. Vas a continuar el proyecto **InovInfo**, una app Android nativa en **Kotlin + Jetpack Compose** de la empresa Electroinova SAS (Cali, Colombia). Electroinova hace el mantenimiento preventivo y correctivo de los **DVR móviles** (grabadores de video con cámaras) instalados en los vehículos de recolección de basura de **Veolia**.

Hoy los técnicos reportan por WhatsApp. Luego alguien copia todo a un cuaderno y después lo pasa a un Excel en Google Sheets que ven los gerentes de Veolia. La app reemplaza ese flujo completo: el técnico registra cada unidad en campo, aun sin internet, y el sistema genera el informe en el formato exacto que Veolia ya recibe.

**El proyecto ya existe.** Antes de escribir código:
1. Revisa la estructura actual del proyecto, `build.gradle(.kts)`, el paquete y las pantallas que ya existen.
2. Reutiliza y refactoriza lo que sirva. No recrees el proyecto desde cero ni cambies el paquete.
3. Si algo de este documento choca con el código existente, dime qué encontraste y qué propones antes de cambiarlo.

**Trabaja por fases (sección 12).** Al terminar cada fase, el proyecto debe compilar y correr. Detente, resume lo que hiciste y lista los archivos creados o modificados. Espera mi confirmación antes de seguir con la siguiente fase.

**La prioridad es la app Android.** El backend ya existe (Node.js/Express + PostgreSQL). En este prompt solo se describe el contrato de API que la app espera (sección 9). No escribas código del backend ni de ninguna web, salvo que te lo pida.

---

## 1. Datos técnicos del proyecto

- Paquete: `com.electroinova.inovinfo`
- minSdk 26, targetSdk el más reciente estable, Kotlin y AGP estables
- UI: Jetpack Compose + Material 3, Navigation Compose
- Arquitectura: MVVM + capas `data / domain / ui`, con repositorios como única fuente de verdad (offline-first)
- Inyección de dependencias: Hilt
- Base local: Room (con migraciones, sin `fallbackToDestructiveMigration` en release)
- Red: Retrofit + OkHttp + kotlinx.serialization
- Trabajo en segundo plano: WorkManager para la sincronización
- Preferencias y sesión: DataStore + EncryptedSharedPreferences (o Tink) para el token
- Imágenes: Coil para mostrar, `ActivityResultContracts.TakePicture` y `PickMultipleVisualMedia` para tomar y cargar, ExifInterface para leer la fecha de la foto
- Idioma de toda la interfaz: español (Colombia)

**Colores de marca** (definirlos en el tema de Material 3, con modo claro y oscuro):
- Primario `#1A3A5C` (azul marino)
- Secundario `#C9A84C` (dorado)
- Fondo `#FFFFFF`, superficie `#F5F7FA`, error `#E24B4A`
- Colores de estado: OK verde `#1F7A4D`, Con novedad ámbar `#8A5A00`, Inoperativa/Pendiente rojo `#B8312F`, Sin llaves/No está gris `#3B4656`

Botones y zonas táctiles de mínimo 48 dp: los técnicos usan la app en la calle, a veces con guantes y con sol.

---

## 2. Roles y autenticación

Ya existe autenticación en el backend (`/api/auth`) con dos roles: **COORDINADOR** y **TECNICO**.

- Al abrir la app, si no hay sesión, se muestra **Login** (usuario/correo y contraseña).
- El token y los datos del usuario (id, nombre, rol) se guardan cifrados.
- **La sesión debe seguir funcionando sin internet**: si ya inició sesión antes, la app abre directo, aunque no haya señal. El token se renueva cuando vuelve la conexión. Si el backend responde 401 con internet, se pide login otra vez **sin borrar los datos pendientes de sincronizar**.
- **El técnico ya no escribe su nombre en ningún reporte.** Cada revisión, suministro y foto se guarda con el `usuarioId` de la sesión, y el nombre sale de ahí en el informe y en el historial.
- La navegación y los botones visibles dependen del rol (sección 7).

| Función | Técnico | Coordinador |
|---|---|---|
| Ver jornadas activas y registrar revisiones | Sí | Sí |
| Digitar el número de una unidad para revisarla | Sí | Sí |
| Registrar visita extraordinaria | Sí | Sí |
| Consultar historial de suministros y de unidades | Sí | Sí |
| Crear unidad que no existe en el catálogo | Queda **provisional** | Queda **confirmada** |
| Crear, editar y desactivar bases operativas | No | Sí |
| Crear, editar, mover y desactivar unidades | No | Sí |
| Aprobar unidades provisionales | No | Sí |
| Gestionar unidades de negocio y catálogo de suministros | No | Sí |
| Crear y cerrar jornadas, generar informe | No | Sí |

---

## 3. Conceptos del negocio

- **Unidad de negocio**: la división de Veolia a la que pertenece el vehículo. Valores actuales: Sur Occidente, Buga, Tuluá, Cali. Aparece en la columna "Unidad de negocio" del informe.
- **Base operativa**: el sitio físico donde están los vehículos y adonde viajan los técnicos. Valores actuales: Palmira, Buga, Tuluá, Cali, Río Paila. Una base puede tener vehículos de varias unidades de negocio. Por ejemplo, en la base Palmira hay unidades de Sur Occidente.
- **Unidad**: el vehículo. Su número es **texto**, no número, porque hay unidades como `BR03`, `BR05` o `Cargador`. Los números no siguen un prefijo por sede.
- **Jornada**: un viaje de revisión a una base operativa en una fecha. Reemplaza las listas y encuestas de WhatsApp.
- **Revisión**: el registro de una unidad dentro de una jornada.
- **Visita extraordinaria**: una revisión pedida por el área de mantenimiento de Veolia fuera del viaje programado. Se registra el día que ocurre y sale en el informe de la siguiente jornada de esa unidad, con la frase "A solicitud del area de mantenimiento…".
- **Suministro**: un elemento instalado, cambiado o reparado: visor, cámara, tarjeta de video, batería, cable, accesorios, main board, etc.

---

## 4. Modelo de datos (Room)

Usa UUID generados en el teléfono como ID de los registros que se crean en campo, para poder crearlos sin internet. Todas las tablas que se sincronizan llevan `createdAt`, `updatedAt` (epoch ms), `syncStatus` (`SYNCED`, `PENDING`, `ERROR`) y `deleted` (borrado lógico).

```
Usuario           id, nombre, rol(COORDINADOR|TECNICO), email, activo

UnidadNegocio     id, nombre, activa

BaseOperativa     id, nombre, ciudad, activa
BaseUnidadNegocio baseId, unidadNegocioId          // unidades de negocio que atiende la base

Unidad            id, numero (String, único sin distinguir mayúsculas), placa?, 
                  tipo (RECOLECTOR|VOLQUETA|BARREDORA|MINICARGADOR|OTRO),
                  unidadNegocioId, baseOperativaId,
                  numCamaras, tieneTelo, tieneVisionDirecta,
                  conexion (DIRECTA|POR_ENCENDIDO|DESCONOCIDA),
                  estadoCatalogo (CONFIRMADA|PROVISIONAL), creadaPor, notas?, activa

Jornada           id, fecha (LocalDate), baseOperativaId, 
                  estado (ABIERTA|CERRADA), creadaPor, nombrePestana?, informeGeneradoAt?
JornadaUnidad     jornadaId, unidadId, orden        // unidades esperadas en la jornada

Revision          id, jornadaId?, unidadId, tecnicoId,
                  tipo (NORMAL|EXTRAORDINARIA), fechaVisita (LocalDate),
                  estado (OK|CON_NOVEDAD|INOPERATIVA|SIN_LLAVES|NO_ESTA_EN_BASE|PENDIENTE)?,
                  motivo?, hallazgo?, textoExtraordinaria?,
                  atrasoHoraMin?, diasGrabacion?, h264Activo,
                  revisadaEnOtraBase (Boolean), 
                  tomadaPor?, tomadaHasta?        // "unidad en revisión"
RevisionCheck     revisionId, item, valor (BIEN|FALLA|NO_TIENE)

Suministro        id, revisionId, unidadId, descripcion (texto libre), motivo,
                  catalogoId?, fecha, tecnicoId
CatalogoSuministro id, nombre, activo                // solo sugerencias, no restringe

Foto              id, revisionId, suministroId?, 
                  tipo (ANTES|DESPUES|EVIDENCIA|SUMINISTRO),
                  rutaLocal, urlRemota?, tomadaEn, subida (Boolean)

ColaSync          id, entidad, entidadId, operacion, intentos, ultimoError?
```

Ítems de `RevisionCheck`, en este orden: Cámara frontal, Cámara lateral, Cámara trasera, Cámara cabina, Monitor visión directa, Visor principal, Telo (pantalla táctil), GPS, WiFi / credenciales.

**Catálogo inicial de suministros** (sugerencias, el texto sigue siendo libre):
- Sum Monitor/visor
- Sum Monitor/visor de visión directa
- Sum Tarjeta Video Especial V/CA 1080 Sistema Dvr Móvil
- Reparación Main Board Dvr Móvil
- Sum - Cambio Cable Encauchetado Especial Con Protección Fase De Video X3 Mts
- Sum Batería Respaldo Interno Especial Sistema Grabación Móvil
- Sum Accesorios en General
- Sum Cámara Frontal
- Sum Visor 7''
- Modulo Estabilizador/Regulador De Tensión Dc/Dc Telo-XL05

---

## 5. Funciones que ya se validaron en campo (deben quedar igual o mejor)

Estas funciones se probaron el 18 de octubre de 2026 en la base Palmira con una versión de prueba. Tienen que existir en la app.

### 5.1 Jornada y lista de unidades
- Encabezado con la base, la fecha y el avance: "N de M revisadas" con barra de progreso.
- Indicador de sincronización siempre visible: "Sincronizado", "Sin conexión · N por enviar" o "Enviando…".
- Contadores que también funcionan como filtros: Todas, Faltan, OK, Con novedad, Inoperativas, Sin llaves, Otras (No está en base + Pendiente), Alertas (revisiones con datos incompletos).
- Buscador por número o placa.
- Cada tarjeta de unidad muestra: número grande, unidad de negocio, subtítulo (hallazgo, motivo, "N suministros" o "La está revisando [nombre]"), chip de estado de color y la primera alerta en rojo si le falta algo.
- Las unidades sin revisar se resaltan con borde dorado.

### 5.2 Digitar el número de la unidad (técnico y coordinador)
Es la forma principal de empezar una revisión, además de tocar la unidad en la lista.
- Campo grande "Digitar unidad", siempre visible arriba de la lista, que acepta letras y números. Tiene autocompletado sobre **todo el catálogo**, no solo la base de la jornada, y muestra la base y la unidad de negocio de cada sugerencia.
- Al confirmar:
  - **Si la unidad está en la jornada**, abre su revisión.
  - **Si existe en el catálogo pero es de otra base**, la agrega a la jornada y marca `revisadaEnOtraBase = true`. El informe añade "(Unidad revisada en base operativa {base de la jornada})".
  - **Si no existe**:
    - Si es técnico, aparece un formulario corto: número, unidad de negocio y tipo. La unidad se crea como **PROVISIONAL** y se agrega a la jornada. El coordinador la ve en "Unidades por aprobar".
    - Si es coordinador, ve el mismo formulario y la unidad queda **CONFIRMADA**.
- Normaliza el número: sin espacios al inicio y al final, en mayúsculas. **Nunca se guarda texto extra en el número**. En la prueba alguien escribió "8014 SUR OCCIDENTE"; la unidad de negocio va en su propio campo.

### 5.3 "Tomar" una unidad
- Al abrir una unidad sin estado, se marca `tomadaPor = usuario` por 3 horas y se envía al servidor si hay señal.
- Los demás ven "La está revisando [nombre]" y, si la abren, un aviso para confirmar antes de revisarla.
- Al salir sin marcar estado, se libera.
- Si ya la revisó otra persona, se muestra "Esta unidad ya la revisó [nombre]".

### 5.4 Revisión guiada
Una sola pantalla con secciones desplegables, en este orden:
1. **Estado de la unidad**: seis botones (OK, Con novedad, Inoperativa, Sin llaves, No está en la base, Pendiente). Tocar el seleccionado lo quita.
   - Inoperativa: motivo opcional ("en taller, varada, sin batería…").
   - Pendiente: motivo **obligatorio**. Además se notifica al coordinador.
   - No está en la base: motivo **obligatorio** ("prestada a otra base, en taller…").
   - Unidad de negocio editable.
2. **Foto de evidencia**: solo si el estado es Inoperativa, Sin llaves o No está en la base.
3. **Foto del antes**: monitor encendido antes de intervenir. Solo si el estado está vacío, OK, Con novedad o Pendiente, igual que las secciones 4 a 8.
4. **Grabación**: atraso de la hora en minutos, días de grabación y "H264+ activado". Si el atraso es **mayor a 15 minutos**, el campo se pone rojo y aparece el aviso "Se sugiere cambiar la pila de respaldo".
5. **Cámaras y equipos**: los ítems de `RevisionCheck`, cada uno con tres botones: Bien, Falla y No tiene. Los ítems que la unidad no tiene según el catálogo (sin Telo, sin visión directa) arrancan en "No tiene".
6. **Hallazgo**: texto libre. Va después de "durante la inspección…". Es obligatorio si el estado es Con novedad.
7. **Suministros y cambios**: lista de tarjetas. Cada una tiene:
   - Descripción en **texto libre**, con sugerencias del catálogo mientras escribe.
   - Motivo, **obligatorio**.
   - Al menos una foto del elemento cambiado, **obligatoria**.
   - Botón "Quitar".
   - Si a esa unidad se le suministró un elemento parecido en los últimos 90 días, se muestra un aviso: "A esta unidad se le cambió {elemento} el {fecha}".
8. **Foto del después**.
9. **Más opciones**: el interruptor "Revisada en otra base", con el texto que agrega al informe.
10. **Así queda en el informe**: vista previa en vivo de las filas que generará la unidad (sección 6.2).
11. **Alertas**: lista de lo que falta, con el mismo texto que se usa en el filtro "Alertas".

Botón inferior: "Listo, volver a la lista". Los cambios se guardan solos al escribir, sin botón de guardar.

### 5.5 Reglas de validación (generan alertas, no bloquean el guardado)
- Con novedad sin hallazgo: "Falta escribir el hallazgo".
- Pendiente sin motivo: "Falta justificar el pendiente".
- No está en la base sin motivo: "Falta decir dónde está la unidad".
- Suministro sin descripción, sin motivo o sin foto: "Suministro N: falta …".
- Visita extraordinaria sin fecha.
- Atraso mayor a 15 min sin un suministro que contenga "batería" o "pila": "Hora atrasada más de 15 min: revisar cambio de pila".

El coordinador no puede **generar el informe** mientras haya revisiones con alertas, salvo que confirme que quiere seguir.

### 5.6 Fotos
- Dos botones en cada sección: **Tomar** (cámara) y **Cargar** (galería, selección múltiple). En campo, la cámara a veces no se abre, así que cargar desde la galería es indispensable.
- Cada foto se **comprime** (lado mayor 1600 px, JPEG calidad 75) y se le **marca** abajo a la izquierda, sobre una franja negra semitransparente: `{unidad} · dd/MM/yyyy HH:mm`. La hora es la de la toma: si viene de la galería, se lee del EXIF (`DateTimeOriginal`) y, si no hay, de la fecha del archivo.
- Las fotos se guardan en el almacenamiento interno de la app y se suben en segundo plano.
- **Recibir desde otras apps**: registrar `ACTION_SEND` y `ACTION_SEND_MULTIPLE` para `image/*` y `video/*`, para que los técnicos compartan capturas desde la app del DVR. Al recibirlas, se pide elegir la unidad y el tipo de foto.
- Miniaturas con botón para borrar. Si una foto está en el servidor pero no en este teléfono, se muestra desde `urlRemota`.

### 5.7 Visita extraordinaria
- Disponible desde el inicio, sin necesidad de una jornada abierta: "Registrar visita extraordinaria".
- Es una `Revision` de tipo `EXTRAORDINARIA` con fecha propia, unidad digitada (5.2) y el texto de lo que se encontró y se hizo. Tiene los mismos suministros, fotos y checks.
- Cuando se genera el informe de la **siguiente jornada** que incluya esa unidad, la visita extraordinaria sale primero en su bloque (sección 6.2).

### 5.8 Funcionamiento sin internet (crítico)
- En el vehículo, el teléfono se conecta al **wifi del DVR, que no tiene internet**.
- Todo se guarda primero en Room. La interfaz nunca espera al servidor.
- Un `SyncWorker` de WorkManager sube los cambios pendientes en orden: unidades provisionales, revisiones, suministros, fotos. Reintenta con backoff exponencial y corre cuando hay red, cada 15 minutos y al abrir la app.
- **Forzar datos móviles**: usa `ConnectivityManager.requestNetwork` con `TRANSPORT_CELLULAR` y `NET_CAPABILITY_INTERNET`, y pasa el `network.socketFactory` al OkHttpClient de sincronización. Así se sincroniza aunque el wifi del DVR siga conectado. Si no hay datos móviles, usa cualquier red con `NET_CAPABILITY_VALIDATED`.
- Antes de dar algo por sincronizado, confirma la respuesta del servidor. **Nunca se borra un dato local sin confirmación del servidor.**
- Conflictos: el servidor guarda la versión con `updatedAt` más reciente. Si la versión local pierde, se avisa al usuario: "La unidad {n} fue actualizada por {nombre}".
- Pantalla "Pendientes de enviar" con la lista de lo que está en cola, los errores y el botón "Reintentar ahora".

---

## 6. Estructura del informe en Google Sheets

El informe lo **genera el backend** en Google Sheets, usando una cuenta de servicio de Google. **Las credenciales de Google nunca van dentro del APK.** La app solo pide generarlo, muestra el resultado y el link (sección 9). Igual, describe esta estructura en la capa `domain`, en un `InformeBuilder` puro en Kotlin con pruebas unitarias, porque la app usa las mismas reglas para la vista previa (5.4.10). El backend debe producir exactamente las mismas filas.

### 6.1 Archivo y pestaña
- Cada jornada genera **una pestaña nueva** dentro del archivo que corresponda, por ejemplo "Informe Técnico DVRMovil Buga-Tuluá-Sur Occidente 2026". El coordinador configura el ID del archivo y de la carpeta de fotos (7.2).
- Nombre de la pestaña: `{Base} {d}{Mmm}{yyyy}`, por ejemplo `Palmira 18Oct2026`. El coordinador lo puede editar al crear la jornada.
- Para pruebas existe el archivo **"Prueba informe"** (ID `1bD5Nall4_9ppzjc76CJs-XxmFbZ2WXdJitN7sHPgTgo`).

### 6.2 Hoja 1: informe para Veolia (formato exacto)

| Fila | Contenido |
|---|---|
| 2 | A2:E2 combinadas: `VISITA TÉCNICA ELECTROINOVA A SEDE OPERATIVA`. Arial 14, centrado, relleno verde `#B7D7C9` |
| 3 | Encabezados: `Fecha` \| `Unidad de negocio` \| `Ref. Vehículo` \| `Actividad` \| `Observación`. Arial 10 negrita, centrado, relleno verde, bordes |
| 4 en adelante | Un bloque por unidad revisada, en el orden de la jornada |

Por cada unidad:
- Columnas A a D (fecha `dd/MM/yyyy`, unidad de negocio, número de unidad, actividad) se **combinan verticalmente** sobre todas las filas del bloque. Van en negrita, salvo la actividad.
- Actividad, siempre: `· Mtto preventivo y correctivo Dvr Movil`.
- Columna E: una fila por línea de observación. Texto ajustado y bordes en todas las celdas.
- Si la unidad de negocio de la unidad no es una de las que atiende la base de la jornada, el bloque se resalta en naranja claro, como se hace hoy con las unidades de Cali.

Texto de la fila principal según el estado. `STD` es `Se ejecutó el servicio de revisión, diagnóstico técnico, ajuste, reparación, mantenimiento general y limpieza del sistema de cámaras`:

| Estado | Fila principal |
|---|---|
| OK | `STD` |
| Con novedad | `STD + ", durante la inspección " + hallazgo` (hallazgo con la primera letra en minúscula y sin punto final) |
| Inoperativa | `Al momento de la visita esta unidad se encuentra inoperativa` + ` ({motivo})` si hay motivo |
| Sin llaves | `Al momento de la inspección no se encontraron llaves` |
| No está en la base | `Al momento de la visita la unidad no se encontraba en la base operativa ({motivo})` |
| Pendiente | `Unidad pendiente por intervención ({motivo})` |

Si `revisadaEnOtraBase` es verdadero y el estado es OK o Con novedad, se agrega al final ` (Unidad revisada en base operativa {base de la jornada})`.

**Orden de las filas del bloque:**
- Sin visita extraordinaria: fila principal y luego una fila por suministro, con su descripción tal como la escribió el técnico.
- Con visita extraordinaria pendiente de informar: primero la fila `A solicitud del area de mantenimiento se realiza visita el {d} de {Mes}` (la fecha en **negrita**, mes con mayúscula inicial, por ejemplo "9 de Septiembre") + ` {textoExtraordinaria}`; luego los suministros de esa visita, luego los de la jornada y al final la fila principal.

Las unidades **sin revisar** no salen en la hoja 1.

**Pie de página**, cinco filas vacías después del último bloque:
```
[imagen de la firma]
Jhon Fernando Agudelo A.            (negrita)
Ingeniero Electrónico               (negrita)
Electroinova SAS                    (negrita)
electroinova.soluciones@gmail.com   (azul, subrayado)

*Todas las reparaciones e instalaciones ejecutadas contaron con el acompañamiento y supervisión de personal designado por la entidad contratante, dejando constancia de las labores realizadas. Como soporte de lo anterior, se anexa el respectivo registro fotográfico en el siguiente enlace. Link: {link de la carpeta de fotos de la jornada}

*Es importante aclarar que Electroinova no será responsable por la no ejecución de intervenciones programadas en aquellas unidades que, al momento de la visita técnica, se encontraran inoperativas, fuera de servicio, ausentes o no disponibles para su atención, sin afectar el alcance pactado.

* Concientizar al personal de la compañía de la importancia del buen uso, manejo y limpieza de los elementos y equipos del sistema de grabacion Movil
[logo Electroinova]
```
Las tres notas van combinadas de A a E, en negrita. Las dos primeras, además, subrayadas.

Anchos de columna aproximados (px): A 100, B 120, C 100, D 240, E 420. Fuente Arial en todo el informe y sin líneas de cuadrícula.

### 6.3 Hoja 2: "Detalle técnico" (uso interno)
Una fila por unidad de la jornada, **incluidas las no revisadas** ("Sin revisar"):

`Unidad | Unidad de negocio | Base operativa | Estado | Técnico | Fecha y hora de revisión | Atraso hora (min) | Días grabación | H264+ | Cámaras y equipos con falla | Hallazgo / motivo | Suministros y motivo | Nº fotos | Visita extraordinaria`

En "Suministros y motivo" va una línea por suministro: `{descripción} — {motivo}`.

### 6.4 Fotos en Google Drive
El backend crea una carpeta por jornada: `Registro fotográfico {Base} {dd-MM-yyyy}/{unidad}/`. Los archivos se nombran `antes_1.jpg`, `despues_1.jpg`, `evidencia_1.jpg` y `suministro{n}_1.jpg`. El link de esa carpeta va en el pie del informe.

---

## 7. Pantallas y navegación

### 7.1 Técnico (barra inferior de 3 pestañas)
1. **Jornada**: la jornada abierta de hoy (5.1 y 5.2). Si hay varias abiertas, un selector. Si no hay ninguna, un estado vacío con el botón "Registrar visita extraordinaria".
2. **Historial**: consulta de suministros y de unidades (sección 8).
3. **Más**: pendientes de enviar, visita extraordinaria, perfil y cerrar sesión.

### 7.2 Coordinador (lo mismo del técnico, más una pestaña "Administrar")
**Administrar** contiene:
- **Bases operativas**: lista con buscador. Crear, editar (nombre, ciudad y unidades de negocio que atiende) y desactivar. No se borra si tiene unidades o historial. El detalle de cada base muestra sus unidades.
- **Unidades**: lista filtrable por base operativa, unidad de negocio y tipo, con buscador.
  - Crear y editar con todos los campos de `Unidad`.
  - **Mover a otra base operativa**, de una o varias a la vez con selección múltiple.
  - Desactivar en lugar de borrar.
  - Validar que el número no se repita.
- **Unidades por aprobar**: las PROVISIONALES creadas por técnicos. Se pueden confirmar (y completar los datos), unir con una unidad existente si era un error de digitación (pasando sus revisiones a la correcta) o descartar.
- **Unidades de negocio**: crear, editar y desactivar.
- **Catálogo de suministros**: crear, editar y desactivar las sugerencias.
- **Jornadas**:
  - Crear: fecha, base operativa y nombre de la pestaña. Las unidades activas de esa base se precargan, y se pueden quitar o agregar otras.
  - Ver el avance en vivo de cada jornada.
  - Cerrar.
  - **Generar informe**: muestra la lista de chequeo (unidades sin revisar, alertas, número de suministros y fotos, fotos sin subir), luego llama al backend y muestra el link a la pestaña creada y a la carpeta de fotos. También permite descargar el .xlsx que devuelve el backend y guardarlo o compartirlo.
- **Destino del informe**: el ID del archivo de Sheets y de la carpeta de Drive, por unidad de negocio y año. Se guarda en el backend.

### 7.3 Navegación
`Login → (según rol) MainScaffold { Jornada | Historial | [Administrar] | Más }`
Rutas internas: `revision/{revisionId}`, `unidad/{unidadId}/historial`, `jornada/{id}/informe`, `admin/bases`, `admin/bases/{id}`, `admin/unidades`, `admin/unidades/{id}`, `admin/aprobar`, `admin/catalogo`, `admin/jornadas/nueva`, `pendientes`, `extraordinaria/nueva`.

---

## 8. Historial de suministros (nuevo)

Pantalla para consultar **qué elementos se han suministrado a través del tiempo**, usando la base de datos relacional del backend.

- **Filtros**: unidad (digitada, con autocompletado), base operativa, unidad de negocio, rango de fechas (atajos: este mes, últimos 3 meses, este año, personalizado), elemento (texto: "visor" encuentra todos los visores) y técnico.
- **Resumen arriba**: total de suministros en el periodo y el conteo por elemento, agrupado por la sugerencia del catálogo más parecida y, si no hay, por el texto. Por ejemplo: "Monitor/visor: 12 · Tarjeta de video: 5 · Batería de respaldo: 9".
- **Lista**: fecha, unidad, base, elemento, motivo, técnico y miniatura de la foto. Tocar abre el detalle con todas las fotos y la revisión completa.
- **Historial de una unidad** (`unidad/{id}/historial`), al tocar el número: línea de tiempo con todas sus revisiones, estados, hallazgos y suministros, más los elementos que se repiten. Sirve para detectar fallas recurrentes, como una unidad a la que le cambian fusibles cada mes.
- Sin internet, la pantalla muestra los datos guardados de la última consulta con la nota "Datos del {fecha y hora}". Las revisiones propias aún no sincronizadas también aparecen, marcadas como "Sin enviar".
- Paginación: 50 resultados por página, con carga al hacer scroll.
- Lo usan los dos roles.

---

## 9. Contrato de API que la app espera

Verifica qué rutas ya existen en el backend (`/api/auth`, `/api/visitas`, `/api/catalogos`, `/api/usuarios`). Ajusta los nombres a lo existente y **lista al final de cada fase las rutas que faltan en el backend**, para crearlas aparte. La URL base se configura en `BuildConfig` por variante (debug/release). En producción es `https://api.inovinfo.site`.

```
POST   /api/auth/login                     → { token, refreshToken, usuario{id,nombre,rol} }
POST   /api/auth/refresh

GET    /api/catalogos?since={ts}           → unidadesNegocio, bases, baseUnidadNegocio,
                                             unidades, catalogoSuministros (solo cambios desde ts)
POST/PUT/DELETE /api/bases[/{id}]          (coordinador)
POST/PUT/DELETE /api/unidades[/{id}]       (coordinador; técnico solo POST provisional)
POST   /api/unidades/mover                 { unidadIds[], baseOperativaId }  (coordinador)
POST   /api/unidades/{id}/aprobar | /unir  (coordinador)
CRUD   /api/unidades-negocio, /api/catalogo-suministros (coordinador)

GET    /api/jornadas?estado=ABIERTA
POST   /api/jornadas                       (coordinador)
GET    /api/jornadas/{id}/revisiones?since={ts}
POST   /api/jornadas/{id}/cerrar           (coordinador)
POST   /api/jornadas/{id}/informe          → { sheetUrl, carpetaFotosUrl, pestana }
GET    /api/jornadas/{id}/informe.xlsx

PUT    /api/revisiones/{id}                upsert idempotente por UUID, con checks y suministros
POST   /api/revisiones/{id}/tomar | /soltar
POST   /api/fotos  (multipart: archivo + metadatos, idempotente por UUID) → { urlRemota }

GET    /api/suministros?unidad=&base=&unidadNegocio=&desde=&hasta=&q=&tecnico=&page=
GET    /api/suministros/resumen?...mismos filtros
GET    /api/unidades/{id}/historial
```

Todas las llamadas llevan `Authorization: Bearer {token}`. El backend valida el rol en cada ruta, no solo la app.

---

## 10. Estructura de paquetes sugerida

```
com.electroinova.inovinfo
├── InovInfoApp.kt                      (Hilt, WorkManager config)
├── MainActivity.kt                     (recibe ACTION_SEND)
├── core/
│   ├── network/   (OkHttp, interceptor de token, CellularNetworkProvider)
│   ├── database/  (AppDatabase, DAOs, converters, migraciones)
│   ├── session/   (SessionManager, rol actual)
│   ├── sync/      (SyncWorker, ColaSync, ConflictResolver)
│   ├── photos/    (PhotoProcessor: compresión, marca de agua, EXIF)
│   └── ui/theme/
├── data/
│   ├── remote/    (ApiService, DTOs)
│   ├── local/     (entidades Room)
│   └── repository/ (AuthRepo, CatalogoRepo, JornadaRepo, RevisionRepo, SuministroRepo, FotoRepo)
├── domain/
│   ├── model/
│   ├── informe/   (InformeBuilder + reglas de texto, con tests)
│   └── validacion/ (ValidadorRevision, con tests)
└── ui/
    ├── login/
    ├── jornada/   (lista, digitar unidad, contadores)
    ├── revision/  (revisión guiada, suministros, fotos)
    ├── extraordinaria/
    ├── historial/ (suministros, historial de unidad)
    ├── pendientes/
    └── admin/     (bases, unidades, aprobar, unidadesNegocio, catalogo, jornadas, informe, destino)
```

---

## 11. Lo que se elimina respecto a la versión de prueba
- Campo de nombre del técnico: ahora sale de la sesión.
- Copiar o pegar avance por WhatsApp y respaldo .json: los reemplaza la sincronización con el servidor.
- Generar el Excel en el teléfono: lo hace el backend.

---

## 12. Fases de implementación

1. **Base y sesión**: revisar el proyecto actual, dependencias, tema de marca, Hilt, Room con todas las entidades, login con rol, sesión sin internet y navegación según el rol con pantallas vacías.
2. **Catálogos y administración**: sincronización de catálogos, CRUD de bases operativas, unidades (con mover y desactivar), unidades de negocio, catálogo de suministros y unidades por aprobar.
3. **Jornada y revisión**: crear jornada (coordinador), lista con contadores y filtros, digitar unidad (5.2), tomar unidad, revisión guiada completa, validaciones y `InformeBuilder` con vista previa y pruebas unitarias.
4. **Fotos**: tomar, cargar múltiples, compresión, marca de agua con EXIF, recibir desde otras apps.
5. **Sincronización**: `SyncWorker`, forzar datos móviles, conflictos y pantalla de pendientes.
6. **Visita extraordinaria, historial de suministros e historial de unidad.**
7. **Generar informe**: lista de chequeo, llamada al backend, links y descarga del .xlsx.

**Criterios de aceptación generales:**
- Con el modo avión activado se puede hacer una jornada completa. Al desactivarlo, todo se sube sin intervención del usuario.
- Con el wifi del DVR conectado y datos móviles encendidos, la sincronización funciona.
- Dos técnicos con cuentas distintas ven en sus teléfonos las unidades que revisó el otro, con su nombre, en menos de un minuto cuando ambos tienen señal.
- El `InformeBuilder` produce, para los datos de la prueba del 18/10/2026 en Palmira, las mismas filas que el informe real (ejemplo: unidad 7063 con novedad, una fila principal y una fila "Sum Accesorios en General").
- Ninguna credencial de Google ni contraseña queda en el código o en el APK.
