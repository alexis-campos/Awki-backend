package com.awki.alerta.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockPushNotificationClient implements PushNotificationClient {
    @Override
    public boolean enviarNotificacionPush(String fcmToken, String titulo, String mensaje) {
        log.info("[FCM PUSH MOCK] Enviando notificación Push a token: {} - Título: {} - Mensaje: {}", fcmToken, titulo, mensaje);
        return true;
    }
}
