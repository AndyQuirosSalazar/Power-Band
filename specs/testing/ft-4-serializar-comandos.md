---
id: ft-4
layer: testing
status: approved
---

# Pruebas de serialización de comandos

## General description

Verificar que los comandos Bridge producen registros binarios de tamaño fijo con sus datos y synth correctos, sin depender de dispositivos ni de la interfaz.

## Inputs

- `BassCommand`, `PianoCommand` y `OrganCommand`.
- Tiempos relativos válidos e inválidos.

## Outputs

- Aserciones sobre los campos del `CommandRecord`, su codificación binaria y errores de validación.

## Test criteria

- Cada comando genera un `CommandRecord` del synth correcto con tiempo relativo y campos aplicables conservados.
- Cada registro se serializa a exactamente 48 bytes big-endian con los 23 bytes reservados en cero.
- Los campos no aplicables quedan en cero y el indicador mayor/menor se serializa como 0 o 1.
- Un tiempo relativo negativo se rechaza.
- Los comandos generan sus registros mediante su propia implementación, sin inspección con `instanceof`.

## Assets

- JUnit 5 (`org.junit.jupiter`), configurado en `pom.xml`.
- Clases Bridge existentes en `src/app/bridge/commands/`.
- Modelo `src/app/model/CommandRecord.java`.

## Implementation guidelines

- Ubicar pruebas bajo `src/test/app/`, reflejando el paquete probado.
- Ejecutar con Maven (`mvn test`) y usar JUnit 5.
- Comprobar la representación binaria y no depender solo de getters.

## Business workflow

1. Construir cada comando con valores conocidos y un tiempo relativo.
2. Convertirlo a `CommandRecord`.
3. Verificar los campos y bytes serializados.
4. Probar el rechazo de tiempos inválidos.
