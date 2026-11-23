---
id: ft-9
layer: frontend
status: approved
---

# Eliminar una grabación

## General description

Agregar una acción `Eliminar` al menu de reproducir junto al boton de reproducir, que se pueda seleccionar el boton cuando se selecciona una grabacion tal cual funciona el boton de reproducir.
## Inputs

- Clic en `Eliminar` (`IConstants.BUTTON_DELETE`).
- Grabación seleccionada y confirmación de eliminar.

## Outputs

- Selector con las grabaciones disponibles en orden cronológico y una acción `Eliminar` para la selección.
- Confirmación que identifica la grabación y ofrece `Cancelar` y `Confirmar`.
- Mensaje de eliminación exitosa o error explícito.

## Test criteria

- El botón `Eliminar` abre la confirmacion de eliminar la grabacion.
- El usuario puede elegir una grabación y solicitar eliminarla.
- Seleccionar una grabación por sí solo no la elimina.
- La confirmación muestra el nombre elegido y las opciones `Cancelar` y `Confirmar`.
- Cancelar o cerrar la confirmación no llama al servicio de eliminación ni modifica archivos.
- Confirmar solicita borrar exactamente la grabación seleccionada; la UI permanece receptiva durante la operación.
- Al eliminar con éxito, se informa el resultado y la lista refleja el cambio.
- Una biblioteca vacía muestra un mensaje informativo; los errores se muestran y nunca se presentan como éxito.
- El menú principal permanece disponible.

## Assets

- Botón `Eliminar` y etiquetas en español para eliminar, confirmar y cancelar.
- Selector y listado cronológico de `specs/frontend/ft-8-reproducir-grabacion.md`.
- Contratos de `specs/database/ft-9-eliminar-grabacion.md` y `specs/backend/ft-9-eliminar-grabacion.md`.

## Implementation guidelines

- Usar Java 26, Maven y Swing; declarar `BUTTON_DELETE` en `src/app/util/IConstants.java`, mantener la UI en `src/app/ui/` y conectar la acción desde `src/app/powerBand.java` y `src/app/ui/MainMenu.java`.
- Reutilizar el patrón de selección y listado de `src/app/ui/RecordingPlaybackPanel.java` cuando sea apropiado.
- Ejecutar la eliminación fuera del Event Dispatch Thread y actualizar Swing en el EDT.
- Inyectar las dependencias e interacciones de confirmación para pruebas sin archivos reales.
- Seguir las convenciones del proyecto, incluidos parámetros con prefijo `p` y un tipo público por archivo.

## Business workflow

1. Al pulsar `Eliminar`, se abre la ventana de confirmacion.
2. Al solicitar eliminar, mostrar una confirmación con el nombre seleccionado.
3. Si el usuario cancela, conservar la biblioteca y cerrar solo la confirmación.
4. Si confirma, ejecutar la eliminación fuera del EDT.
5. Mostrar éxito solo al completarse; refrescar la lista o informar explícitamente el error.
