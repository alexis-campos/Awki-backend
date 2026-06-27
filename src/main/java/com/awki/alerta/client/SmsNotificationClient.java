package com.awki.alerta.client;

public interface SmsNotificationClient {
    boolean enviarSms(String telefono, String mensaje);
}
