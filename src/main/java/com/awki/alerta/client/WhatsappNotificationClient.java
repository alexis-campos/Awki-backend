package com.awki.alerta.client;

public interface WhatsappNotificationClient {
    boolean enviarWhatsapp(String telefono, String mensaje);
}
