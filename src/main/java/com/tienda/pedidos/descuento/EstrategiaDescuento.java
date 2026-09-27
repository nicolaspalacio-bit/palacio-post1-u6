package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;

/**
 * Strategy: cada tipo de cliente encapsula su propia regla de descuento, sin
 * depender de un orden de evaluacion frente a las demas ni de la posibilidad
 * de "cortar" el flujo del pedido -- a diferencia de {@code ValidadorPedido},
 * aqui siempre se aplica exactamente una regla.
 */
public interface EstrategiaDescuento {

    double calcular(ContextoPedido contexto);
}
