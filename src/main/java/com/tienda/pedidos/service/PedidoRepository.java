package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Unica responsable de la persistencia de un pedido confirmado. Extraida del
 * bloque de persistencia que antes vivia embebido en
 * {@code GestorPedidos.procesarPedido} (lineas 104-118 del commit original),
 * sin cambiar ninguna de las sentencias SQL de INSERT/UPDATE.
 *
 * <p>Una diferencia sí deliberada frente a la version original: en lugar de
 * recuperar el id generado con {@code CALL IDENTITY()} (una funcion que H2
 * 2.x ya no reconoce fuera del modo de compatibilidad LEGACY -- se comprobo
 * al ejecutar la suite de pruebas con Maven, que es justamente donde este
 * tipo de problema de integracion se hace visible), se usa el mecanismo
 * estandar de Spring JDBC ({@link GeneratedKeyHolder}), que no depende de
 * ninguna funcion propietaria del motor de base de datos.</p>
 */
@Repository
public class PedidoRepository {

    private final JdbcTemplate jdbcTemplate;

    public PedidoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long guardar(ContextoPedido contexto, double descuento, double impuesto, double total) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, contexto.getRequest().getClienteId());
            ps.setDouble(2, contexto.getSubtotal());
            ps.setDouble(3, descuento);
            ps.setDouble(4, impuesto);
            ps.setDouble(5, total);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(7, "CONFIRMADO");
            return ps;
        }, keyHolder);
        Long pedidoId = keyHolder.getKey().longValue();

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