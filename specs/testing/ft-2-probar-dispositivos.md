---
id: ft-2
layer: testing
status: approved
---

# Pruebas de dispositivos

## General description

Verificar el flujo iniciado por `Probar`, las fuentes de entrada, el filtrado de señales y la asignación de comandos a synths, usando fuentes simuladas en lugar de hardware real.

## Inputs

- Clic simulado en `Probar`.
- Eventos simulados de mando, tableta, micrófono integrado.

## Outputs

- Resultados verificables del inicio de la prueba, filtrado de eventos y comandos enviados.

## Test criteria

- El botón `Probar` inicia la prueba y la interfaz maneja estados y errores sin bloquear el Event Dispatch Thread.
- Las direcciones del mando generan comandos solo para `stringBass`.
- Posición y estado tocando/flotando de la tableta generan comandos solo para `classicPiano`.
- El nivel del micrófono integrado genera comandos solo para `organ70s`.
- Izquierda/derecha/arriba/abajo generan notas de bajo MIDI 36/43/45/41, sin repetir mientras la dirección no cambie.
- La posición X de tableta recorre las notas MIDI 48–72; Y elige mayor/menor; solo el contacto dispara piano.
- El volumen produce amplitudes de órgano cuantizadas; un nivel menor que 0.02 apaga la nota.
- Botones/ejes adicionales del mando y eventos AWT fuera del área de tableta no generan comandos.
- Los eventos AWT dentro del área dedicada se consideran entrada de lápiz; no se prueba distinguir un mouse físico de un lápiz.
- La interfaz no registra entradas de teclado ni usa un capturador global de mouse.
- La ausencia o fallo de una fuente permitida se reporta explícitamente y no se trata como lectura válida.
- Las pruebas pueden ejecutarse sin hardware ni pantalla física.

## Assets

- JUnit 5 (`org.junit.jupiter`), configurado en `pom.xml`.
- Adaptadores o dobles de prueba para las fuentes de eventos y el envío de comandos.

## Implementation guidelines

- Ubicar pruebas bajo `src/test/`, reflejando el paquete de la lógica o interfaz probada.
- Usar JUnit 5 y Maven (`mvn test`); ejecutar interacciones Swing en el Event Dispatch Thread.
- No depender de dispositivos físicos ni añadir pruebas de grabación o reproducción.
- Probar eventos AWT únicamente sobre el componente de tableta en la interfaz.

## Business workflow

1. Preparar dobles de las fuentes permitidas y del destino de comandos.
2. Simular el clic en `Probar` y comprobar que la prueba se inicia.
3. Enviar eventos válidos e inválidos y verificar filtrado y asignación a synths.
4. Simular fuentes no disponibles o fallidas y verificar que se informa el error.
