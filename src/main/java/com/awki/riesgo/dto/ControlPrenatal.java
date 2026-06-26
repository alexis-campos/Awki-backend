package com.awki.riesgo.dto;

public record ControlPrenatal(
        Integer presionSistolica,
        Integer presionDiastolica,
        Proteinuria proteinuria,
        Integer frecuenciaCardiacaFetal,
        Double hemoglobina,
        Double fiebre,
        TipoEdema edemas,
        MovimientosFetalesReporte movimientosFetalesReporte,
        boolean contracciones
) {}
