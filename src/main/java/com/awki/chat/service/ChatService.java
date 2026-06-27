package com.awki.chat.service;

import com.awki.auth.service.AuthService;
import com.awki.chat.dto.*;
import com.awki.chat.entity.MensajeChat;
import com.awki.chat.entity.RolMensajeChat;
import com.awki.chat.entity.ResumenClinico;
import com.awki.chat.repository.MensajeChatRepository;
import com.awki.chat.repository.ResumenClinicoRepository;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.repository.EmbarazoRepository;
import com.awki.common.enums.EstadoEmbarazo;
import com.awki.exception.BusinessRuleException;
import com.awki.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final MensajeChatRepository mensajeChatRepository;
    private final ResumenClinicoRepository resumenClinicoRepository;
    private final EmbarazoRepository embarazoRepository;
    private final AuthService authService;
    private final GeminiClient geminiClient;
    private final SemanticCacheService semanticCacheService;
    private final ChatAsyncService chatAsyncService;

    private static final String GEMINI_SYSTEM_PROMPT = 
            "Eres Awki, un asistente prenatal de apoyo para gestantes. Responde en español claro, breve, cálido y responsable. "
            + "No das diagnósticos definitivos, no reemplazas a un médico y no inventas información clínica. "
            + "Si el mensaje menciona síntomas de alarma como sangrado, pérdida de líquido, convulsiones, fiebre alta, "
            + "dolor abdominal intenso, cefalea intensa, visión borrosa, presión alta o ausencia de movimientos del bebé, "
            + "indica que puede ser un signo de alarma y recomienda contactar a su médico o acudir a emergencia. "
            + "Si la situación parece urgente, sugiere usar el botón SOS. Para consultas generales, brinda orientación preventiva y segura. "
            + "No solicites datos personales innecesarios.";

    @Transactional
    public ChatMensajeResponse enviarMensaje(ChatMensajeRequest request, UsuarioAutenticado usuario) {
        // 1. Validar rol PACIENTE
        if (!"PACIENTE".equals(usuario.rol())) {
            throw new BusinessRuleException("FORBIDDEN", "Solo las pacientes pueden enviar mensajes al chat");
        }

        // 2. Validar que el embarazo exista
        Embarazo embarazo = embarazoRepository.findById(request.embarazoId())
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", request.embarazoId().toString()));

        // 3. Validar que pertenezca a la paciente autenticada
        if (!embarazo.getPacienteId().equals(usuario.id())) {
            throw new BusinessRuleException("FORBIDDEN", "No tienes acceso a este embarazo");
        }

        String contenido = request.contenido().trim();
        boolean alarmaProbable = detectarAlarmaProbable(contenido);

        // 4. Si no es alarma, intentar buscar en caché semántica
        if (!alarmaProbable) {
            var cacheOpt = semanticCacheService.buscarEnCache(contenido);
            if (cacheOpt.isPresent()) {
                String respuestaCache = cacheOpt.get();

                MensajeChat mensajePaciente = guardarMensaje(embarazo, RolMensajeChat.PACIENTE, contenido, false, true, false);
                MensajeChat mensajeIa = guardarMensaje(embarazo, RolMensajeChat.IA, respuestaCache, false, true, false);

                // Hilos asíncronos para motor de riesgo y alertas
                chatAsyncService.analizarRiesgoAsync(embarazo.getId(), contenido, false);
                chatAsyncService.crearAlertaSiCorrespondeAsync(embarazo.getId(), contenido, false);

                return new ChatMensajeResponse(
                        mensajePaciente.getId(),
                        mensajeIa.getId(),
                        respuestaCache,
                        false,
                        true,
                        false,
                        LocalDateTime.now()
                );
            }
        }

        // 5. Construcción del Prompt
        long semanas = ChronoUnit.WEEKS.between(embarazo.getFechaUltimaMenstruacion(), LocalDate.now());
        String contextoMinimo = String.format(
                "Paciente en la semana %d de gestación. Embarazo múltiple: %b. Gestas previas: %d, Partos: %d, Cesáreas: %d.",
                semanas, embarazo.isEmbarazoMultiple(), embarazo.getNumeroGestacion(), embarazo.getNumeroPartos(), embarazo.getNumeroCesareas()
        );

        String prompt = GEMINI_SYSTEM_PROMPT + "\n\nContexto clínico de la gestante:\n" + contextoMinimo 
                + "\n\nConsulta de la paciente:\n" + contenido;

        String respuestaIa;
        boolean fallbackUsado = false;

        try {
            var geminiOpt = geminiClient.generarContenido(prompt);
            if (geminiOpt.isPresent()) {
                respuestaIa = geminiOpt.get();
            } else {
                respuestaIa = obtenerFallbackLocal(contenido, alarmaProbable);
                fallbackUsado = true;
            }
        } catch (Exception e) {
            log.error("[ChatService] Error al generar respuesta de Gemini: {}", e.getMessage());
            respuestaIa = obtenerFallbackLocal(contenido, alarmaProbable);
            fallbackUsado = true;
        }

        // 6. Guardar en base de datos
        MensajeChat mensajePaciente = guardarMensaje(embarazo, RolMensajeChat.PACIENTE, contenido, alarmaProbable, false, fallbackUsado);
        MensajeChat mensajeIa = guardarMensaje(embarazo, RolMensajeChat.IA, respuestaIa, alarmaProbable, false, fallbackUsado);

        // 7. Guardar en caché si es seguro
        if (!alarmaProbable && !fallbackUsado) {
            semanticCacheService.guardarEnCache(contenido, respuestaIa);
        }

        // 8. Tareas asíncronas en segundo plano
        chatAsyncService.analizarRiesgoAsync(embarazo.getId(), contenido, alarmaProbable);
        chatAsyncService.crearAlertaSiCorrespondeAsync(embarazo.getId(), contenido, alarmaProbable);

        return new ChatMensajeResponse(
                mensajePaciente.getId(),
                mensajeIa.getId(),
                respuestaIa,
                alarmaProbable,
                false,
                fallbackUsado,
                LocalDateTime.now()
        );
    }

    public Page<MensajeChatResponse> obtenerHistorial(UUID embarazoId, int page, int size, UsuarioAutenticado usuario) {
        // Validar rol PACIENTE
        if (!"PACIENTE".equals(usuario.rol())) {
            throw new BusinessRuleException("FORBIDDEN", "Solo las pacientes pueden consultar su historial detallado de chat");
        }

        Embarazo embarazo = embarazoRepository.findById(embarazoId)
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", embarazoId.toString()));

        // Validar dueño del embarazo
        if (!embarazo.getPacienteId().equals(usuario.id())) {
            throw new BusinessRuleException("FORBIDDEN", "No tienes acceso a este embarazo");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<MensajeChat> mensajes = mensajeChatRepository.findByEmbarazoIdOrderByCreatedAtDesc(embarazoId, pageable);

        return mensajes.map(m -> new MensajeChatResponse(
                m.getId(),
                m.getEmbarazo().getId(),
                m.getRol().name(),
                m.getContenido(),
                m.isAlarmaProbable(),
                m.isDesdeCache(),
                m.isFallbackUsado(),
                m.getCreatedAt()
        ));
    }

    @Transactional
    public ResumenClinicoResponse obtenerResumenClinico(UUID embarazoId, UsuarioAutenticado usuario) {
        // Validar rol MEDICO o ADMIN_CLINICA
        if (!"MEDICO".equals(usuario.rol()) && !"ADMIN_CLINICA".equals(usuario.rol())) {
            throw new BusinessRuleException("FORBIDDEN", "No tienes permisos para ver el resumen clínico");
        }

        Embarazo embarazo = embarazoRepository.findById(embarazoId)
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", embarazoId.toString()));

        // Validar multi-tenant
        UUID pacienteClinicaId = authService.getClinicaIdByPacienteId(embarazo.getPacienteId());
        if (!pacienteClinicaId.equals(usuario.clinicaId())) {
            throw new BusinessRuleException("FORBIDDEN", "No tienes acceso a los datos de esta paciente");
        }

        // Buscar resumen existente
        var resumenOpt = resumenClinicoRepository.findById(embarazoId);
        if (resumenOpt.isPresent()) {
            ResumenClinico rc = resumenOpt.get();
            return new ResumenClinicoResponse(rc.getEmbarazoId(), rc.getContenidoResumen(), rc.getGeneradoPorModelo(), rc.getUpdatedAt());
        }

        // Si no existe, generarlo síncronamente de los últimos 50 mensajes por primera vez
        log.info("[ChatService] Generando primer resumen clínico para embarazo: {}", embarazoId);
        List<MensajeChat> mensajes = mensajeChatRepository.findTop50ByEmbarazoIdOrderByCreatedAtDesc(embarazoId);
        
        String contenidoResumen;
        String modelo = "Gemini API";

        if (mensajes.isEmpty()) {
            contenidoResumen = "No hay interacciones registradas en el chat con la paciente.";
            modelo = "Sistema";
        } else {
            String transcripcion = mensajes.stream()
                    .map(m -> m.getRol().name() + ": " + m.getContenido())
                    .collect(Collectors.joining("\n"));

            String prompt = "Genera un resumen clínico breve para un médico a partir de estos mensajes de una gestante. No transcribas la conversación completa. Extrae solo datos clínicamente relevantes: síntomas mencionados, frecuencia, intensidad si aparece, posibles signos de alarma, dudas recurrentes, estado emocional relevante y recomendaciones de seguimiento. No incluyas información íntima no clínica. No inventes datos. Escribe en español formal y claro.\n\nConversación:\n" + transcripcion;

            try {
                var responseOpt = geminiClient.generarContenido(prompt);
                if (responseOpt.isPresent()) {
                    contenidoResumen = responseOpt.get();
                } else {
                    contenidoResumen = generarResumenLocalBasico(mensajes);
                    modelo = "Fallback Local";
                }
            } catch (Exception e) {
                contenidoResumen = generarResumenLocalBasico(mensajes);
                modelo = "Fallback Local";
            }
        }

        ResumenClinico nuevoResumen = new ResumenClinico();
        nuevoResumen.setEmbarazoId(embarazoId);
        nuevoResumen.setEmbarazo(embarazo);
        nuevoResumen.setContenidoResumen(contenidoResumen);
        nuevoResumen.setGeneradoPorModelo(modelo);
        nuevoResumen.setUpdatedAt(LocalDateTime.now());
        nuevoResumen = resumenClinicoRepository.save(nuevoResumen);

        return new ResumenClinicoResponse(
                nuevoResumen.getEmbarazoId(),
                nuevoResumen.getContenidoResumen(),
                nuevoResumen.getGeneradoPorModelo(),
                nuevoResumen.getUpdatedAt()
        );
    }

    public void regenerarResumen(UUID embarazoId, UsuarioAutenticado usuario) {
        // Validar rol MEDICO o ADMIN_CLINICA
        if (!"MEDICO".equals(usuario.rol()) && !"ADMIN_CLINICA".equals(usuario.rol())) {
            throw new BusinessRuleException("FORBIDDEN", "No tienes permisos para regenerar el resumen clínico");
        }

        Embarazo embarazo = embarazoRepository.findById(embarazoId)
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", embarazoId.toString()));

        // Validar multi-tenant
        UUID pacienteClinicaId = authService.getClinicaIdByPacienteId(embarazo.getPacienteId());
        if (!pacienteClinicaId.equals(usuario.clinicaId())) {
            throw new BusinessRuleException("FORBIDDEN", "No tienes acceso a los datos de esta paciente");
        }

        // Obtener los últimos 50 mensajes
        List<MensajeChat> mensajes = mensajeChatRepository.findTop50ByEmbarazoIdOrderByCreatedAtDesc(embarazoId);

        // Llamar asíncronamente al servicio de asincronía
        chatAsyncService.regenerarResumenAsync(embarazoId, mensajes, resumenClinicoRepository, embarazo, geminiClient);
    }

    public boolean detectarAlarmaProbable(String contenido) {
        if (contenido == null) return false;
        String normalized = semanticCacheService.normalizar(contenido);

        return normalized.contains("sangrado")
                || normalized.contains("hemorragia")
                || normalized.contains("cefalea intensa")
                || normalized.contains("dolor de cabeza fuerte")
                || normalized.contains("dolor de cabeza muy fuerte")
                || normalized.contains("vision borrosa")
                || normalized.contains("vision doble")
                || normalized.contains("convulsion")
                || normalized.contains("fiebre alta")
                || normalized.contains("perdida de liquido")
                || normalized.contains("perdida de agua")
                || normalized.contains("no siento movimientos")
                || normalized.contains("no siento al bebe")
                || normalized.contains("no siento a mi bebe")
                || normalized.contains("no se mueve mi bebe")
                || normalized.contains("no se mueve mi  bebe")
                || normalized.contains("dolor abdominal intenso")
                || normalized.contains("dolor abdominal muy fuerte")
                || normalized.contains("dolor de barriga fuerte")
                || normalized.contains("hinchazon de cara")
                || normalized.contains("presion alta")
                || normalized.contains("zumbido de oidos");
    }

    private MensajeChat guardarMensaje(Embarazo embarazo, RolMensajeChat rol, String contenido, boolean alarmaProbable, boolean desdeCache, boolean fallbackUsado) {
        MensajeChat mensaje = new MensajeChat();
        mensaje.setEmbarazo(embarazo);
        mensaje.setRol(rol);
        mensaje.setContenido(contenido);
        mensaje.setAlarmaProbable(alarmaProbable);
        mensaje.setDesdeCache(desdeCache);
        mensaje.setFallbackUsado(fallbackUsado);
        return mensajeChatRepository.save(mensaje);
    }

    private String obtenerFallbackLocal(String contenido, boolean alarmaProbable) {
        if (alarmaProbable) {
            return "Lo que mencionas puede ser un signo de alarma durante el embarazo. Por seguridad, contacta a tu médico o acude al centro de salud más cercano. Si sientes que es urgente, usa el botón SOS.";
        }

        String lower = contenido.toLowerCase();
        if (lower.contains("triste") || lower.contains("ansied") || lower.contains("miedo") 
                || lower.contains("sola") || lower.contains("depre") || lower.contains("llor")) {
            return "Siento que estés pasando por esto. Trata de respirar con calma y busca apoyo de alguien cercano. Si te sientes en peligro o muy mal, contacta a tu médico o a un servicio de emergencia.";
        }

        return "Ahora no pude conectarme con la IA, pero tu consulta fue registrada. Puedes intentar nuevamente en unos momentos. Si tienes dolor intenso, sangrado, fiebre alta o algún síntoma preocupante, comunícate con tu médico.";
    }

    private String generarResumenLocalBasico(List<MensajeChat> mensajes) {
        long mensajesPaciente = mensajes.stream().filter(m -> "PACIENTE".equals(m.getRol().name())).count();
        long mensajesConAlarma = mensajes.stream().filter(MensajeChat::isAlarmaProbable).count();

        return "Resumen Clínico Básico (Generado Localmente):\n"
                + "- Se registraron " + mensajesPaciente + " mensajes de la gestante.\n"
                + "- Se detectaron " + mensajesConAlarma + " posibles signos de alarma o alerta.\n"
                + "- Revisar el estado de la paciente en consulta presencial.";
    }
}
