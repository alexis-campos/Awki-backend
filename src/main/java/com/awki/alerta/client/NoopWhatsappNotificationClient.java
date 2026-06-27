package com.awki.alerta.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopWhatsappNotificationClient implements WhatsappNotificationClient {
    @Override
    public boolean enviarWhatsapp(String telefono, String mensaje) {
        log.debug("[WHATSAPP NOOP] El canal de WhatsApp está deshabilitado. No se realiza ninguna acción.");
        return true;
    }
}
