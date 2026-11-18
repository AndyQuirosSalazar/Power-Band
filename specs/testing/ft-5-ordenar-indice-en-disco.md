---
id: ft-5
layer: testing
status: approved
---

# Pruebas de ordenación externa del índice

## General description

Comprobar el ordenamiento de registros fijos por Balanced Merge Sort usando archivos temporales y un conjunto de datos suficiente para ejercitar varias pasadas.

## Inputs

- Archivos temporales de índice con registros de 40 bytes en distintos órdenes.
- Nombres con mayúsculas y minúsculas y offsets distintos.
- Archivos completos y archivos con tamaño corrupto.

## Outputs

- Aserciones sobre el orden final, contenido, tamaño, archivos temporales y errores.

## Test criteria

- El índice vacío, unitario y con registros ordenados, invertidos o mezclados produce el resultado esperado.
- La comparación es lexicográfica ASCII y sensible a mayúsculas.
- Tras ordenar, cada nombre mantiene asociado su offset original y el conjunto de entradas no cambia.
- Una prueba con más entradas que una corrida fuerza múltiples pasadas con archivos auxiliares en disco.
- El algoritmo no depende de cargar todas las entradas a memoria.
- Un archivo cuyo tamaño no sea múltiplo de 40 bytes se rechaza sin modificarlo.
- Los auxiliares se eliminan al terminar con éxito y al fallar una operación.

## Assets

- JUnit 5 (`org.junit.jupiter`), configurado en `pom.xml`.
- Directorios temporales por prueba; no usar el índice real del usuario.

## Implementation guidelines

- Ubicar las pruebas bajo `src/test/app/sorting/`.
- Ejecutar con Maven (`mvn test`) y usar JUnit 5.
- Inspeccionar bytes y orden en el archivo resultante.
- Preparar datos en disco sin usar una colección completa como sustituto del algoritmo bajo prueba.

## Business workflow

1. Crear un índice temporal con entradas en el orden de prueba.
2. Ejecutar Balanced Merge Sort.
3. Leer los registros del archivo y comprobar orden, contenido y offsets.
4. Probar archivo corrupto y limpieza de temporales tras éxito y error.
