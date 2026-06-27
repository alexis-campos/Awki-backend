package com.awki.alerta.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopPushNotificationClient implements PushNotificationClient {
    @Override
    public boolean enviarNotificacionPush(String fcmToken, String titulo, String mensaje) {
        log.debug("[FCM PUSH NOOP] El canal de Push Firebase está deshabilitado. No se realiza ninguna acción.");
        return true;
    }
}
