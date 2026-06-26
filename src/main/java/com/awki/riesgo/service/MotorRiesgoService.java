package com.awki.riesgo.service;

import com.awki.common.enums.NivelRiesgo;
import com.awki.embarazo.dto.AntecedentesResponse;
import com.awki.embarazo.dto.EmbarazoResponse;
import com.awki.embarazo.service.EmbarazoService;
import com.awki.riesgo.dto.*;
import com.awki.riesgo.entity.EvaluacionRiesgo;
import com.awki.riesgo.repository.EvaluacionRiesgoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MotorRiesgoService {

    private final EmbarazoService embarazoService;
    private final EvaluacionRiesgoRepository evaluacionRiesgoRepository;

    public ResultadoRiesgo evaluarDesdeControl(UUID embarazoId, ControlPrenatal control) {
        EmbarazoResponse embarazo = embarazoService.obtenerEmbarazoPorId(embarazoId);
        AntecedentesResponse antecedentes = embarazoService.obtenerAntecedentes(embarazoId);
        ResultadoRiesgo resultado = clasificarDesdeControl(control, embarazo, antecedentes);
        persistirEvaluacion(embarazoId, resultado, FuenteEvaluacion.CONTROL);
        return resultado;
    }

    @Async
    public CompletableFuture<ResultadoRiesgo> evaluarDesdeSintomas(UUID embarazoId, List<SintomaDetectado> sintomas) {
        EmbarazoResponse embarazo = embarazoService.obtenerEmbarazoPorId(embarazoId);
        AntecedentesResponse antecedentes = embarazoService.obtenerAntecedentes(embarazoId);
        ResultadoRiesgo resultado = clasificarDesdeSintomas(sintomas, embarazo, antecedentes);
        persistirEvaluacion(embarazoId, resultado, FuenteEvaluacion.SINTOMAS);
        return CompletableFuture.completedFuture(resultado);
    }

    public ResultadoRiesgo evaluarSOS(UUID embarazoId) {
        ResultadoRiesgo resultado = new ResultadoRiesgo(
                NivelRiesgo.ROJO,
                List.of("sos_manual"),
                true,
                "ALERTA CRÍTICA: Activación manual de emergencia SOS. Requiere atención médica INMEDIATA."
        );
        persistirEvaluacion(embarazoId, resultado, FuenteEvaluacion.SOS);
        return resultado;
    }

    private ResultadoRiesgo clasificarDesdeControl(ControlPrenatal c,
                                                    EmbarazoResponse embarazo,
                                                    AntecedentesResponse ant) {
        // ROJO — fail-fast en orden de prioridad
        if (c.presionSistolica() != null && c.presionSistolica() >= 140) {
            return buildRojo("presion_sistolica_critica");
        }
        if (c.presionDiastolica() != null && c.presionDiastolica() >= 90) {
            return buildRojo("presion_diastolica_critica");
        }
        if (c.proteinuria() == Proteinuria.DOS_CRUCES || c.proteinuria() == Proteinuria.TRES_CRUCES) {
            return buildRojo("proteinuria_severa");
        }
        if (c.frecuenciaCardiacaFetal() != null &&
                (c.frecuenciaCardiacaFetal() < 100 || c.frecuenciaCardiacaFetal() > 180)) {
            return buildRojo("frecuencia_cardiaca_fetal_anormal");
        }
        if (c.fiebre() != null && c.fiebre() >= 38.0) {
            return buildRojo("fiebre_alta");
        }

        // AMARILLO — recopilar todos los criterios activos
        List<String> criterios = new ArrayList<>();

        double umbralAnemia = ant.residenciaAltitud() ? 10.7 : 11.0;

        if (ant.edadMaternaRiesgo()) criterios.add("edad_materna_riesgo");
        if (c.presionSistolica() != null && c.presionSistolica() >= 130) criterios.add("presion_sistolica_elevada");
        if (c.presionDiastolica() != null && c.presionDiastolica() >= 80) criterios.add("presion_diastolica_elevada");
        if (c.hemoglobina() != null && c.hemoglobina() < umbralAnemia && c.hemoglobina() >= 8.0) criterios.add("anemia");
        if (c.proteinuria() == Proteinuria.UNA_CRUZ) criterios.add("proteinuria_leve");
        if (c.movimientosFetalesReporte() == MovimientosFetalesReporte.DISMINUIDOS) criterios.add("movimientos_fetales_disminuidos");
        if (c.edemas() == TipoEdema.CARA || c.edemas() == TipoEdema.GENERALIZADO) criterios.add("edemas_significativos");
        if (c.contracciones() && embarazo.semanasGestacionActuales() < 37) criterios.add("contracciones_prematuras");
        if (ant.hipertensionPrevia()) criterios.add("antecedente_hipertension");
        if (ant.preeclampsiaPrevia()) criterios.add("antecedente_preeclampsia");
        if (ant.residenciaAltitud()) criterios.add("residencia_altitud");
        if (embarazo.numeroCesareas() != null && embarazo.numeroCesareas() >= 1) criterios.add("cesarea_previa");

        if (!criterios.isEmpty()) {
            return buildAmarillo(criterios);
        }

        return buildVerde();
    }

    private ResultadoRiesgo clasificarDesdeSintomas(List<SintomaDetectado> sintomas,
                                                     EmbarazoResponse embarazo,
                                                     AntecedentesResponse ant) {
        Set<TipoSintoma> presentes = sintomas.stream()
                .map(SintomaDetectado::tipo)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoSintoma.class)));

        // ROJO — fail-fast en orden de prioridad
        if (presentes.contains(TipoSintoma.CEFALEA_INTENSA) &&
                (presentes.contains(TipoSintoma.VISION_BORROSA)
                        || presentes.contains(TipoSintoma.TINNITUS)
                        || presentes.contains(TipoSintoma.EPIGASTRALGIA))) {
            return buildRojo("cefalea_intensa_con_signos_preeclampsia");
        }
        if (presentes.contains(TipoSintoma.SANGRADO_VAGINAL)) {
            return buildRojo("sangrado_vaginal");
        }
        if (presentes.contains(TipoSintoma.AUSENCIA_MOVIMIENTOS_FETALES)
                && embarazo.semanasGestacionActuales() >= 20) {
            return buildRojo("ausencia_movimientos_fetales");
        }
        if (presentes.contains(TipoSintoma.CONVULSIONES)) {
            return buildRojo("convulsiones");
        }
        if (presentes.contains(TipoSintoma.PERDIDA_LIQUIDO_AMNIOTICO)) {
            return buildRojo("perdida_liquido_amniotico");
        }

        // AMARILLO
        List<String> criterios = new ArrayList<>();

        if (presentes.contains(TipoSintoma.CEFALEA)) criterios.add("cefalea");
        if (presentes.contains(TipoSintoma.ARDOR_ORINAR)) criterios.add("ardor_al_orinar");
        if (ant.edadMaternaRiesgo()) criterios.add("edad_materna_riesgo");
        if (ant.hipertensionPrevia()) criterios.add("antecedente_hipertension");
        if (ant.preeclampsiaPrevia()) criterios.add("antecedente_preeclampsia");
        if (ant.residenciaAltitud()) criterios.add("residencia_altitud");

        if (!criterios.isEmpty()) {
            return buildAmarillo(criterios);
        }

        return buildVerde();
    }

    private ResultadoRiesgo buildRojo(String criterio) {
        return new ResultadoRiesgo(
                NivelRiesgo.ROJO,
                List.of(criterio),
                true,
                "ALERTA CRÍTICA: " + criterio.replace("_", " ") + ". Requiere atención médica INMEDIATA."
        );
    }

    private ResultadoRiesgo buildAmarillo(List<String> criterios) {
        String detalle = criterios.stream()
                .map(c -> c.replace("_", " "))
                .collect(Collectors.joining(", "));
        return new ResultadoRiesgo(
                NivelRiesgo.AMARILLO,
                criterios,
                true,
                "ALERTA MODERADA: " + detalle + ". Se recomienda evaluación médica."
        );
    }

    private ResultadoRiesgo buildVerde() {
        return new ResultadoRiesgo(NivelRiesgo.VERDE, List.of(), false, "Sin criterios de riesgo detectados.");
    }

    private void persistirEvaluacion(UUID embarazoId, ResultadoRiesgo resultado, FuenteEvaluacion fuente) {
        EvaluacionRiesgo evaluacion = new EvaluacionRiesgo();
        evaluacion.setEmbarazoId(embarazoId);
        evaluacion.setNivel(resultado.nivel());
        evaluacion.setCriteriosActivos(resultado.criteriosActivos());
        evaluacion.setDescripcionAlerta(resultado.descripcionAlerta());
        evaluacion.setGenerarAlerta(resultado.generarAlerta());
        evaluacion.setFuenteEvaluacion(fuente);
        evaluacionRiesgoRepository.save(evaluacion);
    }
}
