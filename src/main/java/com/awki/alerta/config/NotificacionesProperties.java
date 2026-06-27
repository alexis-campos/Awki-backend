package com.awki.alerta.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "notifications")
public class NotificacionesProperties {
    private boolean mockEnabled = true;
    private TwilioProps twilio = new TwilioProps();
    private WhatsappProps whatsapp = new WhatsappProps();
    private FirebaseProps firebase = new FirebaseProps();

    @Getter @Setter
    public static class TwilioProps {
        private boolean enabled = false;
        private String accountSid = "mock-sid";
        private String authToken = "mock-token";
        private String fromNumber = "+000000000";
        private int timeoutSeconds = 10;
    }

    @Getter @Setter
    public static class WhatsappProps {
        private boolean enabled = false;
        private String token = "mock-token";
        private String phoneNumberId = "mock-phone-id";
        private int timeoutSeconds = 10;
    }

    @Getter @Setter
    public static class FirebaseProps {
        private boolean enabled = false;
        private String credentialsPath = "";
        private int timeoutSeconds = 10;
    }
}
