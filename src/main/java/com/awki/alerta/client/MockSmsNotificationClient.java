package com.awki.alerta.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockSmsNotificationClient implements SmsNotificationClient {
    @Override
    public boolean enviarSms(String telefono, String mensaje) {
        log.info("[SMS MOCK] Enviando SMS al número: {} - Contenido: {}", telefono, mensaje);
        return true;
    }
}
