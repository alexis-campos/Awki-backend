package com.awki.alerta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AlertaResponse(
    UUID id,
    UUID embarazoId,
    UUID pacienteId,
    String nombrePaciente,
    UUID medicoId,
    UUID clinicaId,
    String tipoAlerta,
    String nivelUrgencia,
    String descripcion,
    List<String> sintomasDisparadores,
    String origen,
    boolean vistaPorMedico,
    LocalDateTime fechaVista,
    String estadoEntrega,
    boolean websocketEnviado,
    boolean smsEnviado,
    boolean whatsappEnviado,
    boolean fcmEnviado,
    BigDecimal latitud,
    BigDecimal longitud,
    String mensajeLibre,
    LocalDateTime createdAt
) {}
