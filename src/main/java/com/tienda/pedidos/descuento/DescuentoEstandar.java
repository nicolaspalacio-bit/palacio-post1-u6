package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

/**
 * Estrategia por defecto para cualquier tipo de cliente sin una regla de
 * descuento propia (por ejemplo, ESTANDAR). Existir como clase explicita, en
 * lugar de como un {@code null} manejado con un condicional adicional, es lo
 * que permite que {@link SelectorEstrategiaDescuento} no necesite ninguna
 * rama especial para "el caso sin descuento".
 */
@Component
public class DescuentoEstandar implements EstrategiaDescuento {

    @Override
    public double calcular(ContextoPedido contexto) {
        return 0.0;
    }
}
