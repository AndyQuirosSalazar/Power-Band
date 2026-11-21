---
id: ft-8
layer: backend
status: approved
---

# Reproducir comandos grabados

## General description

Recrear y ejecutar los comandos Bridge persistidos para una grabación seleccionada, respetando su orden y tiempos relativos. El envío se realiza con el cliente SuperCollider existente y no debe bloquear el Event Dispatch Thread.

## Inputs

- Nombre seleccionado y metadatos de ft-8 database.
- Secuencia de `CommandRecord` de ft-4/ft-6.
- Cliente de ejecución de synths o dependencia equivalente, inyectable para pruebas.

## Outputs

- Comandos Bridge ejecutados sobre el synth que corresponde al ID de cada registro.
- Estado de reproducción terminada, cancelada o fallida, con el error de lectura/envío disponible para la interfaz.

## Test criteria

- El ID de bajo produce `BassCommand`, el ID de piano produce `PianoCommand` con calidad mayor/menor correcta y el ID de órgano produce `OrganCommand` con el volumen almacenado.
- La secuencia de comandos y el orden de comandos con igual timestamp se conservan.
- Los intervalos entre eventos respetan los tiempos relativos registrados; el retraso de un evento no acumula deriva en los siguientes.
- La reproducción se ejecuta en segundo plano y permite que el EDT siga procesando eventos.
- Un error de lectura o envío termina la operación como fallida y se propaga para mostrarlo; no se presenta como reproducción completada.
- Una reproducción no inspecciona el audio ni modifica los comandos o archivos almacenados.

## Assets

- `CommandRecord` de 48 bytes: `specs/backend/ft-4-serializar-comandos.md`.
- Contrato de lectura de la biblioteca: `specs/database/ft-8-reproducir-grabacion.md`.
- `BassCommand`, `PianoCommand`, `OrganCommand`, `SynthCommand` y `BandClient` existentes.

## Implementation guidelines

- Usar Java 26; mantener la lógica de reproducción en `src/app/logic/` y reutilizar el patrón Bridge en `src/app/bridge/commands/`.
- Enviar a SuperCollider mediante `BandClient` en `src/app/supercollider/`; conservar la asignación de synths.
- Usar una fuente de tiempo monotónica para programar los eventos y evitar deriva acumulada.
- Mantener la lectura y reproducción fuera del EDT.
- Inyectar reloj/programador y destino de comandos donde sea necesario para pruebas sin SuperCollider.
- No afirmar que UDP confirma la recepción o audibilidad del sonido.
- Seguir las convenciones del proyecto, incluidos parámetros con prefijo `p` y un tipo público por archivo.

## Business workflow

1. Obtener la grabación seleccionada y su secuencia validada de comandos desde almacenamiento.
2. Iniciar la línea de tiempo usando un reloj monotónico.
3. Esperar cada timestamp relativo sin bloquear el EDT.
4. Convertir cada registro al comando Bridge correspondiente y enviarlo al synth asignado.
5. Finalizar con estado de éxito local o detener y reportar el primer error de lectura/envío.
