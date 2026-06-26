package com.awki.embarazo.dto;

import com.awki.common.enums.EstadoEmbarazo;
import java.time.LocalDate;
import java.util.UUID;

public record EmbarazoResponse(
        UUID id,
        UUID pacienteId,
        UUID medicoId,
        LocalDate fechaUltimaMenstruacion,
        LocalDate fechaProbableParto,
        LocalDate fechaProbablePartoEco,
        Integer semanasGestacionIngreso,
        long semanasGestacionActuales,
        int trimestre,
        Integer numeroGestacion,
        Integer numeroPartos,
        Integer numeroAbortos,
        Integer numeroCesareas,
        boolean embarazoMultiple,
        EstadoEmbarazo estado,
        LocalDate fechaFin
) {}
