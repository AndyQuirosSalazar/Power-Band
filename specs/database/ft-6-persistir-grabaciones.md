---
id: ft-6
layer: database
status: draft
---

# Persistir grabaciones

## General description

Agregar una grabación completada a `recordings.music`, indexar su nombre en `recordings.index` y agregar nombre/fecha a `recordings.list`. La búsqueda de duplicados debe ser binaria por seek en el índice ordenado. Depende de los formatos de ft-4 y del ordenamiento externo de ft-5.

## Inputs

- Nombre validado: de 1 a 31 caracteres ASCII imprimibles, sin espacios externos.
- Secuencia de `CommandRecord` de 48 bytes con tiempos relativos.
- Fecha/hora de creación.
- Estado de los tres archivos y, si el conjunto es parcial, decisión de inicialización seleccionada por la interfaz de ft-7.

## Outputs

- `recordings.music`: encabezado de 48 bytes seguido por comandos de 48 bytes; las grabaciones se almacenan consecutivamente.
- `recordings.index`: registros de 40 bytes ordenados por nombre, con la posición del encabezado en `recordings.music`.
- `recordings.list`: nombre y fecha/hora ISO-8601 UTC, en orden de creación.
- Resultado de búsqueda de existencia por nombre o error explícito de almacenamiento.
- Estado que permite a la interfaz consultar y resolver la inicialización de archivos parciales.

## Test criteria

- El encabezado de música mide 48 bytes: nombre en 32 bytes ASCII, offset inicial de comandos (`long`) y offset final exclusivo (`long`), ambos en bytes.
- Cada comando mide 48 bytes según ft-4; los offsets delimitan exactamente el rango de comandos y las grabaciones no dejan huecos.
- Cada entrada del índice mide 40 bytes: nombre de 32 bytes y offset del encabezado (`long`).
- El índice se ordena mediante el algoritmo de ft-5.
- La búsqueda binaria lee por seek desde el índice en disco y usa el mismo comparador lexicográfico ASCII sensible a mayúsculas que ft-5.
- Un nombre duplicado se rechaza antes de modificar cualquiera de los tres archivos; `Piano` y `piano` son distintos.
- Una grabación nueva agrega encabezado y comandos, actualiza el índice y agrega una línea al final de la lista cronológica.
- Si ninguno de los tres archivos existe, se crean vacíos automáticamente.
- Si solo existe parte del conjunto, la capa informa que requiere una decisión y aplica la opción elegida por la interfaz: borrar únicamente los tres archivos y reiniciar, crear solo los faltantes conservando los existentes, o cancelar sin cambios.
- Si el tamaño de `recordings.music` no es múltiplo de 48 bytes o el índice no es múltiplo de 40 bytes, se informa un error sin reparar ni truncar archivos.
- Los fallos de lectura, escritura, ordenación o datos inválidos se reportan explícitamente; una operación incompleta no se comunica como exitosa.
- Las rutas de archivo y tamaños se declaran en `src/app/util/IConstants.java`.

## Assets

- Formato de comando: `specs/backend/ft-4-serializar-comandos.md`.
- Ordenación externa del índice: `specs/database/ft-5-ordenar-indice-en-disco.md`.
- Archivos `recordings.music`, `recordings.index` y `recordings.list`, relativos al directorio de trabajo de la aplicación.
- Paquetes previstos en `src/app/storage/`, `src/app/sorting/` y `src/app/exceptions/`.

## Implementation guidelines

- Usar Java 26 y APIs estándar de archivos, con acceso aleatorio para búsqueda binaria.
- Escribir los registros binarios en orden big-endian con los tamaños acordados en ft-4.
- Buscar duplicados antes de escribir y nunca cargar el índice completo en memoria para buscar u ordenar.
- Delegar el ordenamiento de `recordings.index` a la implementación de ft-5.
- Mantener el timestamp de la lista en ISO-8601 UTC y anexar las entradas en orden de grabación.
- Usar una excepción de almacenamiento explícita bajo `src/app/exceptions/`; no ocultar corrupción ni errores de E/S.
- Seguir las convenciones del proyecto, incluidos parámetros con prefijo `p`, constantes en `IConstants` y un tipo público por archivo.

## Business workflow

1. Al primer acceso, comprobar los tres archivos; crear los tres vacíos si ninguno existe.
2. Si el conjunto es parcial, solicitar decisión a la interfaz y esperar la elección antes de modificar archivos.
3. Validar alineación de tamaños y datos; rechazar archivos corruptos.
4. Buscar el nombre mediante búsqueda binaria en disco; rechazar duplicados sin cambios.
5. Agregar encabezado y comandos al final de `recordings.music`.
6. Agregar el nombre y offset al índice y ordenarlo con ft-5.
7. Anexar nombre y fecha/hora a `recordings.list`.
8. Informar éxito solo al completar las actualizaciones requeridas; comunicar explícitamente cualquier error.
