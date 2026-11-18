---
id: ft-7
layer: frontend
status: draft
---

# Guardar grabación desde la interfaz

## General description

Al finalizar una grabación, permitir guardarla con un nombre o cancelarla. Validar el nombre y ofrecer una decisión clara cuando falten algunos archivos de almacenamiento. Depende del contrato de persistencia de ft-6.

## Inputs

- Grabación completada: comandos con tiempos relativos y fecha/hora de creación.
- Nombre escrito por el usuario.
- Resultado de búsqueda de nombre y operación de almacenamiento de ft-6.
- Estado de inicialización de los archivos de grabaciones.

## Outputs

- Grabación guardada y confirmación visible, o advertencias que permitan corregir el nombre.
- Al cancelar el guardado no se modifica la biblioteca.
- Si existe solo una parte de los archivos, ofrecer borrar únicamente `recordings.music`, `recordings.index` y `recordings.list` e iniciar una biblioteca nueva; crear solo los faltantes conservando los existentes; o cancelar sin cambios.
- Errores de almacenamiento visibles; no informar éxito si la operación falla.

## Test criteria

- Al completar una grabación se presentan las acciones Guardar y Cancelar y un campo para el nombre.
- Se acepta un nombre de 1 a 31 caracteres ASCII imprimibles; los espacios iniciales/finales se eliminan antes de validarlo.
- Un nombre vacío, no ASCII, mayor a 31 caracteres o ya existente produce una advertencia y permite corregirlo sin perder la grabación.
- La comparación distingue mayúsculas: `Piano` y `piano` son nombres distintos.
- Cancelar no escribe la grabación.
- Ante archivos parciales, las tres opciones acordadas se presentan claramente; cancelar conserva los archivos sin cambios.
- Los errores de lectura/escritura se muestran explícitamente y no bloquean el Event Dispatch Thread.

## Assets

- Botón existente `Grabar` (`IConstants.BUTTON_RECORD`).
- Etiquetas en español: Guardar y Cancelar.
- Contrato de almacenamiento: `specs/database/ft-6-persistir-grabaciones.md`.

## Implementation guidelines

- Usar Java 26, Maven y Swing; mantener la interfaz en `src/app/ui/` y la lógica de negocio en `src/app/logic/`.
- Conectar el guardado al almacenamiento de ft-6 y a los comandos Bridge existentes.
- Ejecutar operaciones de disco fuera del Event Dispatch Thread y reportar errores explícitamente.
- Seguir las convenciones del proyecto: paquetes en minúsculas, nombres Java convencionales, parámetros con prefijo `p` y un tipo público por archivo.

## Business workflow

1. Al finalizar una grabación, mostrar el campo de nombre y las opciones Guardar y Cancelar.
2. Recortar espacios externos y validar de 1 a 31 caracteres ASCII imprimibles.
3. Consultar al almacenamiento de ft-6 si el nombre ya existe, con comparación sensible a mayúsculas.
4. Si el nombre no es válido o existe, mostrar una advertencia y conservar la grabación para corregirlo.
5. Si los archivos están incompletos, presentar las opciones acordadas y aplicar únicamente la decisión seleccionada.
6. Si el nombre es válido, solicitar el guardado y mostrar éxito solo al completarse.
7. Si se cancela, no modificar los archivos de grabaciones.
