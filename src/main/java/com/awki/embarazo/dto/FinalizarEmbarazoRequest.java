package com.awki.embarazo.dto;

import com.awki.common.enums.EstadoEmbarazo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record FinalizarEmbarazoRequest(
        @NotNull(message = "El estado de finalización es obligatorio")
        EstadoEmbarazo estado,

        @NotNull(message = "La fecha de fin es obligatoria")
        @PastOrPresent(message = "La fecha de fin no puede ser futura")
        LocalDate fechaFin
) {}
