package com.tienda.pedidos.validacion;

import org.springframework.stereotype.Component;

// Nuevo eslabon: volumen -- necesita el total de unidades del pedido, que el
// contexto todavia no expone, asi que lo recalcula el mismo desde el request.
@Component
public class PromocionVolumen extends ValidadorPedido {

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        int totalUnidades = contexto.getRequest().getItems().stream()
            .mapToInt(item -> item.getCantidad())
            .sum();
        if (totalUnidades > 20) {
            contexto.aplicarDescuentoCampana(0.12);
        }
    }
}
