package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.PedidoRequest;

/**
 * Contexto mutable que viaja a traves de la cadena de validacion. Cada eslabon
 * lee lo que necesita del pedido original y escribe en este objeto el resultado
 * de su propia verificacion (tipo de cliente resuelto, subtotal calculado, o el
 * rechazo con su motivo), sin que los eslabones se conozcan entre si.
 */
public class ContextoPedido {

    private final PedidoRequest request;
    private String tipoCliente;
    private double subtotal;
    private boolean rechazado = false;
    private String motivoRechazo;
    private double descuentoCampana = 0;

    public ContextoPedido(PedidoRequest request) {
        this.request = request;
    }

    public PedidoRequest getRequest() {
        return request;
    }

    public String getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(String tipoCliente) {
        this.tipoCliente = tipoCliente;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public boolean isRechazado() {
        return rechazado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void rechazar(String motivo) {
        this.rechazado = true;
        this.motivoRechazo = motivo;
    }

    // Campo agregado para que los nuevos eslabones de campana (Parte 2) puedan
    // escribir un descuento sin pasar por ningun EstrategiaDescuento existente.
    public double getDescuentoCampana() {
        return descuentoCampana;
    }

    public void aplicarDescuentoCampana(double valor) {
        if (valor > this.descuentoCampana) {
            this.descuentoCampana = valor; // el mayor descuento de campana gana
        }
    }
}
