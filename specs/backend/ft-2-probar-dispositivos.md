---
id: ft-2
layer: backend
status: approved
---

# Probar dispositivos

## General description

Admitir el mando consultado con GLFW, eventos AWT de un área dedicada a la tableta y el nivel del micrófono predeterminado del sistema. Traducir sus señales a comandos del synth correspondiente durante la prueba iniciada desde `Probar`. Los eventos AWT del área de tableta se consideran entrada del lápiz; la aplicación no puede distinguirlos de un mouse físico.

## Inputs

- Inicio de sesión de prueba desde la interfaz.
- Eventos del mando: direcciones.
- Eventos AWT locales al área de tableta: posición y estado tocando/flotando.
- Nivel de volumen del micrófono integrado.

## Outputs

- Direcciones del mando traducidas a comandos para `stringBass`.
- Posición y estado del lápiz traducidos a comandos para `classicPiano`.
- Volumen del micrófono integrado traducido a comandos para `organ70s`.
- Estado de disponibilidad y errores de adquisición para la interfaz.

## Test criteria

- Solo se consultan ejes direccionales del mando; se ignoran botones y ejes adicionales.
- Solo se procesan eventos AWT dentro del área dedicada a la tableta; posición y contacto/flotación se obtienen de posición, arrastre, pulsación y liberación. No se registra teclado ni se instala un capturador global.
- El micrófono usa únicamente el nivel de la entrada de grabación predeterminada del sistema; el audio no se guarda.
- Izquierda/derecha/arriba/abajo del mando producen, respectivamente, notas MIDI 36/43/45/41 de `stringBass`, una vez por cambio de dirección.
- La posición X de la tableta se escala a notas MIDI 48–72; la mitad superior/inferior de Y selecciona acorde mayor/menor. Tocar dispara `classicPiano`; flotar no lo dispara.
- El volumen RMS del micrófono escala la amplitud de `organ70s`; volumen menor que 0.02 apaga la nota y cambios se cuantizan en ocho niveles.
- Cada señal aceptada llega únicamente al synth asignado y ninguna señal descartada produce un comando.
- Si una fuente permitida no está disponible o falla, se informa el error sin simular una lectura exitosa.

## Assets

- Contratos Bridge a implementar: instrumentos y comandos bajo `src/app/bridge/instruments/` y `src/app/bridge/commands/`.
- Acceso a los synths existentes mediante `src/app/supercollider/BandClient.java`.
- LWJGL 3.3.4 GLFW para el mando; AWT/Swing para la tableta; Java Sound para el micrófono.

## Implementation guidelines

- Mantener la adquisición y traducción de eventos en `src/app/logic/`, separadas de Swing y del envío de comandos.
- Usar el patrón Bridge existente o previsto para desacoplar instrumentos y comandos.
- Usar Java 26 y Maven; aislar las fuentes físicas tras interfaces para poder probarlas sin hardware.
- Usar el micrófono de grabación predeterminado del sistema. Definir en `src/app/util/IConstants.java` el nombre configurable del dispositivo; vacío significa usar el predeterminado.
- No guardar audio; procesar únicamente buffers temporales para calcular volumen. No añadir teclado como fuente ni captar eventos de mouse fuera del área de tableta.
- Exponer los estados de entrada para poder verificar las asignaciones de dirección, posición/contacto y nivel de volumen especificadas en esta capa.

## Business workflow

1. La interfaz solicita iniciar la prueba.
2. La lógica habilita GLFW, el área AWT dedicada y la entrada de audio predeterminada; informa si alguna fuente no está disponible.
3. La lógica lee solo ejes direccionales, eventos del área de tableta y volumen del micrófono.
4. La lógica traduce las señales permitidas y envía cada comando al synth asignado.
5. La lógica informa el estado o los errores a la interfaz.
