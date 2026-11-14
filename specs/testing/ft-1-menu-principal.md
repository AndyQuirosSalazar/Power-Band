---
id: ft-1
layer: testing
status: approved
---

# Pruebas del menú principal

## General description

Verificar la presentación del menú principal y la respuesta básica de sus cuatro botones, sin probar funciones de grabación, reproducción o instrumentos.

## Inputs

- Instancia de la ventana del menú.
- Clics simulados en `Probar`, `Grabar`, `Reproducir` y `Salir`.

## Outputs

- Resultados de prueba para el título, visibilidad, habilitación, orden y respuesta de los botones.

## Test criteria

- Las pruebas pueden ejecutarse en un entorno sin pantalla física (headless).
- Se verifica el título `Power Band` y que los botones visibles y habilitados aparecen en el orden requerido.
- Los clics en `Probar`, `Grabar` y `Reproducir` no cierran el menú ni lanzan excepciones.
- El clic en `Salir` cierra la ventana.
- Las interacciones con Swing se ejecutan en el Event Dispatch Thread.

## Assets

- JUnit, de acuerdo con el stack definido en `CLAUDE.md`.

## Implementation guidelines

- Ubicar las pruebas bajo `src/test/`, reflejando el paquete de la interfaz.
- Probar únicamente el comportamiento observable del menú; no implementar ni simular funcionalidades futuras de los botones placeholder.

## Business workflow

1. Crear la ventana en el entorno de pruebas.
2. Comprobar título, botones, orden y estado habilitado.
3. Simular los clics de los botones placeholder y verificar que la ventana permanezca abierta.
4. Simular el clic en `Salir` y verificar que la ventana se cierre.
