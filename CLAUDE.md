# Project description


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

