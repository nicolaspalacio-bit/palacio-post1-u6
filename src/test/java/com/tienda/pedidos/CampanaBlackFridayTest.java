package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La campana Black Friday aplica un 25% fijo mientras esta activa, un
 * porcentaje mayor que el de las otras dos campanas (Corporativo 10%,
 * Volumen 12%). Si se probara en el mismo contexto que {@link GestorPedidosIntegrationTest}
 * con la bandera en true, el 25% dominaria siempre el {@code Math.max} y
 * enmascararia el resultado de las otras dos pruebas. Por eso esta campana
 * se prueba en un contexto Spring y una base de datos H2 propios, con la
 * bandera activada solo aqui.
 */
@SpringBootTest(properties = {
    "promo.black-friday.activa=true",
    "spring.datasource.url=jdbc:h2:mem:blackfridaydb;DB_CLOSE_DELAY=-1"
})
class CampanaBlackFridayTest {

    @Autowired
    private GestorPedidos gestorPedidos;

    @Test
    @DisplayName("Con la campana Black Friday activa, se aplica el 25% de descuento")
    void campanaBlackFriday_activa() {
        PedidoRequest request = new PedidoRequest(5L, "pedro.salcedo@correo.com",
            List.of(new ItemPedido(103L, 1))); // 650000

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertEquals(580_125.0, resultado.getTotal(), 0.01);
    }
}
