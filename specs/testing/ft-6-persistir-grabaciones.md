---
id: ft-6
layer: testing
status: approved
---

# Pruebas de persistencia de grabaciones

## General description

Verificar la escritura de una grabación en los tres archivos, la búsqueda binaria de nombres, la inicialización y el manejo de errores usando almacenamiento temporal.

## Inputs

- Secuencias de `CommandRecord` de prueba.
- Directorios temporales con archivos ausentes, completos, parciales o corruptos.
- Nombres válidos, inválidos, duplicados y que difieren por mayúsculas.
- Decisiones de inicialización simuladas.

## Outputs

- Aserciones sobre bytes, offsets, lista cronológica, búsqueda y errores.

## Test criteria

- Encabezados y comandos en `recordings.music` usan registros de 48 bytes; entradas de índice usan 40 bytes.
- Las posiciones del encabezado y los rangos de comandos apuntan a los datos esperados.
- El índice resultante está ordenado por ft-5; las grabaciones nuevas aparecen en orden de creación en `recordings.list`.
- La búsqueda encuentra y rechaza nombres existentes, distingue mayúsculas y consulta el archivo por posiciones sin cargar todo el índice.
- Rechazar un nombre duplicado no cambia ningún archivo.
- Los tres archivos ausentes se inicializan; archivos parciales aplican cada decisión seleccionada y cancelar no cambia los datos.
- Archivos corruptos se rechazan sin truncamiento ni reparación automática.
- Fallos al leer o escribir se exponen y no producen un resultado de éxito.

## Assets

- JUnit 5 (`org.junit.jupiter`), configurado en `pom.xml`.
- Directorios temporales aislados y datos de comandos simulados.

## Implementation guidelines

- Ubicar pruebas bajo `src/test/app/storage/`.
- Ejecutar con Maven (`mvn test`) y usar JUnit 5.
- Comprobar directamente tamaños, contenido y posiciones de los archivos.
- No depender de archivos de usuario, dispositivos o servicios externos.

## Business workflow

1. Preparar archivos temporales con diferentes estados iniciales.
2. Agregar grabaciones de prueba y verificar los tres archivos.
3. Probar existencia, duplicados, orden del índice y orden cronológico de la lista.
4. Probar inicialización parcial, cancelación, corrupción y errores de E/S.
