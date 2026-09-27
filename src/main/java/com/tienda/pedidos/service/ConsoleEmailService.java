package com.tienda.pedidos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementacion de {@link EmailService} que imprime el correo en el log en
 * lugar de enviarlo por SMTP, para poder ejecutar el sistema y las pruebas
 * sin depender de un servidor de correo real.
 */
@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        log.info("----- Correo simulado -----");
        log.info("Para: {}", destinatario);
        log.info("Asunto: {}", asunto);
        log.info("Cuerpo:\n{}", cuerpo);
        log.info("----------------------------");
    }
}
