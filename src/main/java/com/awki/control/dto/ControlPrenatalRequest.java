package com.awki.control.dto;

import com.awki.control.entity.EdemasControl;
import com.awki.control.entity.MovimientosFetalesControl;
import com.awki.control.entity.PresentacionFetal;
import com.awki.control.entity.ProteinuriaControl;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ControlPrenatalRequest(
        @NotNull(message = "El id del embarazo es obligatorio")
        UUID embarazoId,

        @NotNull(message = "La fecha del control es obligatoria")
        @PastOrPresent(message = "La fecha del control no puede ser futura")
        LocalDate fechaControl,

        @NotNull(message = "Las semanas de gestación son obligatorias")
        @Min(value = 4, message = "Las semanas de gestación mínimas son 4")
        @Max(value = 42, message = "Las semanas de gestación máximas son 42")
        Integer semanasGestacion,

        @NotNull(message = "El peso es obligatorio")
        @DecimalMin(value = "30.0", message = "El peso mínimo es 30 kg")
        @DecimalMax(value = "200.0", message = "El peso máximo es 200 kg")
        BigDecimal pesoKg,

        @DecimalMin(value = "100.0", message = "La talla mínima es 100 cm")
        @DecimalMax(value = "220.0", message = "La talla máxima es 220 cm")
        BigDecimal tallaCm,

        @NotNull(message = "La presión sistólica es obligatoria")
        @Min(value = 60, message = "La presión sistólica mínima es 60")
        @Max(value = 220, message = "La presión sistólica máxima es 220")
        Integer presionArterialSistolica,

        @NotNull(message = "La presión diastólica es obligatoria")
        @Min(value = 40, message = "La presión diastólica mínima es 40")
        @Max(value = 150, message = "La presión diastólica máxima es 150")
        Integer presionArterialDiastolica,

        @DecimalMin(value = "0.0", message = "La altura uterina no puede ser negativa")
        BigDecimal alturaUterinaCm,

        @Min(value = 60, message = "La frecuencia cardiaca fetal mínima es 60")
        @Max(value = 220, message = "La frecuencia cardiaca fetal máxima es 220")
        Integer frecuenciaCardiacaFetal,

        PresentacionFetal presentacionFetal,

        @DecimalMin(value = "4.0", message = "La hemoglobina mínima es 4 g/dL")
        @DecimalMax(value = "20.0", message = "La hemoglobina máxima es 20 g/dL")
        BigDecimal hemoglobinaGdl,

        ProteinuriaControl proteinuria,

        @DecimalMin(value = "0.0", message = "La glucosa no puede ser negativa")
        BigDecimal glucosaMgdl,

        MovimientosFetalesControl movimientosFetalesReporte,

        EdemasControl edemas,

        LocalDate proximaCita,

        @Size(max = 3000, message = "Las observaciones no deben superar 3000 caracteres")
        String observacionesMedico,

        BigDecimal fiebre,

        Boolean contracciones
) {}