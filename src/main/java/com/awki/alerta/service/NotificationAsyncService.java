package com.awki.alerta.service;

import com.awki.alerta.client.PushNotificationClient;
import com.awki.alerta.client.SmsNotificationClient;
import com.awki.alerta.client.WhatsappNotificationClient;
import com.awki.alerta.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAsyncService {

    private final SmsNotificationClient smsClient;
    private final WhatsappNotificationClient whatsappClient;
    private final PushNotificationClient pushClient;
    private final AlertaRepository alertaRepository;
    private final NotificacionesOrchestrator orchestrator;

    @Async("taskExecutor")
    public void notificarAlertaAsync(UUID alertaId) {
        log.info("[Async] Iniciando orquestación de notificaciones para la alerta: {}", alertaId);
        try {
            orchestrator.orquestarNotificaciones(alertaId);
        } catch (Exception e) {
            log.error("[Async] Error durante la orquestación de la alerta: {}", alertaId, e);
        }
    }

    @Async("taskExecutor")
    public void enviarSmsAsync(String telefono, String mensaje, UUID alertaId) {
        log.info("[Async] Enviando SMS asíncrono a {}. Alerta: {}", telefono, alertaId);
        try {
            boolean success = smsClient.enviarSms(telefono, mensaje);
            if (success && alertaId != null) {
                alertaRepository.findById(alertaId).ifPresent(alerta -> {
                    alerta.setSmsEnviado(true);
                    alerta.setIntentosSms(alerta.getIntentosSms() + 1);
                    alertaRepository.save(alerta);
                });
            }
        } catch (Exception e) {
            log.error("[Async] Error al enviar SMS a {}. Alerta: {}", telefono, alertaId, e);
            if (alertaId != null) {
                alertaRepository.findById(alertaId).ifPresent(alerta -> {
                    alerta.setIntentosSms(alerta.getIntentosSms() + 1);
                    alertaRepository.save(alerta);
                });
            }
        }
    }

    @Async("taskExecutor")
    public void enviarWhatsappAsync(String telefono, String mensaje, UUID alertaId) {
        log.info("[Async] Enviando WhatsApp asíncrono a {}. Alerta: {}", telefono, alertaId);
        try {
            boolean success = whatsappClient.enviarWhatsapp(telefono, mensaje);
            if (success && alertaId != null) {
                alertaRepository.findById(alertaId).ifPresent(alerta -> {
                    alerta.setWhatsappEnviado(true);
                    alerta.setIntentosWhatsapp(alerta.getIntentosWhatsapp() + 1);
                    alertaRepository.save(alerta);
                });
            }
        } catch (Exception e) {
            log.error("[Async] Error al enviar WhatsApp a {}. Alerta: {}", telefono, alertaId, e);
            if (alertaId != null) {
                alertaRepository.findById(alertaId).ifPresent(alerta -> {
                    alerta.setIntentosWhatsapp(alerta.getIntentosWhatsapp() + 1);
                    alertaRepository.save(alerta);
                });
            }
        }
    }

    @Async("taskExecutor")
    public void enviarFcmAsync(String fcmToken, String titulo, String body, UUID alertaId) {
        log.info("[Async] Enviando FCM asíncrono. Alerta: {}", alertaId);
        try {
            boolean success = pushClient.enviarNotificacionPush(fcmToken, titulo, body);
            if (success && alertaId != null) {
                alertaRepository.findById(alertaId).ifPresent(alerta -> {
                    alerta.setFcmEnviado(true);
                    alerta.setIntentosFcm(alerta.getIntentosFcm() + 1);
                    alertaRepository.save(alerta);
                });
            }
        } catch (Exception e) {
            log.error("[Async] Error al enviar FCM. Alerta: {}", alertaId, e);
            if (alertaId != null) {
                alertaRepository.findById(alertaId).ifPresent(alerta -> {
                    alerta.setIntentosFcm(alerta.getIntentosFcm() + 1);
                    alertaRepository.save(alerta);
                });
            }
        }
    }
}
