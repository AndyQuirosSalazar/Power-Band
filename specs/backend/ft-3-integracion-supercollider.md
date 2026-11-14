---
id: ft-3
layer: backend
status: approved
---

# Integracion con SuperCollider

## General description

Enviar a SuperCollider, mediante `BandClient` y OSC/UDP, los comandos producidos por las señales de dispositivos durante la prueba iniciada desde `Probar`. Cada comando debe invocar el metodo del synth asignado a su instrumento.

## Inputs

- `BassCommand` producido por las direcciones del mando.
- `PianoCommand` producido por la posicion y el contacto de la tableta.
- `OrganCommand` producido por el nivel del microfono.

## Outputs

- Mensajes OSC dirigidos al servidor SuperCollider en `127.0.0.1:57110`.
- Errores locales de inicializacion o envio disponibles para el estado de la prueba.

## Test criteria

- `BassCommand` invoca `BandClient.playBass(note, durMs)` para `stringBass`.
- `PianoCommand` invoca `BandClient.playPiano(note, durMs, major)` para `classicPiano`.
- `OrganCommand` invoca `BandClient.noteOn(...)` para volumen audible y `BandClient.noteOff(note)` por debajo del umbral de silencio.
- Los mensajes OSC contienen el synth y los controles esperados; ningun comando se envia a un synth distinto del asignado.
- La sesion de prueba mantiene un cliente y lo cierra al terminar; los errores locales de envio se informan y no se presentan como envios exitosos.
- La creacion del socket UDP o el envio de un paquete no se considera confirmacion de que SuperCollider lo recibio.

## Assets

- Cliente OSC existente: `src/app/supercollider/BandClient.java`.
- Comandos Bridge existentes: `src/app/bridge/commands/`.
- Definiciones cargadas en SuperCollider: `classicPiano`, `stringBass` y `organ70s`; el servidor debe estar iniciado y las definiciones cargadas antes de probar.
- El proyecto no contiene archivos `.sc`; las definiciones de synth son un requisito externo de ejecucion, no un archivo a generar en esta capa.

## Implementation guidelines

- Implementar solo Java 26 en Maven; conservar el punto de entrada en `src/app/powerBand.java`.
- Mantener la asignacion de instrumentos y comandos del patron Bridge existente.
- Integrar el ciclo de vida de `BandClient` con la sesion de prueba, sin crear un cliente por cada señal.
- Mantener los valores de destino OSC predeterminados de `BandClient` (`127.0.0.1:57110`).
- No declarar conectado o saludable al servidor sin una confirmacion OSC; UDP no confirma la recepcion.
- Informar los fallos locales de envio a la interfaz mediante el mecanismo de estado existente.

## Business workflow

1. Al iniciar `Probar`, crear un `BandClient` para la sesion.
2. Recibir los comandos Bridge generados por las fuentes de dispositivos.
3. Ejecutar cada comando sobre el metodo de `BandClient` correspondiente a su synth.
4. Informar errores locales de inicializacion o envio.
5. Al cerrar la prueba, detener las fuentes y cerrar el cliente.
