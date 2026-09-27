package com.tienda.pedidos.validacion;

/**
 * Eslabon abstracto de la cadena de responsabilidad. Cada subclase concentra
 * una unica verificacion y decide, a traves de {@link ContextoPedido#rechazar},
 * si el pedido puede continuar hacia el siguiente eslabon. El metodo
 * {@link #validar(ContextoPedido)} es quien aplica el corte anticipado: si un
 * eslabon rechaza el pedido, los siguientes ni siquiera se ejecutan.
 */
public abstract class ValidadorPedido {

    private ValidadorPedido siguiente;

    public ValidadorPedido encadenar(ValidadorPedido siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void validar(ContextoPedido contexto) {
        ejecutar(contexto);
        if (!contexto.isRechazado() && siguiente != null) {
            siguiente.validar(contexto);
        }
    }

    protected abstract void ejecutar(ContextoPedido contexto);
}
