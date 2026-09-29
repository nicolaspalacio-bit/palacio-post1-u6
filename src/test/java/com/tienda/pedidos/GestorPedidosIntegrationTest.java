package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Suite de regresion para {@link GestorPedidos}. Se escribe ANTES de refactorizar
 * (Paso 2 del post-contenido) y debe seguir pasando sin modificaciones despues de
 * cada refactorizacion, ya que verifica el comportamiento observable del sistema
 * (aceptar/rechazar, descuento, total) y no su implementacion interna.
 *
 * <p>Los datos de referencia se cargan desde {@code data.sql} (ver el comentario
 * de cabecera de ese archivo para el rol de cada cliente de prueba).</p>
 */
@SpringBootTest
class GestorPedidosIntegrationTest {

    @Autowired
    private GestorPedidos gestorPedidos;

    private static final double DELTA = 0.01;

    // ---------------------------------------------------------------------
    // Rutas de validacion
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Rechaza el pedido cuando el stock disponible es menor a la cantidad solicitada")
    void rechazaPorStockInsuficiente() {
        PedidoRequest request = new PedidoRequest(5L, "pedro.salcedo@correo.com",
            List.of(new ItemPedido(105L, 5))); // stock inicial del producto 105 es 2

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertFalse(resultado.isConfirmado());
        assertTrue(resultado.getMotivoRechazo().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("Rechaza el pedido cuando el cliente no existe")
    void rechazaPorClienteInexistente() {
        PedidoRequest request = new PedidoRequest(9999L, "desconocido@correo.com",
            List.of(new ItemPedido(102L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertFalse(resultado.isConfirmado());
        assertEquals("Cliente no registrado", resultado.getMotivoRechazo());
    }

    @Test
    @DisplayName("Cliente moroso con deuda pendiente: el resultado depende del horario de corte (20:00)")
    void clienteMorosoConDeuda_dependeDelHorarioDeCorte() {
        // El metodo original consulta LocalTime.now() directamente (no es inyectable), asi
        // que esta prueba se adapta a la hora real de ejecucion en lugar de mockearla: solo
        // una de las dos ramas de abajo se ejecuta segun el reloj de la maquina que corre
        // la suite; la otra queda documentada, no eliminada.
        PedidoRequest request = new PedidoRequest(4L, "marcela.duque@correo.com",
            List.of(new ItemPedido(102L, 1)));
        boolean antesDelCorte = LocalTime.now().isBefore(LocalTime.of(20, 0));

        if (antesDelCorte) {
            ResultadoPedido resultado = gestorPedidos.procesarPedido(request);
            assertFalse(resultado.isConfirmado());
            assertTrue(resultado.getMotivoRechazo().contains("deuda pendiente"));
        } else {
            ResultadoPedido resultado = gestorPedidos.procesarPedido(request);
            assertTrue(resultado.isConfirmado(),
                "Fuera del horario de corte, el pedido del cliente moroso debe aceptarse excepcionalmente");
        }
    }

    @Test
    @DisplayName("Cliente moroso sin deuda pendiente (ya saldada) no se rechaza por mora")
    void clienteMorosoSinDeudaPendiente_seProcesaNormalmente() {
        PedidoRequest request = new PedidoRequest(6L, "camila.rojas@correo.com",
            List.of(new ItemPedido(102L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
    }

    // ---------------------------------------------------------------------
    // Reglas de descuento
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Cliente VIP con subtotal superior a 1.000.000: descuento del 15%")
    void clienteVip_descuentoQuinceParaSubtotalAlto() {
        PedidoRequest request = new PedidoRequest(1L, "laura.restrepo@correo.com",
            List.of(new ItemPedido(104L, 2))); // 900000 x 2 = 1800000

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertEquals(1_820_700.0, resultado.getTotal(), DELTA);
    }

    @Test
    @DisplayName("Cliente VIP con subtotal bajo: descuento del 5%")
    void clienteVip_descuentoCincoParaSubtotalBajo() {
        PedidoRequest request = new PedidoRequest(1L, "laura.restrepo@correo.com",
            List.of(new ItemPedido(102L, 1))); // 80000

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertEquals(90_440.0, resultado.getTotal(), DELTA);
    }

    @Test
    @DisplayName("Cliente FRECUENTE con mas de 10 pedidos previos: descuento del 8%")
    void clienteFrecuente_masDeDiezPedidos_descuentoOcho() {
        PedidoRequest request = new PedidoRequest(2L, "comercial.andina@correo.com",
            List.of(new ItemPedido(101L, 1))); // 150000

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertEquals(164_220.0, resultado.getTotal(), DELTA);
    }

    @Test
    @DisplayName("Cliente FRECUENTE con 4 a 10 pedidos previos: descuento del 4%")
    void clienteFrecuente_entreCuatroYDiezPedidos_descuentoCuatro() {
        PedidoRequest request = new PedidoRequest(3L, "julian.ospina@correo.com",
            List.of(new ItemPedido(101L, 1))); // 150000

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertEquals(171_360.0, resultado.getTotal(), DELTA);
    }

    @Test
    @DisplayName("Cliente ESTANDAR sin ninguna regla de descuento aplicable")
    void clienteEstandar_sinDescuento() {
        PedidoRequest request = new PedidoRequest(5L, "pedro.salcedo@correo.com",
            List.of(new ItemPedido(102L, 1))); // 80000

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertEquals(95_200.0, resultado.getTotal(), DELTA);
    }

    // ---------------------------------------------------------------------
    // Parte 2 — campanas de descuento (Black Friday se prueba aparte, en
    // CampanaBlackFridayTest, con su propio contexto y la bandera activa;
    // en este contexto la bandera permanece en false, por lo que Corporativo
    // y Volumen se observan sin que Black Friday los enmascare).
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Cliente con NIT registrado recibe el descuento de campana CORPORATIVO")
    void campanaCorporativo_clienteConNit() {
        PedidoRequest request = new PedidoRequest(7L, "compras@distribucionesandina.com",
            List.of(new ItemPedido(103L, 1))); // 650000

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertEquals(696_150.0, resultado.getTotal(), DELTA);
    }

    @Test
    @DisplayName("Pedido con mas de 20 unidades recibe el descuento de campana VOLUMEN")
    void campanaVolumen_masDeVeinteUnidades() {
        PedidoRequest request = new PedidoRequest(5L, "pedro.salcedo@correo.com",
            List.of(new ItemPedido(102L, 25))); // 80000 x 25 = 2000000

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertEquals(2_094_400.0, resultado.getTotal(), DELTA);
    }
}
