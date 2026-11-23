---
id: ft-9
layer: database
status: approved
---

# Eliminar grabaciones de la biblioteca

## General description

Extender la biblioteca para borrar permanentemente una grabación por nombre y mantener coherentes `recordings.music`, `recordings.index` y `recordings.list`. La grabación eliminada no debe poder listarse ni reproducirse.

## Inputs

- Nombre exacto de una grabación seleccionada y confirmada.
- Archivos actuales de la biblioteca.

## Outputs

- Resultado que indique si se eliminó una grabación o si no se encontró el nombre.
- Archivos actualizados conservando el formato, los datos de las demás grabaciones y su orden cronológico.
- Error explícito ante archivos corruptos o fallos de lectura/escritura.

## Test criteria

- La búsqueda del registro usa el índice ordenado por búsqueda binaria en disco y distingue mayúsculas.
- Eliminar una grabación quita su encabezado y comandos de `recordings.music`; las grabaciones restantes quedan contiguas.
- Los offsets del encabezado y del índice de cada grabación restante apuntan a sus datos después de compactar el archivo.
- `recordings.index` ya no contiene el nombre eliminado y mantiene el orden por nombre y los offsets correctos.
- `recordings.list` ya no contiene la entrada eliminada y conserva el orden y las fechas de las demás.
- Eliminar la única grabación deja los tres archivos vacíos; eliminar de un archivo válido vacío no modifica los archivos y devuelve no encontrada.
- Un nombre inexistente no modifica ningún archivo.
- Tras eliminar una grabación, las demás siguen pudiéndose listar, buscar y leer con los comandos originales.
- Los archivos malformados y fallos de E/S producen error explícito y nunca se comunica éxito incompleto.
- La operación no carga listas completas de registros binarios en memoria; limpia los archivos temporales usados.

## Assets

- Formatos binarios y reglas de búsqueda de `specs/database/ft-6-persistir-grabaciones.md`.
- Contrato de listado y lectura de `specs/database/ft-8-reproducir-grabacion.md`.
- `RecordingLibrary`, `RecordingStorage`, `BalancedMergeSort` y `RecordingStorageException`.

## Implementation guidelines

- Usar Java 26 y APIs estándar; mantener persistencia en `src/app/storage/` y seguir el contrato de `src/app/storage/RecordingLibrary.java`.
- Compactar físicamente el archivo de música y actualizar offsets absolutos de encabezados e índice sin cargar todas las grabaciones en memoria.
- Conservar el orden por nombre del índice y el orden cronológico de la lista.
- Preparar archivos temporales completos antes de reemplazar los datos; propagar fallos y no informar éxito hasta actualizar los tres archivos.
- No crear ni reparar archivos ausentes o corruptos como efecto secundario de una consulta de eliminación.
- Seguir las convenciones del proyecto, incluidos parámetros con prefijo `p`, constantes en `src/app/util/IConstants.java` y un tipo público por archivo.

## Business workflow

1. Validar la presencia y estructura de los archivos sin inicializarlos ni repararlos.
2. Buscar el nombre exacto mediante búsqueda binaria por seek en `recordings.index`.
3. Si no existe, devolver no encontrada sin modificar archivos.
4. Validar el encabezado y el rango de comandos de la grabación seleccionada.
5. Generar los archivos actualizados: omitir la grabación, compactar música, corregir offsets y filtrar su entrada de la lista.
6. Reemplazar los archivos de la biblioteca y limpiar temporales.
7. Informar éxito solo cuando los tres archivos estén actualizados; propagar cualquier error.
