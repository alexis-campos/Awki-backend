package com.awki.alerta.port;

import com.awki.alerta.dto.CrearAlertaInternaRequest;
import com.awki.alerta.entity.NivelUrgencia;
import com.awki.alerta.entity.OrigenAlerta;
import com.awki.alerta.entity.TipoAlerta;
import com.awki.alerta.service.AlertaService;
import com.awki.chat.port.AlertaChatPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertaChatPortImpl implements AlertaChatPort {

    private final AlertaService alertaService;

    @Override
    public void crearAlertaSiCorresponde(UUID embarazoId, String contenido, boolean alarmaProbable) {
        log.info("[AlertaChatPortImpl] Recibida llamada desde chat. Embarazo: {}, Alarma probable: {}", embarazoId, alarmaProbable);
        if (alarmaProbable) {
            String descripcion = "Síntoma crítico detectado por IA en Chat: " + truncarContenido(contenido, 200);
            CrearAlertaInternaRequest req = new CrearAlertaInternaRequest(
                    embarazoId,
                    TipoAlerta.SINTOMA_CRITICO_IA,
                    NivelUrgencia.ROJO,
                    descripcion,
                    List.of("SINTOMA_CRITICO_IA"),
                    OrigenAlerta.CHAT
            );
            alertaService.crearAlertaDesdeRiesgo(req);
        }
    }

    private String truncarContenido(String contenido, int maxLen) {
        if (contenido == null) return "";
        if (contenido.length() <= maxLen) return contenido;
        return contenido.substring(0, maxLen - 3) + "...";
    }
}
