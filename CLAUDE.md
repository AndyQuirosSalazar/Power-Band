# Project description
Power Band es un programa de creación e interacción musical que permite crear, guardar y reproducir música generada mediante la invocación, desde Java, de sintetizadores hechos en SuperCollider.

Componentes
Sintetizadores (SuperCollider). Tres synths en myband.sc: classicPiano, stringBass y organ70s. Java los invoca mediante BandClient, que envía mensajes OSC por UDP al servidor de SuperCollider (127.0.0.1:57110).
Instrumentos. Dispositivos físicos conectados a Java, un dispositivo distinto por synth. No pueden ser teclado ni mouse.
Mando (stick) controla classicPiano.
Tableta de dibujo controla organ70s. Solo lee dónde está el lápiz y si está tocando o flotando.
Micrófono controla stringBass. Solo lee el volumen.
Editor musical. Monitorea los comandos que los instrumentos envían a SuperCollider, y permite grabar, almacenar, cargar y reproducir música.
Editor musical
Grabar: guarda todos los comandos y sus tiempos en un archivo en tiempo real. Al terminar, se asigna un nombre único a la música.
Cargar: muestra las músicas disponibles en orden cronológico (desde recordings.list).
Reproducir: busca la grabación por nombre con búsqueda binaria en disco y envía los comandos respetando los tiempos grabados, para que suene idéntica.
Archivos
Archivo    Contenido
recordings.music    Todas las grabaciones, una tras otra, cada una con encabezado (nombre, punto de inicio y punto de terminación).
recordings.index    Índice de las músicas grabadas, ordenado por nombre.
recordings.list    Nombre y fecha y hora de cada grabación.

Los registros de recordings.music y recordings.index son de tamaño fijo, escritos en binario. El índice se ordena con Balanced Merge Sort sobre archivos en disco; no se ordenan ni recorren arreglos en memoria.

Patrón de diseño

Bridge: la jerarquía de instrumentos (abstracción) está separada de la jerarquía de comandos que invocan a los synths (implementación).

# Tech stack
- java 26.0.2
- maven project 
- java swing para el UI
- unit testing junit
- github source control and code repository 
- The entrypoint (`public static void main`) stays in `src/app/powerBand.java`.
- Domain classes stay in `src/app/model/`.
- Business logic stays in `src/app/logic/`.
- Packages are lowercase; classes are `PascalCase`; methods and fields are `camelCase`; constants are `UPPER_SNAKE_CASE`.
- One public top-level type per file, and the file name matches the type name.

# Specs de alto nivel para ciertas classes minimas del model

src/app/
├── powerBand.java   Main del sistema
├── util/ 
│   ├── IConstants.java
├── bridge/
│   ├── instruments/  Instrument (abstracta), GamepadInstrument, MicInstrument, TabletInstrument
│   └── commands/     SynthCommand (interfaz), PianoCommand, BassCommand, OrganCommand
├── supercollider/  SuperColliderClient (envuelve BandClient)
├── editor/         MusicEditor, Recorder, Player, CommandListener
├── storage/        Registros y manejo de archivos en disco
├── sorting/        BalancedMergeSort sobre archivos
└── exceptions/     Excepciones personalizadas
src/test/

