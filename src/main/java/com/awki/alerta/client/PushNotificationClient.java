package com.awki.alerta.client;

public interface PushNotificationClient {
    boolean enviarNotificacionPush(String fcmToken, String titulo, String mensaje);
}
