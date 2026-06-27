package com.awki.alerta.dto;

import com.awki.alerta.entity.NivelUrgencia;
import com.awki.alerta.entity.OrigenAlerta;
import com.awki.alerta.entity.TipoAlerta;

import java.util.List;
import java.util.UUID;

public record CrearAlertaInternaRequest(
    UUID embarazoId,
    TipoAlerta tipoAlerta,
    NivelUrgencia nivelUrgencia,
    String descripcion,
    List<String> sintomasDisparadores,
    OrigenAlerta origen
) {}
