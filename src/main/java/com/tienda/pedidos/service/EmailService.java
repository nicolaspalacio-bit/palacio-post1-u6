package com.tienda.pedidos.service;

/**
 * Puerto de salida para el envio de notificaciones por correo. La implementacion
 * real (SMTP, proveedor transaccional, etc.) queda fuera del alcance de este
 * laboratorio; se usa una implementacion de consola para no depender de un
 * servidor de correo externo durante el desarrollo y las pruebas.
 */
public interface EmailService {

    void enviar(String destinatario, String asunto, String cuerpo);
}
