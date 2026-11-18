---
id: ft-4
layer: backend
status: approved
---

# Serializar comandos de synth

## General description

Representar cada comando Bridge como un `CommandRecord` de tamaño fijo, incluyendo el tiempo relativo de la grabación. Cada tipo concreto de comando construye su propio registro sin inspección con `instanceof`. Esta funcionalidad define el formato consumido por la persistencia de grabaciones de ft-6.

## Inputs

- `BassCommand`, `PianoCommand` o `OrganCommand`.
- Tiempo relativo no negativo en milisegundos desde el inicio de la grabación.

## Outputs

- `CommandRecord` con tiempo relativo, ID estable del synth, nota, duración, indicador mayor/menor, volumen y bytes reservados.
- Registro binario de comando de 48 bytes para `recordings.music`.

## Test criteria

- `SynthCommand` expone una operación como `toCommandRecord(long pRelativeTimeMillis)`; cada comando crea su registro sin `instanceof`.
- El registro serializado mide exactamente 48 bytes, en orden big-endian: tiempo (`long`, 8 bytes), ID de synth (`int`, 4), nota (`int`, 4), duración (`int`, 4), mayor (`byte`, 1), volumen (`float`, 4) y relleno reservado (23 bytes en cero).
- El registro conserva el tiempo recibido y los valores del comando correspondiente.
- Los campos no aplicables se escriben como cero; `major` solo puede ser 0 o 1.
- Un tiempo relativo negativo es rechazado.
- Los IDs de synth son constantes estables y diferentes para bajo, piano y órgano.
- `CommandRecord` ofrece los datos tipados necesarios para escribir y leer su representación binaria; no depende de la interfaz ni del almacenamiento.

## Assets

- `SynthCommand`, `BassCommand`, `PianoCommand` y `OrganCommand` existentes en `src/app/bridge/commands/`.
- `CommandRecord` en `src/app/model/CommandRecord.java`.
- Constantes de IDs y tamaños en `src/app/util/IConstants.java`.
- Formato binario consumido por `specs/database/ft-6-persistir-grabaciones.md`.

## Implementation guidelines

- Usar Java 26 y las clases estándar de Java para la representación binaria.
- Mantener la separación Bridge: cada comando concreto conoce cómo convertir sus propios campos.
- No inferir el tipo de synth con `instanceof`; usar IDs declarados como constantes.
- Colocar el modelo en `src/app/model/` y las implementaciones Bridge en `src/app/bridge/commands/`.
- Seguir las convenciones del proyecto, incluidos parámetros con prefijo `p` y un tipo público por archivo.

## Business workflow

1. La lógica de grabación proporciona al comando su tiempo relativo.
2. El comando Bridge convierte sus propios datos y el tiempo en un `CommandRecord`.
3. El registro codifica los campos aplicables, ceros para campos no aplicables y relleno cero hasta 48 bytes.
4. El almacenamiento de ft-6 puede escribir el registro al final de la grabación actual.
