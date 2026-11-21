---
id: ft-8
layer: testing
status: approved
---

# Pruebas de selección y reproducción

## General description

Verificar la lectura de la biblioteca, la selección UI y la reproducción temporizada de registros usando archivos temporales y dependencias simuladas.

## Inputs

- Archivos temporales con cero o varias grabaciones, índices y listas cronológicas.
- Registros válidos y datos de encabezado/rangos corruptos.
- Selecciones, cancelación y errores simulados de almacenamiento y envío.
- Reloj/programador y destino de comandos falsos.

## Outputs

- Aserciones sobre orden y contenido de la lista, registros leídos, comandos enviados, tiempos, cancelación y mensajes de error.

## Test criteria

- La lista mantiene el orden cronológico aunque el índice tenga orden lexicográfico.
- Se lee por nombre la grabación correcta y solo sus comandos delimitados por el encabezado.
- Archivos ausentes en conjunto dan una lista vacía; conjuntos parciales, rangos inválidos y registros corruptos producen errores sin modificar archivos.
- Cada ID de synth se convierte al comando Bridge correcto y todos los registros se reproducen en orden, incluyendo timestamps iguales.
- El programador falso verifica tiempos relativos sin dormir ni depender de SuperCollider, y un envío fallido produce estado de error sin continuar reportando éxito.
- El selector Swing muestra todos los nombres en orden cronológico, permite elegir una, y cancelar no invoca al reproductor.
- La UI maneja lista vacía y errores y las operaciones lentas no bloquean el EDT.

## Assets

- JUnit 5 (`org.junit.jupiter`), configurado en `pom.xml`.
- Directorios temporales, almacenamiento y reproductor simulados; no usar hardware, SuperCollider ni archivos reales del usuario.

## Implementation guidelines

- Ubicar pruebas bajo `src/test/app/`, reflejando los paquetes de `src/app/`.
- Ejecutar con Maven (`mvn test`) y usar JUnit 5.
- Ejecutar interacciones Swing en el Event Dispatch Thread.
- Usar un reloj/programador falso para verificar la línea de tiempo de forma determinista.

## Business workflow

1. Preparar archivos temporales con listas e índices en órdenes distintos.
2. Verificar listado cronológico, búsqueda por nombre, rangos de encabezado y manejo de corrupción.
3. Reproducir registros con reloj y destino de synth simulados; comprobar mapeo y tiempos.
4. Probar selector, confirmación, cancelación, biblioteca vacía y errores en el EDT.
