---
id: ft-9
layer: backend
status: approved
---

# Eliminar una grabación seleccionada

## General description

Coordinar la eliminación de una grabación confirmada por la interfaz mediante la biblioteca existente. La operación identifica el registro por nombre exacto y comunica si fue eliminado o ya no existe.

## Inputs

- Nombre de la grabación seleccionada y confirmada por la interfaz.
- Biblioteca de grabaciones inyectada.

## Outputs

- Resultado que distingue eliminación exitosa de nombre no encontrado.
- Error de almacenamiento explícito para fallos de lectura o escritura.

## Test criteria

- La operación delega a la biblioteca el nombre recibido sin seleccionar ni eliminar otra grabación.
- Un nombre no encontrado se informa como tal y no se reporta éxito.
- Los errores de almacenamiento se propagan para que la interfaz los muestre; no se convierten en éxito.
- La lógica no solicita confirmación ni depende de Swing.

## Assets

- Contrato de biblioteca y resultado de `specs/database/ft-9-eliminar-grabacion.md`.
- `RecordingLibrary` y manejo de errores de almacenamiento existentes.

## Implementation guidelines

- Usar Java 26; mantener la lógica de negocio en `src/app/logic/` y reutilizar `src/app/storage/RecordingLibrary.java`.
- Mantener la confirmación en la capa de interfaz; llamar a la lógica solo después de confirmar.
- Inyectar la biblioteca para pruebas deterministas.
- Seguir las convenciones del proyecto, incluidos parámetros con prefijo `p` y un tipo público por archivo.

## Business workflow

1. Recibir el nombre confirmado por la interfaz.
2. Solicitar a la biblioteca eliminar esa grabación.
3. Devolver éxito solo si la biblioteca la eliminó; informar ausencia o propagar el error de almacenamiento.
