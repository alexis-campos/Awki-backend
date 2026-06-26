package com.awki.embarazo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.util.UUID;

public record EmbarazoRequest(
        @NotNull(message = "El id de la paciente es obligatorio")
        UUID pacienteId,

        @NotNull(message = "La fecha de última menstruación (FUM) es obligatoria")
        @PastOrPresent(message = "La FUM no puede ser una fecha futura")
        LocalDate fechaUltimaMenstruacion,

        LocalDate fechaProbablePartoEco,

        @NotNull(message = "El número de gestaciones es obligatorio")
        Integer numeroGestacion,

        Integer numeroPartos,
        Integer numeroAbortos,
        Integer numeroCesareas,
        Boolean embarazoMultiple
) {}
