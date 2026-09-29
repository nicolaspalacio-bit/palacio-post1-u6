package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Combina el descuento por tipo de cliente ({@link SelectorEstrategiaDescuento})
 * con el mayor descuento de campana activo, tomando el maximo entre ambos --
 * la misma regla de negocio que antes aplicaba la cadena a traves de
 * {@code ContextoPedido.aplicarDescuentoCampana}, pero ahora sin escribir en
 * un campo mutable compartido desde clases que no son responsables de validar
 * nada.
 */
@Component
public class CalculadorDescuentoFinal {

    private final SelectorEstrategiaDescuento selectorPorCliente;
    private final List<EstrategiaDescuento> campanas;

    public CalculadorDescuentoFinal(SelectorEstrategiaDescuento selectorPorCliente,
                                     DescuentoBlackFriday blackFriday, DescuentoCorporativo corporativo,
                                     DescuentoVolumen volumen) {
        this.selectorPorCliente = selectorPorCliente;
        this.campanas = List.of(blackFriday, corporativo, volumen);
    }

    public double calcular(ContextoPedido contexto) {
        double porTipoCliente = selectorPorCliente.seleccionar(contexto.getTipoCliente()).calcular(contexto);
        double porCampana = campanas.stream()
            .mapToDouble(estrategia -> estrategia.calcular(contexto))
            .max()
            .orElse(0.0);
        return Math.max(porTipoCliente, porCampana);
    }
}
