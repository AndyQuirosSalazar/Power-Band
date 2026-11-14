---
id: ft-3
layer: testing
status: approved
---

# Pruebas de integracion con SuperCollider

## General description

Verificar que los comandos Bridge produzcan los mensajes OSC correctos para los synths asignados y que el flujo de prueba use y cierre correctamente `BandClient`, sin requerir dispositivos fisicos.

## Inputs

- Comandos simulados `BassCommand`, `PianoCommand` y `OrganCommand`.
- Un receptor UDP local de prueba para capturar paquetes OSC.
- Eventos simulados de mando, tableta y microfono cuando se pruebe el flujo de sesion.

## Outputs

- Resultados verificables de direccion OSC, synth, controles y ciclo de vida del cliente.

## Test criteria

- Un `BassCommand` produce `/s_new` para `stringBass` con su nota y duracion.
- Un `PianoCommand` produce `/s_new` para `classicPiano` con nota, duracion y tercera mayor o menor.
- Un `OrganCommand` audible produce `/s_new` para `organ70s` con frecuencia y controles de parciales esperados.
- Un `OrganCommand` bajo el umbral produce `/n_set` con `gate` en cero para la nota activa.
- Las señales de mando, tableta y microfono llegan solo al synth asignado; flotar con la tableta no genera un comando de piano.
- El flujo inicia una sola instancia de `BandClient` por sesion y la cierra al terminar la prueba.
- Un error local de envio se propaga al estado observable de la prueba y no se registra como envio exitoso.
- Las pruebas automatizadas se ejecutan sin SuperCollider, hardware ni pantalla fisica.
- La verificacion manual con SuperCollider confirma sonido para cada synth despues de iniciar el servidor y cargar sus definiciones; un receptor UDP de prueba por si solo no demuestra que `scsynth` proceso los mensajes.

## Assets

- JUnit 5 de `pom.xml`.
- Receptor UDP temporal y decodificador OSC de prueba, sin dependencias de dispositivos fisicos.
- Definiciones externas `classicPiano`, `stringBass` y `organ70s` para la verificacion manual.

## Implementation guidelines

- Ubicar pruebas bajo `src/test/`, reflejando los paquetes de las clases probadas.
- Usar JUnit 5 y Maven (`mvn test`); cerrar sockets y recursos aun si falla una asercion.
- Verificar mensajes OSC capturados en loopback y no depender de que SuperCollider este instalado para la suite automatizada.
- No afirmar recepcion del servidor a partir de un `DatagramSocket.send` exitoso.

## Business workflow

1. Iniciar un receptor UDP local controlado por la prueba.
2. Ejecutar los comandos Bridge con un `BandClient` configurado para loopback.
3. Decodificar los paquetes y verificar direccion, synth y controles.
4. Ejecutar el flujo de sesion con fuentes simuladas y verificar asignacion y cierre.
5. Como aceptacion manual, iniciar SuperCollider, cargar los synths y confirmar que las señales de cada instrumento producen el sonido correspondiente.
