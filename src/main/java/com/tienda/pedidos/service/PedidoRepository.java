package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Unica responsable de la persistencia de un pedido confirmado. Extraida tal
 * cual del bloque de persistencia que antes vivia embebido en
 * {@code GestorPedidos.procesarPedido} (lineas 104-118 del commit original),
 * sin cambiar ninguna de las sentencias SQL.
 */
@Repository
public class PedidoRepository {

    private final JdbcTemplate jdbcTemplate;

    public PedidoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long guardar(ContextoPedido contexto, double descuento, double impuesto, double total) {
        jdbcTemplate.update(
            "INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            contexto.getRequest().getClienteId(), contexto.getSubtotal(), descuento, impuesto, total,
            Timestamp.valueOf(LocalDateTime.now()), "CONFIRMADO");
        Long pedidoId = jdbcTemplate.queryForObject("CALL IDENTITY()", Long.class);

        for (ItemPedido item : contexto.getRequest().getItems()) {
            jdbcTemplate.update(
                "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad) VALUES (?, ?, ?)",
                pedidoId, item.getProductoId(), item.getCantidad());
            jdbcTemplate.update(
                "UPDATE inventario SET stock = stock - ? WHERE producto_id = ?",
                item.getCantidad(), item.getProductoId());
        }
        return pedidoId;
    }
}
