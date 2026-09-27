# Post-contenido — Unidad 6: Antipatrones de Diseño

Repositorio del post-contenido de la Unidad 6 de *Patrones de Diseño de Software*.
Contiene un único proyecto Spring Boot (`pedidos-service/`) en el que se diagnostica
y corrige, con evidencia extraída directamente del código, un antipatrón combinado
en la clase `GestorPedidos` y, más adelante, un segundo antipatrón distinto que
aparece al hacer crecer ese mismo sistema.

Este documento se escribe de forma incremental, en el mismo orden en que ocurrió
el trabajo: primero el diagnóstico, después la corrección. Este commit corresponde
únicamente al diagnóstico de la Parte 1.

---

## Diagnóstico — Parte 1: `GestorPedidos`

### El síntoma de fondo

`GestorPedidos.procesarPedido(PedidoRequest)` es, a la fecha de este commit, el
**único método público de la clase**, y sin embargo concentra seis responsabilidades
que no tienen ninguna razón estructural para compartir un mismo método:

| # | Responsabilidad                          | Líneas      | Evidencia |
|---|-------------------------------------------|-------------|-----------|
| 1 | Validación de stock                       | 34–46       | Consulta SQL embebida (`SELECT stock FROM inventario...`) dentro de un `for` que recorre los ítems del pedido. |
| 2 | Validación de cliente y mora               | 49–68       | Dos consultas SQL más, con una regla de negocio (excepción por horario de corte, línea 60) mezclada en medio de la validación. |
| 3 | Cálculo de subtotal                        | 71–76       | Una consulta SQL **por cada ítem**, dentro del propio cálculo del precio. |
| 4 | Cálculo de descuento                       | 79–96       | Condicional anidado a dos niveles (tipo de cliente → rango de monto o número de pedidos previos), con una tercera consulta SQL embebida en la línea 89. |
| 5 | Persistencia directa vía JDBC              | 104–118     | Tres sentencias `INSERT`/`UPDATE` ejecutadas directamente contra `JdbcTemplate`, sin repositorio ni transacción explícita. |
| 6 | Notificación                               | 121–138     | Construcción manual del cuerpo del correo con `StringBuilder`, entremezclada con el envío y su manejo de errores. |

Es decir: **114 líneas** (29–142) de un único método público que pasa, sin ninguna
frontera, por cuatro capas de abstracción distintas —reglas de negocio, acceso a
datos, formato de texto y orquestación— todo al mismo nivel de anidamiento.

### No es solo un método largo: es una clase que ya "sabe demasiado"

El archivo no termina en la línea 142. Contiene además **seis métodos privados**
(`purgarPedidosVencidos`, `obtenerHistorialCliente`, `calcularImpuestoRegional`,
`formatearFactura`, `construirCuerpoCorreo`, `reintentarNotificacion`; líneas
152–211) que **procesarPedido invoca**, pero que no tienen relación con procesar
*un* pedido: limpieza programada de registros vencidos, historial de cliente para
un log de depuración, una segunda fórmula de impuesto que nunca se usa para cobrar
nada, una segunda implementación del cuerpo del correo que compite con la que ya
existe en línea, y una lógica de reintento recursiva. Dos observaciones adicionales,
citadas directamente del código:

- `purgarPedidosVencidos` (línea 158) borra pedidos en estado `PENDIENTE`, un
  estado que **el propio sistema nunca asigna** —todos los pedidos se insertan
  directamente como `CONFIRMADO` (línea 108)—, así que ejecuta una consulta contra
  la base de datos en cada pedido procesado para borrar sistemáticamente cero
  filas.
- `calcularImpuestoRegional` (línea 169) duplica, con una tasa parametrizable, el
  mismo 19 % que `procesarPedido` ya calcula de forma fija en la línea 98; conviven
  dos fuentes de verdad para el mismo número.

Esto es evidencia de que el problema no es únicamente un método largo: es una
clase que, con el tiempo, se convirtió en el lugar donde cualquier cosa remotamente
relacionada con "pedidos" terminó viviendo, tuviera o no relación con el flujo de
un pedido individual.

### Diagnóstico: God Object y Spaghetti Code combinados

Con la evidencia anterior, `GestorPedidos` presenta **dos antipatrones distintos y
simultáneos**, no uno solo:

- **God Object.** La clase viola directamente el Principio de Responsabilidad
  Única: valida, calcula, persiste, notifica y, además, hace limpieza de datos y
  mantiene una segunda ruta de cálculo/formato que nadie usa. No hay una única
  razón para que esta clase cambie; hay al menos seis.
- **Spaghetti Code.** El cálculo de descuento (líneas 79–96) anida condicionales
  según el tipo de cliente y, dentro de cada rama, según el monto o el número de
  pedidos previos, con una consulta SQL intercalada en medio de esa ramificación.
  Contando únicamente los puntos de decisión de `procesarPedido` (sin contar los
  operadores `&&`), el método tiene aproximadamente **16 caminos independientes**
  —una complejidad ciclomática cercana a 17—, muy por encima de un método que se
  pueda leer y probar con confianza.

### Qué se necesitaría para agregar un nuevo tipo de cliente hoy

Como ejercicio de diagnóstico: agregar un tipo de cliente nuevo con su propia
regla de descuento hoy exigiría (a) añadir una rama más al `if/else` de las
líneas 80–96, (b) sin tocar el resto del método, porque todo comparte el mismo
bloque; y como ese bloque no está cubierto por ninguna prueba unitaria aislada
—solo por pruebas de extremo a extremo sobre `procesarPedido` completo—, no hay
forma de verificar la nueva rama sin ejercitar también la validación de stock,
la validación de mora y la persistencia completa. Esa dependencia innecesaria
entre partes que no deberían conocerse es, en sí misma, el costo concreto del
antipatrón.

---

*(Este README se completa progresivamente: la sección "Decisiones de diseño" con
el patrón aplicado y las alternativas descartadas, el diagnóstico de la Parte 2 y
las conclusiones se agregan en commits posteriores, según avanza el trabajo.)*
