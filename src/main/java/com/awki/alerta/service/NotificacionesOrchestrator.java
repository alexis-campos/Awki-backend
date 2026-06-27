package com.awki.alerta.service;

import com.awki.alerta.client.PushNotificationClient;
import com.awki.alerta.client.SmsNotificationClient;
import com.awki.alerta.client.WhatsappNotificationClient;
import com.awki.alerta.entity.Alerta;
import com.awki.alerta.entity.CanalPreferido;
import com.awki.alerta.entity.ContactoEmergencia;
import com.awki.alerta.entity.DispositivoMedico;
import com.awki.alerta.entity.EstadoEntrega;
import com.awki.alerta.repository.AlertaRepository;
import com.awki.alerta.repository.ContactoEmergenciaRepository;
import com.awki.alerta.repository.DispositivoMedicoRepository;
import com.awki.websocket.dto.AlertaPayload;
import com.awki.websocket.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionesOrchestrator {

    private final AlertaRepository alertaRepository;
    private final ContactoEmergenciaRepository contactoRepository;
    private final DispositivoMedicoRepository dispositivoMedicoRepository;
    
    private final SmsNotificationClient smsClient;
    private final WhatsappNotificationClient whatsappClient;
    private final PushNotificationClient pushClient;
    private final WebSocketNotificationService webSocketService;

    @Transactional
    public void orquestarNotificaciones(UUID alertaId) {
        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada: " + alertaId));

        log.info("Orquestando notificaciones para la alerta: {} - Tipo: {} - Nivel: {}", 
                alerta.getId(), alerta.getTipoAlerta(), alerta.getNivelUrgencia());

        boolean wsEntregado = false;
        
        // 1. Notificar al Médico por WebSocket
        if (alerta.getMedico() != null) {
            UUID medicoId = alerta.getMedico().getId();
            try {
                String nombrePac = "Sistema";
                UUID pacId = null;
                if (alerta.getPaciente() != null) {
                    pacId = alerta.getPaciente().getId();
                    nombrePac = alerta.getPaciente().getNombres() + " " + alerta.getPaciente().getApellidos();
                }

                AlertaPayload wsPayload = new AlertaPayload(
                        alerta.getId(),
                        pacId,
                        nombrePac,
                        alerta.getTipoAlerta().name(),
                        alerta.getDescripcion(),
                        alerta.getNivelUrgencia().name(),
                        System.currentTimeMillis()
                );
                
                webSocketService.enviarAlertaNueva(medicoId, wsPayload);
                alerta.setWebsocketEnviado(true);
                alerta.setEstadoEntrega(EstadoEntrega.ENTREGADA_WEBSOCKET);
                wsEntregado = true;
                log.info("Notificación de WebSocket enviada con éxito al médico: {}", medicoId);
            } catch (Exception e) {
                log.error("Fallo al enviar notificación por WebSocket al médico: {}. Iniciando canales de respaldo.", medicoId, e);
            }
        }

        // 2. Fallback: Si WebSocket falla técnicamente (o el médico no estaba conectado), ir a SMS y FCM
        if (!wsEntregado && alerta.getMedico() != null) {
            // Nota: El médico no tiene teléfono directo, se envía a número de respaldo/clínica configurado
            String telRespaldo = "+000000000"; 
            try {
                log.info("Iniciando envío de SMS de respaldo para el médico...");
                boolean smsOk = smsClient.enviarSms(telRespaldo, "Awki Alerta Clínica: " + alerta.getDescripcion());
                alerta.setIntentosSms(alerta.getIntentosSms() + 1);
                if (smsOk) {
                    alerta.setSmsEnviado(true);
                    alerta.setEstadoEntrega(EstadoEntrega.ENTREGADA_SMS);
                    log.info("SMS de respaldo enviado al médico.");
                }
            } catch (Exception e) {
                log.error("Error al enviar SMS de respaldo al médico", e);
            }

            // Enviar FCM (Push Notification) a los dispositivos del médico
            List<DispositivoMedico> dispositivos = dispositivoMedicoRepository.findByMedico_Id(alerta.getMedico().getId());
            if (!dispositivos.isEmpty()) {
                log.info("Encontrados {} dispositivos registrados para el médico. Enviando push...", dispositivos.size());
                for (DispositivoMedico dispositivo : dispositivos) {
                    try {
                        boolean pushOk = pushClient.enviarNotificacionPush(
                                dispositivo.getFcmToken(), 
                                "ALERTA CLÍNICA: " + alerta.getNivelUrgencia(), 
                                alerta.getDescripcion()
                        );
                        alerta.setIntentosFcm(alerta.getIntentosFcm() + 1);
                        if (pushOk) {
                            alerta.setFcmEnviado(true);
                            alerta.setEstadoEntrega(EstadoEntrega.ENTREGADA_FCM);
                            log.info("Notificación push FCM enviada con éxito.");
                        }
                    } catch (Exception e) {
                        log.error("Error al enviar FCM al dispositivo {}", dispositivo.getId(), e);
                    }
                }
            } else {
                log.info("El médico no tiene dispositivos registrados para notificaciones push.");
            }
        }

        // 3. Si la alerta es un SOS, notificar a los contactos de emergencia de la paciente
        if (alerta.getTipoAlerta() == com.awki.alerta.entity.TipoAlerta.SOS_MANUAL && alerta.getPaciente() != null) {
            UUID pacienteId = alerta.getPaciente().getId();
            List<ContactoEmergencia> contactos = contactoRepository.findByPaciente_IdAndActivoTrue(pacienteId);
            log.info("Procesando SOS. Encontrados {} contactos de emergencia para la paciente {}", contactos.size(), pacienteId);
            
            String msgSms = "EMERGENCIA Awki: Su contacto " + 
                    alerta.getPaciente().getNombres() + " " + alerta.getPaciente().getApellidos() + 
                    " ha activado un botón SOS. " + 
                    (alerta.getMensajeLibre() != null ? ("Mensaje: " + alerta.getMensajeLibre()) : "") +
                    (alerta.getLatitud() != null ? (" Ubicación: https://maps.google.com/?q=" + alerta.getLatitud() + "," + alerta.getLongitud()) : "");

            for (ContactoEmergencia contacto : contactos) {
                try {
                    if (contacto.getCanalPreferido() == CanalPreferido.SMS || contacto.getCanalPreferido() == CanalPreferido.AMBOS) {
                        log.info("Enviando SMS de SOS a contacto de emergencia: {}", contacto.getNombre());
                        smsClient.enviarSms(contacto.getTelefono(), msgSms);
                    }
                    if (contacto.getCanalPreferido() == CanalPreferido.WHATSAPP || contacto.getCanalPreferido() == CanalPreferido.AMBOS) {
                        log.info("Enviando WhatsApp de SOS a contacto de emergencia: {}", contacto.getNombre());
                        whatsappClient.enviarWhatsapp(contacto.getTelefono(), msgSms);
                    }
                } catch (Exception e) {
                    log.error("Error al enviar notificación de SOS a contacto: {}", contacto.getNombre(), e);
                }
            }
        }

        // Si fallaron todos los canales
        if (alerta.getEstadoEntrega() == EstadoEntrega.PENDIENTE) {
            alerta.setEstadoEntrega(EstadoEntrega.PENDIENTE_ENTREGA);
        }

        alertaRepository.save(alerta);
    }
}
