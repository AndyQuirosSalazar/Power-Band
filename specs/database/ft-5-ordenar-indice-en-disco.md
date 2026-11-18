---
id: ft-5
layer: database
status: draft
---

# Ordenar índice en disco

## General description

Implementar Balanced Merge Sort externo para ordenar `recordings.index` por el nombre almacenado en cada registro fijo de 40 bytes. El algoritmo ordena mediante archivos auxiliares, sin cargar el índice completo en arreglos o colecciones en memoria.

## Inputs

- Archivo de índice binario con registros de 40 bytes: nombre ASCII en 32 bytes y offset de encabezado (`long`) en 8 bytes.
- Orden lexicográfico ASCII sensible a mayúsculas.
- Ubicación para los archivos auxiliares temporales.

## Outputs

- Archivo de índice ordenado por nombre, conservando todos los registros y sus offsets.
- Error explícito si el archivo no puede leerse, escribirse u ordenarse.

## Test criteria

- Un índice vacío o con un solo registro termina correctamente sin alterar sus registros.
- Registros en orden ascendente, descendente y mixto quedan ordenados correctamente.
- El orden distingue mayúsculas y usa la misma comparación lexicográfica ASCII que ft-6 empleará para la búsqueda binaria.
- Ningún registro se pierde, duplica o modifica durante las pasadas de mezcla.
- El tamaño del archivo antes y después es idéntico y múltiplo de 40 bytes.
- Un índice cuyo tamaño no sea múltiplo de 40 bytes produce un error; no se trunca ni repara.
- Casos con más registros que una corrida de mezcla fuerzan distribución y mezcla en archivos físicos.
- Los archivos temporales se limpian al finalizar tanto exitosamente como tras un error.
- La implementación no lee el índice completo en un arreglo o colección para ordenarlo.

## Assets

- Archivo de índice `recordings.index`, cuyo formato se comparte con `specs/database/ft-6-persistir-grabaciones.md`.
- Directorio temporal aislado para las pruebas.

## Implementation guidelines

- Implementar el algoritmo en `src/app/sorting/` usando Java 26 y APIs estándar de archivos.
- Usar corridas y archivos auxiliares físicos para distribuir y mezclar registros.
- Mantener en memoria solo registros/buffers de trabajo necesarios para la mezcla, nunca el índice completo.
- Compartir con ft-6 el mismo comparador lexicográfico ASCII sensible a mayúsculas.
- Limpiar recursos temporales en todas las rutas de salida e informar errores de E/S explícitamente.
- Seguir las convenciones del proyecto, incluidos parámetros con prefijo `p` y un tipo público por archivo.

## Business workflow

1. Validar que el tamaño del índice sea múltiplo de 40 bytes.
2. Distribuir los registros en corridas usando archivos auxiliares.
3. Mezclar corridas en pasadas sucesivas, comparando los nombres con el orden acordado.
4. Dejar el índice completo ordenado en su archivo y eliminar temporales.
