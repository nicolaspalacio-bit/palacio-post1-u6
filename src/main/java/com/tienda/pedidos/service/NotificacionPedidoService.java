package com.tienda.pedidos.service;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Unica responsable de construir y enviar la notificacion de confirmacion.
 * Extraida del bloque de notificacion que antes vivia embebido en
 * {@code GestorPedidos.procesarPedido} (lineas 120-138 del commit original).
 * El manejo de errores conserva el mismo criterio que tenia el metodo
 * original: una falla en el envio se registra, pero no revierte el pedido
 * ya confirmado y persistido.
 */
@Service
public class NotificacionPedidoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionPedidoService.class);

    private final EmailService emailService;

    public NotificacionPedidoService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void notificarConfirmacion(ContextoPedido contexto, Long pedidoId, double descuento,
                                       double impuesto, double total) {
        String asunto = "Confirmacion de pedido #" + pedidoId;
        String cuerpo = construirCuerpo(contexto, descuento, impuesto, total);
        try {
            emailService.enviar(contexto.getRequest().getClienteEmail(), asunto, cuerpo);
        } catch (Exception e) {
            log.error("No se pudo enviar la notificacion del pedido {}: {}", pedidoId, e.getMessage());
        }
    }

    private String construirCuerpo(ContextoPedido contexto, double descuento, double impuesto, double total) {
        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Estimado cliente,\n\n").append("Su pedido ha sido confirmado.\n");
        cuerpo.append("Subtotal: $").append(contexto.getSubtotal()).append("\n");
        if (descuento > 0) {
            cuerpo.append("Descuento aplicado: ").append((int) (descuento * 100)).append("%\n");
        }
        cuerpo.append("Impuesto: $").append(impuesto).append("\n").append("Total: $").append(total).append("\n");
        return cuerpo.toString();
    }
}
