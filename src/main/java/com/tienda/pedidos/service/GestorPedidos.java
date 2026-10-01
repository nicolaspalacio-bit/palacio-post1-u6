package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.CalculadorDescuentoFinal;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorPedido;
import com.tienda.pedidos.validacion.ValidadorStock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Orquestador delgado: coordina las cuatro capas (validacion, descuento,
 * persistencia, notificacion) sin conocer los detalles internos de ninguna.
 *
 * <p>Tras el episodio de Golden Hammer de la Parte 2, {@code primerValidador}
 * vuelve a encadenar unicamente {@link ValidadorStock} y {@link ValidadorCliente}
 * -- los dos eslabones que realmente tienen una dependencia de orden y una
 * necesidad de corte anticipado. Las tres campanas de descuento se resuelven
 * ahora a traves de {@link CalculadorDescuentoFinal}, junto con el descuento
 * por tipo de cliente.</p>
 */
@Service
public class GestorPedidos {

    private final ValidadorPedido primerValidador;
    private final CalculadorDescuentoFinal calculadorDescuento;
    private final PedidoRepository repository;
    private final NotificacionPedidoService notificacion;
    private final JdbcTemplate jdbcTemplate;

    public GestorPedidos(ValidadorStock stock, ValidadorCliente cliente,
                          CalculadorDescuentoFinal calculadorDescuento, PedidoRepository repository,
                          NotificacionPedidoService notificacion, JdbcTemplate jdbcTemplate) {
        // encadenar(siguiente) devuelve "siguiente" (para poder seguir
        // extendiendo la cadena con .encadenar(...).encadenar(...)), asi que
        // la cabeza real de la cadena sigue siendo "stock": se asigna aparte,
        // no como resultado de la llamada a encadenar().
        stock.encadenar(cliente);
        this.primerValidador = stock;
        this.calculadorDescuento = calculadorDescuento;
        this.repository = repository;
        this.notificacion = notificacion;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {
        ContextoPedido contexto = new ContextoPedido(request);
        primerValidador.validar(contexto);
        if (contexto.isRechazado()) {
            return ResultadoPedido.rechazado(contexto.getMotivoRechazo());
        }

        double subtotal = calcularSubtotal(request);
        contexto.setSubtotal(subtotal);

        double descuento = calculadorDescuento.calcular(contexto);
        double impuesto = (subtotal - subtotal * descuento) * 0.19;
        double total = subtotal - (subtotal * descuento) + impuesto;

        Long pedidoId = repository.guardar(contexto, descuento, impuesto, total);
        notificacion.notificarConfirmacion(contexto, pedidoId, descuento, impuesto, total);
        return ResultadoPedido.confirmado(pedidoId, total);
    }

    // Unica consulta por item, sin ninguna otra logica de negocio mezclada: el
    // calculo de precio puro es lo unico que se mantiene en esta clase, porque
    // es un paso trivial de agregacion sobre el propio request, no una
    // responsabilidad completa que amerite su propio colaborador.
    private double calcularSubtotal(PedidoRequest request) {
        double subtotal = 0;
        for (ItemPedido item : request.getItems()) {
            Double precioUnitario = jdbcTemplate.queryForObject(
                "SELECT precio FROM productos WHERE id = ?", Double.class, item.getProductoId());
            subtotal += precioUnitario * item.getCantidad();
        }
        return subtotal;
    }
}