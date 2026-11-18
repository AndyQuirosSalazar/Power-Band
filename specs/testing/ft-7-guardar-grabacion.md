---
id: ft-7
layer: testing
status: draft
---

# Pruebas de la interfaz para guardar

## General description

Verificar el diálogo de nombre, sus validaciones y la interacción con los resultados de persistencia de ft-6, usando dependencias simuladas.

## Inputs

- Eventos simulados de Guardar y Cancelar.
- Nombres válidos, vacíos, no ASCII, largos y duplicados.
- Resultados simulados de búsqueda, almacenamiento y detección de archivos parciales.

## Outputs

- Aserciones sobre mensajes, conservación de la grabación, decisión de inicialización y archivos no modificados al cancelar.

## Test criteria

- El diálogo permite Guardar o Cancelar una grabación completada.
- Nombres vacíos, no ASCII, mayores de 31 caracteres o duplicados producen advertencia y no descartan la grabación.
- La comparación de duplicados distingue mayúsculas y los espacios externos se eliminan antes de validar.
- Cancelar el guardado no solicita ni confirma una escritura.
- Al presentarse archivos parciales, se muestran las opciones de reiniciar, crear faltantes o cancelar; cancelar no invoca una modificación.
- El éxito solo se muestra si el almacenamiento confirma que terminó.
- Los errores de almacenamiento se muestran sin bloquear el Event Dispatch Thread.

## Assets

- JUnit 5 (`org.junit.jupiter`), configurado en `pom.xml`.
- Dobles de prueba para la interfaz de almacenamiento de ft-6.

## Implementation guidelines

- Ubicar pruebas bajo `src/test/app/ui/`.
- Ejecutar con Maven (`mvn test`) y usar JUnit 5.
- Ejecutar las interacciones Swing en el Event Dispatch Thread.
- Usar almacenamiento simulado; no depender de archivos reales ni hardware.

## Business workflow

1. Abrir el diálogo con una grabación de prueba completada.
2. Probar nombres inválidos, existentes y válidos; comprobar advertencias y conservación de la grabación.
3. Probar Guardar, Cancelar y las decisiones de inicialización parcial.
4. Comprobar que solo un resultado satisfactorio del almacenamiento muestra confirmación.
