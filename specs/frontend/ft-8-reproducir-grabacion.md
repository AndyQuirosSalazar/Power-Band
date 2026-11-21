---
id: ft-8
layer: frontend
status: approved
---

# Seleccionar y reproducir una grabación

## General description

Conectar el botón `Reproducir` del menú con un diálogo para elegir una grabación guardada y reproducirla. Mostrar las grabaciones en orden cronológico y conservar el menú disponible.

## Inputs

- Clic en `Reproducir` (`IConstants.BUTTON_PLAY`).
- Lista de nombres y fechas de creación de la biblioteca.
- Grabación seleccionada por el usuario.
- Estado y errores de lectura y reproducción de ft-8 database/backend.

## Outputs

- Diálogo con las grabaciones disponibles, mostrando nombre y fecha/hora en orden cronológico.
- Reproducción de la grabación seleccionada, o mensaje claro cuando no haya grabaciones o ocurra un error.
- Cancelar o cerrar el selector no inicia reproducción ni modifica la biblioteca.

## Test criteria

- Al hacer clic en `Reproducir` se muestran todas las grabaciones disponibles en el orden de `recordings.list`.
- El usuario debe seleccionar una grabación y confirmar Reproducir; cancelar no envía comandos.
- Una biblioteca vacía muestra un mensaje informativo y no abre una reproducción.
- Mientras una reproducción está activa, la UI permanece receptiva y no permite iniciar otra simultánea.
- Los errores de lectura o envío se muestran explícitamente y no se comunican como éxito.
- El menú principal permanece abierto durante y después de la selección/reproducción.

## Assets

- Botón existente `Reproducir` (`IConstants.BUTTON_PLAY`).
- Etiquetas en español: Reproducir y Cancelar.
- Contratos: `specs/database/ft-8-reproducir-grabacion.md` y `specs/backend/ft-8-reproducir-grabacion.md`.

## Implementation guidelines

- Usar Java 26, Maven y Swing; mantener la UI en `src/app/ui/` y conectar el botón desde `src/app/powerBand.java`/`src/app/ui/MainMenu.java`.
- Ejecutar la lectura y reproducción fuera del Event Dispatch Thread y actualizar Swing en el EDT.
- Inyectar los servicios para que el selector y los estados puedan probarse sin archivos de usuario ni SuperCollider real.
- No alterar los archivos de grabaciones durante la selección o reproducción.
- Seguir las convenciones del proyecto, incluidos parámetros con prefijo `p` y un tipo público por archivo.

## Business workflow

1. Al pulsar Reproducir, solicitar la lista de grabaciones a la biblioteca.
2. Si está vacía, informar que aún no hay grabaciones.
3. Si contiene grabaciones, mostrar nombre y fecha/hora en orden cronológico.
4. Esperar una selección y confirmación; al cancelar, cerrar sin efectos.
5. Iniciar la reproducción de la selección y mantener la UI receptiva.
6. Al terminar o fallar, actualizar el estado y mostrar errores explícitos.
