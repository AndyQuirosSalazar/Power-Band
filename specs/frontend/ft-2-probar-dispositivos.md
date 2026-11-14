---
id: ft-2
layer: frontend
status: approved
---

# Probar dispositivos

## General description

Conectar el botón `Probar` del menú principal con una sesión para probar el mando, la tableta y el micrófono integrado. La tableta se lee desde un área dedicada con eventos AWT; esos eventos no permiten distinguir físicamente el lápiz de un mouse, por lo que el usuario se asegurará de usar el lápiz.

## Inputs

- Clic del usuario en `Probar`.
- Estado y eventos de los dispositivos permitidos, proporcionados por la lógica de prueba.

## Outputs

- Inicio de la prueba de mando, tableta de dibujo y micrófono integrado.
- Estado o errores de la prueba visibles para el usuario, sin bloquear el Event Dispatch Thread.

## Test criteria

- Al hacer clic en `Probar`, se inicia la prueba; los otros botones conservan su comportamiento.
- La interfaz comunica si la prueba puede comenzar o si una fuente permitida no está disponible.
- El área de la tableta reporta posición y contacto/flotación usando eventos AWT de movimiento, arrastre, pulsación y liberación.
- El área de la tableta no registra entradas de teclado ni instala un capturador global de mouse.

## Assets

- Etiqueta existente `Probar` (`IConstants.BUTTON_TEST`).

## Implementation guidelines

- Usar Java Swing y mantener el punto de entrada en `src/app/powerBand.java`.
- Integrar el botón existente en `src/app/ui/MainMenu.java` con la lógica bajo `src/app/logic/`.
- Seguir las convenciones del proyecto: paquetes en minúsculas, nombres Java convencionales y un tipo público por archivo.

## Business workflow

1. El usuario selecciona `Probar`.
2. La interfaz inicia la sesión de prueba mediante la lógica de dispositivos.
3. La interfaz presenta el estado de las fuentes permitidas y los errores explícitos.
