package com.tienda.pedidos.validacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Segundo eslabon: existencia del cliente y su situacion de mora, con la misma
 * excepcion por horario de corte que tenia el metodo original. Depende de que
 * {@link ValidadorStock} ya haya pasado (Chain of Responsibility corta la cadena
 * antes de llegar aqui si el stock ya fue insuficiente).
 */
@Component
public class ValidadorCliente extends ValidadorPedido {

    private static final LocalTime HORA_DE_CORTE = LocalTime.of(20, 0);

    private final JdbcTemplate jdbcTemplate;

    public ValidadorCliente(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        Long clienteId = contexto.getRequest().getClienteId();
        String tipo = jdbcTemplate.queryForObject(
            "SELECT tipo_cliente FROM clientes WHERE id = ?", String.class, clienteId);
        if (tipo == null) {
            contexto.rechazar("Cliente no registrado");
            return;
        }
        contexto.setTipoCliente(tipo);

        if (tipo.equals("MOROSO")) {
            Double deuda = jdbcTemplate.queryForObject(
                "SELECT SUM(monto) FROM facturas WHERE cliente_id = ? AND pagada = false",
                Double.class, clienteId);
            boolean dentroDelHorarioDeCorte = LocalTime.now().isBefore(HORA_DE_CORTE);
            if (deuda != null && deuda > 0 && dentroDelHorarioDeCorte) {
                contexto.rechazar("Cliente con deuda pendiente: $" + deuda);
            }
        }
    }
}
