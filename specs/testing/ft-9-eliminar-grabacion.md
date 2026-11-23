---
id: ft-9
layer: testing
status: approved
---

# Pruebas para eliminar grabaciones

## General description

Verificar la eliminación confirmada desde la interfaz hasta los archivos, incluyendo cancelación, actualización de offsets y preservación de las demás grabaciones.

## Inputs

- Bibliotecas temporales con cero, una o varias grabaciones.
- Nombres existentes, inexistentes y con diferencias de mayúsculas.
- Confirmación, cancelación y errores de almacenamiento simulados.

## Outputs

- Aserciones sobre cambios binarios y textuales, offsets, resultados, mensajes y llamadas a servicios.

## Test criteria

- Cancelar o cerrar la confirmación no invoca la eliminación y no modifica los archivos.
- Confirmar elimina solo la grabación seleccionada y actualiza la lista del selector.
- La confirmación identifica correctamente el nombre elegido y ofrece `Cancelar` y `Confirmar`.
- Eliminar grabaciones al inicio, en medio y al final conserva intactos los comandos restantes y ajusta encabezados/índice.
- El índice continúa ordenado, la lista conserva el orden cronológico y ninguna incluye la grabación eliminada.
- La biblioteca con una grabación queda vacía; un nombre inexistente no produce cambios.
- Las grabaciones restantes siguen pudiéndose listar y leer tras eliminar otra.
- Los errores de almacenamiento se muestran explícitamente y no generan mensajes de éxito.
- Las pruebas usan directorios temporales, interacciones Swing simuladas y dependencias falsas; no usan archivos reales del usuario ni SuperCollider.

## Assets

- JUnit 5 (`org.junit.jupiter`), configurado en `pom.xml`.
- Pruebas y almacenamiento aislados con `@TempDir`; dependencias inyectables para la UI.

## Implementation guidelines

- Ubicar pruebas bajo `src/test/app/`, reflejando los paquetes de `src/app/`.
- Ejecutar con Maven (`mvn test`) y usar JUnit 5.
- Ejecutar interacciones Swing en el Event Dispatch Thread.
- Comprobar directamente bytes, offsets, orden y contenido de todos los archivos.
- Seguir las convenciones de nombres y organización de las pruebas existentes.

## Business workflow

1. Crear directorios temporales con bibliotecas de distintos tamaños y órdenes.
2. Probar cancelación y confirmación de la interfaz, además de biblioteca vacía y errores.
3. Eliminar registros en distintas posiciones y comparar contenido, offsets e índices.
4. Volver a listar y leer las grabaciones restantes para comprobar que continúan intactas.
