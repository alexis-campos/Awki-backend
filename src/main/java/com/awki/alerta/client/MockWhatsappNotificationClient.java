package com.awki.alerta.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockWhatsappNotificationClient implements WhatsappNotificationClient {
    @Override
    public boolean enviarWhatsapp(String telefono, String mensaje) {
        log.info("[WHATSAPP MOCK] Enviando WhatsApp al número: {} - Contenido: {}", telefono, mensaje);
        return true;
    }
}
