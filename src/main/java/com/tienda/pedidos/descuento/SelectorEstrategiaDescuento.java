package com.tienda.pedidos.descuento;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Unico punto de decision: reemplaza el {@code if/else} anidado por tipo de
 * cliente por una busqueda en un mapa. Agregar un tipo de cliente nuevo con su
 * propia regla de descuento ya no exige tocar ningun condicional existente
 * (Principio Abierto/Cerrado): basta con una clase nueva y una entrada nueva
 * en este mapa.
 */
@Component
public class SelectorEstrategiaDescuento {

    private final Map<String, EstrategiaDescuento> estrategias;

    public SelectorEstrategiaDescuento(DescuentoVip vip, DescuentoFrecuente frecuente,
                                        DescuentoEstandar estandar) {
        this.estrategias = Map.of(
            "VIP", vip,
            "FRECUENTE", frecuente,
            "ESTANDAR", estandar);
    }

    public EstrategiaDescuento seleccionar(String tipoCliente) {
        return estrategias.getOrDefault(tipoCliente, estrategias.get("ESTANDAR"));
    }
}
