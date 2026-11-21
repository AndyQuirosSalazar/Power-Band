---
id: ft-8
layer: database
status: approved
---

# Leer grabaciones para reproducir

## General description

Extender la persistencia para listar las grabaciones guardadas y leer los comandos de una por nombre. La lectura debe usar el índice binario existente y respetar los offsets del encabezado sin modificar la biblioteca.

## Inputs

- Nombre de grabación seleccionado por el usuario.
- Archivos `recordings.music`, `recordings.index` y `recordings.list`.

## Outputs

- Lista inmutable de metadatos de grabaciones (nombre y fecha/hora de creación), en el orden cronológico de `recordings.list`.
- Secuencia de `CommandRecord` de la grabación seleccionada, en el orden almacenado.
- Error explícito de almacenamiento ante datos incompletos, corruptos o errores de lectura.

## Test criteria

- La lista conserva el orden de creación de `recordings.list`, aunque `recordings.index` esté ordenado por nombre.
- La búsqueda del nombre seleccionado usa búsqueda binaria por seek en `recordings.index`, con comparación ASCII sensible a mayúsculas.
- Se leen únicamente los comandos delimitados por el encabezado seleccionado: inicio inclusivo y fin exclusivo.
- Los offsets del encabezado están alineados a registros de 48 bytes, dentro de `recordings.music`, y delimitan una cantidad válida de comandos.
- Cada comando se decodifica con el formato y validaciones de ft-4.
- Si los tres archivos no existen, la consulta devuelve una lista vacía. Si solo existe parte del conjunto o los archivos están corruptos, se informa el error sin crear, reparar, truncar ni modificar archivos.
- Un nombre no existente produce un resultado explícito de no encontrado; no se devuelve otra grabación como sustituto.

## Assets

- Formato binario de `recordings.music` e índice: `specs/database/ft-6-persistir-grabaciones.md`.
- Formato de comando: `specs/backend/ft-4-serializar-comandos.md`.
- Archivos existentes en `src/app/storage/RecordingStorage.java` e interfaz `RecordingLibrary`.

## Implementation guidelines

- Usar Java 26 y APIs estándar de archivos.
- Mantener persistencia en `src/app/storage/` y los metadatos de grabación en `src/app/model/` si se necesita un tipo de dominio.
- Leer el índice por acceso aleatorio; no cargar el índice completo para buscar.
- Las consultas de reproducción son de solo lectura y no invocan la inicialización que crea archivos ni las decisiones de ft-7.
- Usar `RecordingStorageException` para corrupción y errores de almacenamiento; no ocultar fallos de E/S.
- Seguir las convenciones del proyecto, incluidos parámetros con prefijo `p`, constantes en `IConstants` y un tipo público por archivo.

## Business workflow

1. Comprobar la presencia del conjunto de archivos sin modificarlo.
2. Leer los metadatos desde `recordings.list` y conservar su orden cronológico.
3. Buscar por nombre en el índice ordenado mediante búsqueda binaria en disco.
4. Leer y validar el encabezado en el offset asociado al índice.
5. Leer y validar los comandos del rango `[inicio, fin)` del encabezado.
6. Entregar la lista o secuencia solicitada, o reportar explícitamente ausencia, corrupción o error de lectura.
