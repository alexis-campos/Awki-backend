package com.awki.websocket.service;

import com.awki.websocket.dto.AlertaPayload;
import com.awki.websocket.dto.RiesgoPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void enviarAlertaNueva(UUID medicoId, AlertaPayload payload) {
        messagingTemplate.convertAndSend("/topic/medico/" + medicoId + "/alertas", payload);
    }

    public void enviarRiesgoActualizado(UUID medicoId, RiesgoPayload payload) {
        messagingTemplate.convertAndSend("/topic/medico/" + medicoId + "/riesgo", payload);
    }
}
