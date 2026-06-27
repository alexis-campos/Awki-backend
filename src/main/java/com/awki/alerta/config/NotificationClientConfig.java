package com.awki.alerta.config;

import com.awki.alerta.client.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationClientConfig {

    @Bean
    public SmsNotificationClient smsNotificationClient(NotificacionesProperties props) {
        if (props.isMockEnabled()) {
            return new MockSmsNotificationClient();
        }
        if (props.getTwilio().isEnabled()) {
            // Preparado para Twilio Real
            return new MockSmsNotificationClient();
        }
        return new NoopSmsNotificationClient();
    }

    @Bean
    public WhatsappNotificationClient whatsappNotificationClient(NotificacionesProperties props) {
        if (props.isMockEnabled()) {
            return new MockWhatsappNotificationClient();
        }
        if (props.getWhatsapp().isEnabled()) {
            // Preparado para WhatsApp Real
            return new MockWhatsappNotificationClient();
        }
        return new NoopWhatsappNotificationClient();
    }

    @Bean
    public PushNotificationClient pushNotificationClient(NotificacionesProperties props) {
        if (props.isMockEnabled()) {
            return new MockPushNotificationClient();
        }
        if (props.getFirebase().isEnabled()) {
            // Preparado para FCM Real
            return new MockPushNotificationClient();
        }
        return new NoopPushNotificationClient();
    }
}
