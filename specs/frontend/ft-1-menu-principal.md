---
id: ft-1
layer: frontend
status: approved
---

# Menú principal

## General description

Mostrar una ventana principal de Java Swing con el título `Power Band` y cuatro botones dispuestos verticalmente, en este orden: `Probar`, `Grabar`, `Reproducir` y `Salir`. Los tres primeros son placeholders en esta versión; no inician otras funciones.

## Inputs

- Clic del usuario en uno de los cuatro botones.

## Outputs

- Ventana principal visible con el título y los botones especificados.
- `Salir` cierra la ventana.
- `Probar`, `Grabar` y `Reproducir` aceptan clics y mantienen el menú abierto, sin iniciar otras funciones.

## Test criteria

- Al iniciar desde `src/app/powerBand.java`, se muestra la ventana del menú en el Event Dispatch Thread de Swing.
- La ventana tiene el título `Power Band`.
- Hay un titulo en medio de la ventana de color #2C4EE6 con las palabras "Power Band"
- Los cuatro botones están visibles, habilitados y ordenados verticalmente como se especifica.
- Al hacer clic en `Probar`, `Grabar` o `Reproducir`, el menú permanece abierto y no se produce una excepción.
- Al hacer clic en `Salir`, la ventana se cierra.

## Assets

None.

## Implementation guidelines

- Usar Java Swing.
- Mantener el punto de entrada en `src/app/powerBand.java`.
- Si hace falta separar la interfaz, ubicarla en el paquete `app.ui` bajo `src/app/ui/`.
- Seguir las convenciones del proyecto: paquetes en minúsculas, clases en PascalCase, métodos y campos en camelCase y un tipo público de nivel superior por archivo.

## Business workflow

1. Iniciar la aplicación y mostrar el menú principal.
2. Permitir al usuario seleccionar cualquiera de los cuatro botones.
3. Mantener el menú visible para `Probar`, `Grabar` y `Reproducir`.
4. Cerrar la ventana al seleccionar `Salir`.
