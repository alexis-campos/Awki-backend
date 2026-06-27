package com.awki.alerta.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopSmsNotificationClient implements SmsNotificationClient {
    @Override
    public boolean enviarSms(String telefono, String mensaje) {
        log.debug("[SMS NOOP] El canal de SMS está deshabilitado. No se realiza ninguna acción.");
        return true;
    }
}
