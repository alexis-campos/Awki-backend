package com.awki.riesgo.service;

import com.awki.common.enums.NivelRiesgo;
import com.awki.embarazo.dto.AntecedentesResponse;
import com.awki.embarazo.dto.EmbarazoResponse;
import com.awki.embarazo.service.EmbarazoService;
import com.awki.riesgo.dto.*;
import com.awki.riesgo.entity.EvaluacionRiesgo;
import com.awki.riesgo.repository.EvaluacionRiesgoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.awki.common.enums.EstadoEmbarazo.ACTIVO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotorRiesgoServiceTest {

    @Mock
    private EmbarazoService embarazoService;

    @Mock
    private com.awki.alerta.service.AlertaService alertaService;

    @Mock
    private EvaluacionRiesgoRepository evaluacionRiesgoRepository;

    @InjectMocks
    private MotorRiesgoService motorRiesgoService;

    private UUID embarazoId;
    private EmbarazoResponse embarazoBase;
    private AntecedentesResponse antecedentesBase;

    @BeforeEach
    void setUp() {
        embarazoId = UUID.randomUUID();

        embarazoBase = new EmbarazoResponse(
                embarazoId, UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().minusWeeks(20), LocalDate.now().plusWeeks(20),
                null, 20, 20L, 2,
                1, 0, 0, 0,
                false, ACTIVO, null
        );

        antecedentesBase = new AntecedentesResponse(
                embarazoId, false, false, false, false, false,
                false, false, false, false,
                false, false, false, null
        );

        when(evaluacionRiesgoRepository.save(any(EvaluacionRiesgo.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ─────────────────────── evaluarSOS ───────────────────────

    @Test
    void evaluarSOS_SiempreRetornaRojo() {
        ResultadoRiesgo resultado = motorRiesgoService.evaluarSOS(embarazoId);

        assertEquals(NivelRiesgo.ROJO, resultado.nivel());
        assertTrue(resultado.generarAlerta());
        assertTrue(resultado.criteriosActivos().contains("sos_manual"));
        verify(evaluacionRiesgoRepository).save(any());
    }

    // ─────────────────────── evaluarDesdeControl — ROJO ───────────────────────

    @Test
    void evaluarDesdeControl_Rojo_PorPresionSistolica() {
        stubEmbarazo();
        ControlPrenatal control = control(140, 80, null, null, null, null, null, false);

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.ROJO, resultado.nivel());
        assertEquals("presion_sistolica_critica", resultado.criteriosActivos().get(0));
    }

    @Test
    void evaluarDesdeControl_Rojo_PorPresionDiastolica() {
        stubEmbarazo();
        ControlPrenatal control = control(120, 90, null, null, null, null, null, false);

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.ROJO, resultado.nivel());
        assertEquals("presion_diastolica_critica", resultado.criteriosActivos().get(0));
    }

    @Test
    void evaluarDesdeControl_Rojo_PorProteinuriaDoCruces() {
        stubEmbarazo();
        ControlPrenatal control = control(null, null, Proteinuria.DOS_CRUCES, null, null, null, null, false);

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.ROJO, resultado.nivel());
        assertEquals("proteinuria_severa", resultado.criteriosActivos().get(0));
    }

    @Test
    void evaluarDesdeControl_Rojo_PorProteinuriaTresCruces() {
        stubEmbarazo();
        ControlPrenatal control = control(null, null, Proteinuria.TRES_CRUCES, null, null, null, null, false);

        assertEquals(NivelRiesgo.ROJO, motorRiesgoService.evaluarDesdeControl(embarazoId, control).nivel());
    }

    @Test
    void evaluarDesdeControl_Rojo_PorFrecuenciaCardiacaBaja() {
        stubEmbarazo();
        ControlPrenatal control = control(null, null, null, 99, null, null, null, false);

        assertEquals(NivelRiesgo.ROJO, motorRiesgoService.evaluarDesdeControl(embarazoId, control).nivel());
    }

    @Test
    void evaluarDesdeControl_Rojo_PorFrecuenciaCardiacaAlta() {
        stubEmbarazo();
        ControlPrenatal control = control(null, null, null, 181, null, null, null, false);

        assertEquals(NivelRiesgo.ROJO, motorRiesgoService.evaluarDesdeControl(embarazoId, control).nivel());
    }

    @Test
    void evaluarDesdeControl_Rojo_PorFiebre() {
        stubEmbarazo();
        ControlPrenatal control = control(null, null, null, null, null, 38.0, null, false);

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.ROJO, resultado.nivel());
        assertEquals("fiebre_alta", resultado.criteriosActivos().get(0));
    }

    // ─────────────────────── evaluarDesdeControl — AMARILLO ───────────────────────

    @Test
    void evaluarDesdeControl_Amarillo_PorPresionSistolicaElevada() {
        stubEmbarazo();
        ControlPrenatal control = control(130, 70, null, null, null, null, null, false);

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.AMARILLO, resultado.nivel());
        assertTrue(resultado.criteriosActivos().contains("presion_sistolica_elevada"));
    }

    @Test
    void evaluarDesdeControl_Amarillo_PorAnemia_SinAltitud() {
        stubEmbarazo();
        ControlPrenatal control = control(null, null, null, null, 10.5, null, null, false);

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.AMARILLO, resultado.nivel());
        assertTrue(resultado.criteriosActivos().contains("anemia"));
    }

    @Test
    void evaluarDesdeControl_Amarillo_PorAnemia_ConAltitud_UmbralAjustado() {
        antecedentesBase = antecedentesConAltitud();
        stubEmbarazo();
        // 10.8 >= umbral_altitud (10.7) → NO es anemia con ajuste por altura
        ControlPrenatal control = control(null, null, null, null, 10.8, null, null, false);

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        // residencia_altitud sola activa AMARILLO, pero hemoglobina 10.8 NO es anemia con umbral 10.7
        assertEquals(NivelRiesgo.AMARILLO, resultado.nivel());
        assertFalse(resultado.criteriosActivos().contains("anemia"));
        assertTrue(resultado.criteriosActivos().contains("residencia_altitud"));
    }

    @Test
    void evaluarDesdeControl_Amarillo_PorAnemia_ConAltitud_BajoUmbral() {
        antecedentesBase = antecedentesConAltitud();
        stubEmbarazo();
        // 10.5 < umbral_altitud (10.7) → SÍ es anemia
        ControlPrenatal control = control(null, null, null, null, 10.5, null, null, false);

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.AMARILLO, resultado.nivel());
        assertTrue(resultado.criteriosActivos().contains("anemia"));
    }

    @Test
    void evaluarDesdeControl_Amarillo_PorEdemasCara() {
        stubEmbarazo();
        ControlPrenatal control = new ControlPrenatal(null, null, null, null, null, null,
                TipoEdema.CARA, null, false);

        assertEquals(NivelRiesgo.AMARILLO, motorRiesgoService.evaluarDesdeControl(embarazoId, control).nivel());
    }

    @Test
    void evaluarDesdeControl_Amarillo_PorContracciones_AntesDe37Semanas() {
        embarazoBase = embarazoConSemanas(36L);
        stubEmbarazo();
        ControlPrenatal control = control(null, null, null, null, null, null, null, true);

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.AMARILLO, resultado.nivel());
        assertTrue(resultado.criteriosActivos().contains("contracciones_prematuras"));
    }

    @Test
    void evaluarDesdeControl_NoEsAmarillo_PorContracciones_DespuesDe37Semanas() {
        embarazoBase = embarazoConSemanas(38L);
        stubEmbarazo();
        ControlPrenatal control = control(null, null, null, null, null, null, null, true);

        // contracciones a 38 semanas no es criterio de riesgo
        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);
        assertEquals(NivelRiesgo.VERDE, resultado.nivel());
    }

    @Test
    void evaluarDesdeControl_Amarillo_PorAntecedentes() {
        antecedentesBase = new AntecedentesResponse(
                embarazoId, false, true, true, false, false,
                false, false, false, false,
                false, false, false, null
        );
        stubEmbarazo();
        ControlPrenatal control = controlNormal();

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.AMARILLO, resultado.nivel());
        assertTrue(resultado.criteriosActivos().contains("antecedente_hipertension"));
        assertTrue(resultado.criteriosActivos().contains("antecedente_preeclampsia"));
    }

    @Test
    void evaluarDesdeControl_Amarillo_PorCesareaPrevia() {
        embarazoBase = embarazoConCesareas(1);
        stubEmbarazo();
        ControlPrenatal control = controlNormal();

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.AMARILLO, resultado.nivel());
        assertTrue(resultado.criteriosActivos().contains("cesarea_previa"));
    }

    // ─────────────────────── evaluarDesdeControl — VERDE ───────────────────────

    @Test
    void evaluarDesdeControl_Verde_SinCriterios() {
        stubEmbarazo();
        ControlPrenatal control = controlNormal();

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeControl(embarazoId, control);

        assertEquals(NivelRiesgo.VERDE, resultado.nivel());
        assertFalse(resultado.generarAlerta());
        assertTrue(resultado.criteriosActivos().isEmpty());
    }

    // ─────────────────────── evaluarDesdeSintomas — ROJO ───────────────────────

    @Test
    void evaluarDesdeSintomas_Rojo_CefaleaIntensaConVisionBorrosa() throws ExecutionException, InterruptedException {
        stubEmbarazo();
        List<SintomaDetectado> sintomas = List.of(
                new SintomaDetectado(TipoSintoma.CEFALEA_INTENSA),
                new SintomaDetectado(TipoSintoma.VISION_BORROSA)
        );

        CompletableFuture<ResultadoRiesgo> future = motorRiesgoService.evaluarDesdeSintomas(embarazoId, sintomas);
        ResultadoRiesgo resultado = future.get();

        assertEquals(NivelRiesgo.ROJO, resultado.nivel());
        assertEquals("cefalea_intensa_con_signos_preeclampsia", resultado.criteriosActivos().get(0));
    }

    @Test
    void evaluarDesdeSintomas_Rojo_CefaleaIntensaConTinnitus() throws ExecutionException, InterruptedException {
        stubEmbarazo();
        List<SintomaDetectado> sintomas = List.of(
                new SintomaDetectado(TipoSintoma.CEFALEA_INTENSA),
                new SintomaDetectado(TipoSintoma.TINNITUS)
        );

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeSintomas(embarazoId, sintomas).get();
        assertEquals(NivelRiesgo.ROJO, resultado.nivel());
    }

    @Test
    void evaluarDesdeSintomas_Rojo_SangradoVaginal() throws ExecutionException, InterruptedException {
        stubEmbarazo();
        List<SintomaDetectado> sintomas = List.of(new SintomaDetectado(TipoSintoma.SANGRADO_VAGINAL));

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeSintomas(embarazoId, sintomas).get();

        assertEquals(NivelRiesgo.ROJO, resultado.nivel());
        assertEquals("sangrado_vaginal", resultado.criteriosActivos().get(0));
    }

    @Test
    void evaluarDesdeSintomas_Rojo_AusenciaMovimientosConMasDe20Semanas() throws ExecutionException, InterruptedException {
        embarazoBase = embarazoConSemanas(24L);
        stubEmbarazo();
        List<SintomaDetectado> sintomas = List.of(new SintomaDetectado(TipoSintoma.AUSENCIA_MOVIMIENTOS_FETALES));

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeSintomas(embarazoId, sintomas).get();

        assertEquals(NivelRiesgo.ROJO, resultado.nivel());
        assertEquals("ausencia_movimientos_fetales", resultado.criteriosActivos().get(0));
    }

    @Test
    void evaluarDesdeSintomas_NoEsRojo_AusenciaMovimientosConMenosDe20Semanas() throws ExecutionException, InterruptedException {
        embarazoBase = embarazoConSemanas(18L);
        stubEmbarazo();
        List<SintomaDetectado> sintomas = List.of(new SintomaDetectado(TipoSintoma.AUSENCIA_MOVIMIENTOS_FETALES));

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeSintomas(embarazoId, sintomas).get();

        assertNotEquals(NivelRiesgo.ROJO, resultado.nivel());
    }

    @Test
    void evaluarDesdeSintomas_Rojo_Convulsiones() throws ExecutionException, InterruptedException {
        stubEmbarazo();
        List<SintomaDetectado> sintomas = List.of(new SintomaDetectado(TipoSintoma.CONVULSIONES));

        assertEquals(NivelRiesgo.ROJO,
                motorRiesgoService.evaluarDesdeSintomas(embarazoId, sintomas).get().nivel());
    }

    @Test
    void evaluarDesdeSintomas_Rojo_PerdidaLiquidoAmniotico() throws ExecutionException, InterruptedException {
        stubEmbarazo();
        List<SintomaDetectado> sintomas = List.of(new SintomaDetectado(TipoSintoma.PERDIDA_LIQUIDO_AMNIOTICO));

        assertEquals(NivelRiesgo.ROJO,
                motorRiesgoService.evaluarDesdeSintomas(embarazoId, sintomas).get().nivel());
    }

    // ─────────────────────── evaluarDesdeSintomas — AMARILLO ───────────────────────

    @Test
    void evaluarDesdeSintomas_Amarillo_PorCefaleaSola() throws ExecutionException, InterruptedException {
        stubEmbarazo();
        List<SintomaDetectado> sintomas = List.of(new SintomaDetectado(TipoSintoma.CEFALEA));

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeSintomas(embarazoId, sintomas).get();

        assertEquals(NivelRiesgo.AMARILLO, resultado.nivel());
        assertTrue(resultado.criteriosActivos().contains("cefalea"));
    }

    @Test
    void evaluarDesdeSintomas_Amarillo_PorArdorAlOrinar() throws ExecutionException, InterruptedException {
        stubEmbarazo();
        List<SintomaDetectado> sintomas = List.of(new SintomaDetectado(TipoSintoma.ARDOR_ORINAR));

        assertEquals(NivelRiesgo.AMARILLO,
                motorRiesgoService.evaluarDesdeSintomas(embarazoId, sintomas).get().nivel());
    }

    // ─────────────────────── evaluarDesdeSintomas — VERDE ───────────────────────

    @Test
    void evaluarDesdeSintomas_Verde_SinSintomas() throws ExecutionException, InterruptedException {
        stubEmbarazo();

        ResultadoRiesgo resultado = motorRiesgoService.evaluarDesdeSintomas(embarazoId, List.of()).get();

        assertEquals(NivelRiesgo.VERDE, resultado.nivel());
        assertFalse(resultado.generarAlerta());
    }

    // ─────────────────────── helpers ───────────────────────

    private void stubEmbarazo() {
        when(embarazoService.obtenerEmbarazoPorId(embarazoId)).thenReturn(embarazoBase);
        when(embarazoService.obtenerAntecedentes(embarazoId)).thenReturn(antecedentesBase);
    }

    private ControlPrenatal control(Integer sistolica, Integer diastolica, Proteinuria proteinuria,
                                    Integer fcf, Double hemoglobina, Double fiebre,
                                    TipoEdema edema, boolean contracciones) {
        return new ControlPrenatal(sistolica, diastolica, proteinuria, fcf, hemoglobina,
                fiebre, edema, null, contracciones);
    }

    private ControlPrenatal controlNormal() {
        return new ControlPrenatal(110, 70, Proteinuria.NEGATIVO, 140, 12.0, 36.5,
                TipoEdema.NINGUNO, MovimientosFetalesReporte.NORMALES, false);
    }

    private AntecedentesResponse antecedentesConAltitud() {
        return new AntecedentesResponse(
                embarazoId, false, false, false, false, false,
                false, false, false, false,
                false, true, false, null
        );
    }

    private EmbarazoResponse embarazoConSemanas(long semanas) {
        return new EmbarazoResponse(
                embarazoId, UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().minusWeeks(semanas), LocalDate.now().plusWeeks(40 - semanas),
                null, (int) semanas, semanas, semanas > 26 ? 3 : semanas > 13 ? 2 : 1,
                1, 0, 0, 0,
                false, ACTIVO, null
        );
    }

    private EmbarazoResponse embarazoConCesareas(int numeroCesareas) {
        return new EmbarazoResponse(
                embarazoId, UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().minusWeeks(20), LocalDate.now().plusWeeks(20),
                null, 20, 20L, 2,
                1, 0, 0, numeroCesareas,
                false, ACTIVO, null
        );
    }
}
