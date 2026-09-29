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

## Diagnóstico — Parte 2: las tres campañas de descuento

### El escenario

Dos semanas después de cerrada la Parte 1, mercadeo solicitó tres campañas
nuevas (`BLACK_FRIDAY`, `CORPORATIVO`, `VOLUMEN`). La solución que efectivamente
se integró al proyecto —commit anterior a este— fue agregar tres eslabones
más a la cadena de validación que ya existía: `PromocionBlackFriday`,
`PromocionCorporativo` y `PromocionVolumen`, encadenados después de
`ValidadorStock` y `ValidadorCliente`.

El código compila y las tres campañas funcionan. El problema, igual que en la
Parte 1, no es funcional: es de diseño. La pregunta que separa este commit del
anterior no es *"¿funciona?"*, sino *"¿es esta la herramienta correcta para
este problema, o es la herramienta que ya conocíamos?"*

### La evidencia: ninguna de las tres campañas necesita ser un eslabón

`Chain of Responsibility` se justificó en la Parte 1 por una propiedad
concreta: **las validaciones tienen una dependencia real de orden y de corte
anticipado** —si `ValidadorStock` rechaza, `ValidadorCliente` no debe
ejecutarse—. Las tres clases nuevas no comparten esa propiedad:

| Campaña | ¿Depende del orden frente a las otras? | ¿Alguna vez rechaza el pedido? |
|---|---|---|
| `PromocionBlackFriday` | No — solo lee una bandera de configuración | No, nunca |
| `PromocionCorporativo` | No — solo depende del NIT del cliente | No, nunca |
| `PromocionVolumen` | No — solo depende del total de unidades del propio request | No, nunca |

Ninguna de las tres necesita ejecutarse *después* de otra, ninguna necesita
la posibilidad de cortar la cadena, y ninguna implementa la única operación
para la que `ValidadorPedido` fue diseñado —decidir si el pedido continúa o
se rechaza (contrato heredado de la Parte 1)—. En cambio, las tres calculan
un porcentaje a partir de datos del pedido o del cliente: exactamente la
misma forma que ya tienen `DescuentoVip` y `DescuentoFrecuente`.

Una segunda pieza de evidencia, en el propio `ContextoPedido`: el campo
`descuentoCampana` y su método `aplicarDescuentoCampana` (que se queda con
"el mayor valor recibido") existen únicamente porque tres eslabones
necesitaban escribir en un lugar compartido sin poder devolver un valor
directamente —una señal de que se está forzando una responsabilidad de
*cálculo* dentro de un mecanismo pensado para *validar y cortar el flujo*.

### Diagnóstico: Golden Hammer

Esto es Golden Hammer, no un tercer God Object ni más Spaghetti Code: la
solución no se evaluó contra el problema nuevo, se reutilizó porque **ya
funcionó la vez anterior** ("los eslabones ya sabían cómo conectarse entre
sí" es, literalmente, la única justificación que motivó agregarlos así). El
costo no es visible de inmediato —el sistema funciona— sino que aparece en
el momento en que alguien intenta razonar sobre `ValidadorPedido`: una clase
cuyo nombre y contrato prometen "decidir si el pedido continúa" termina
conteniendo tres implementaciones que nunca deciden nada de eso.


