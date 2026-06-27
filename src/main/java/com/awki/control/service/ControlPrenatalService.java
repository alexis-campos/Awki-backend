package com.awki.control.service;

import com.awki.auth.entity.Medico;
import com.awki.common.enums.EstadoEmbarazo;
import com.awki.control.dto.ControlPrenatalRequest;
import com.awki.control.dto.ControlPrenatalResponse;
import com.awki.control.dto.ControlPrenatalResumenResponse;
import com.awki.control.entity.ControlPrenatal;
import com.awki.control.entity.EdemasControl;
import com.awki.control.entity.MovimientosFetalesControl;
import com.awki.control.entity.ProteinuriaControl;
import com.awki.control.repository.ControlPrenatalRepository;
import com.awki.control.repository.MedicoControlRepository;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.repository.EmbarazoRepository;
import com.awki.exception.BusinessRuleException;
import com.awki.exception.ResourceNotFoundException;
import com.awki.riesgo.dto.ResultadoRiesgo;
import com.awki.riesgo.dto.TipoEdema;
import com.awki.riesgo.service.MotorRiesgoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ControlPrenatalService {

    private final ControlPrenatalRepository controlPrenatalRepository;
    private final EmbarazoRepository embarazoRepository;
    private final MedicoControlRepository medicoControlRepository;
    private final MotorRiesgoService motorRiesgoService;

    @Transactional
    public ControlPrenatalResponse crearControl(ControlPrenatalRequest request) {
        Embarazo embarazo = embarazoRepository.findById(request.embarazoId())
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", request.embarazoId().toString()));

        if (embarazo.getEstado() != EstadoEmbarazo.ACTIVO) {
            throw new BusinessRuleException(
                    "PREGNANCY_NOT_ACTIVE",
                    "Solo se pueden registrar controles en embarazos activos"
            );
        }

        Medico medico = obtenerMedicoAutenticado();
        BigDecimal imc = calcularImc(request.pesoKg(), request.tallaCm());

        com.awki.riesgo.dto.ControlPrenatal controlRiesgo = new com.awki.riesgo.dto.ControlPrenatal(
                request.presionArterialSistolica(),
                request.presionArterialDiastolica(),
                mapProteinuria(request.proteinuria()),
                request.frecuenciaCardiacaFetal(),
                toDouble(request.hemoglobinaGdl()),
                toDouble(request.fiebre()),
                mapEdemas(request.edemas()),
                mapMovimientos(request.movimientosFetalesReporte()),
                Boolean.TRUE.equals(request.contracciones())
        );

        ResultadoRiesgo resultadoRiesgo = motorRiesgoService.evaluarDesdeControl(
                embarazo.getId(),
                controlRiesgo
        );

        ControlPrenatal control = new ControlPrenatal();
        control.setEmbarazo(embarazo);
        control.setMedicoId(medico.getId());
        control.setNumeroControl((int) controlPrenatalRepository.countByEmbarazo_Id(embarazo.getId()) + 1);
        control.setFechaControl(request.fechaControl());
        control.setSemanasGestacion(request.semanasGestacion());
        control.setPesoKg(request.pesoKg());
        control.setTallaCm(request.tallaCm());
        control.setImc(imc);
        control.setPresionArterialSistolica(request.presionArterialSistolica());
        control.setPresionArterialDiastolica(request.presionArterialDiastolica());
        control.setAlturaUterinaCm(request.alturaUterinaCm());
        control.setFrecuenciaCardiacaFetal(request.frecuenciaCardiacaFetal());
        control.setPresentacionFetal(request.presentacionFetal());
        control.setHemoglobinaGdl(request.hemoglobinaGdl());
        control.setProteinuria(request.proteinuria());
        control.setGlucosaMgdl(request.glucosaMgdl());
        control.setMovimientosFetalesReporte(request.movimientosFetalesReporte());
        control.setEdemas(request.edemas());
        control.setProximaCita(request.proximaCita());
        control.setNivelRiesgoCalculado(resultadoRiesgo.nivel());
        control.setObservacionesMedico(request.observacionesMedico());

        ControlPrenatal guardado = controlPrenatalRepository.save(control);

        return new ControlPrenatalResponse(
                guardado.getId(),
                guardado.getNivelRiesgoCalculado(),
                resultadoRiesgo.generarAlerta() ? resultadoRiesgo.criteriosActivos() : List.of(),
                guardado.getImc(),
                guardado.getSemanasGestacion()
        );
    }

    @Transactional(readOnly = true)
    public List<ControlPrenatalResumenResponse> listarPorEmbarazo(UUID embarazoId) {
        if (!embarazoRepository.existsById(embarazoId)) {
            throw new ResourceNotFoundException("Embarazo", embarazoId.toString());
        }

        return controlPrenatalRepository.findByEmbarazo_IdOrderByFechaControlDesc(embarazoId)
                .stream()
                .map(this::toResumenResponse)
                .toList();
    }

    private Medico obtenerMedicoAutenticado() {
        String usuarioId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();

        return medicoControlRepository.findByUsuarioId(UUID.fromString(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Médico", usuarioId));
    }

    private ControlPrenatalResumenResponse toResumenResponse(ControlPrenatal control) {
        return new ControlPrenatalResumenResponse(
                control.getId(),
                control.getFechaControl(),
                control.getSemanasGestacion(),
                control.getNumeroControl(),
                control.getNivelRiesgoCalculado(),
                control.getPresionArterialSistolica() + "/" + control.getPresionArterialDiastolica(),
                control.getPesoKg(),
                control.getHemoglobinaGdl()
        );
    }

    private BigDecimal calcularImc(BigDecimal pesoKg, BigDecimal tallaCm) {
        if (pesoKg == null || tallaCm == null || BigDecimal.ZERO.compareTo(tallaCm) == 0) {
            return null;
        }

        BigDecimal tallaMetros = tallaCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return pesoKg.divide(tallaMetros.multiply(tallaMetros), 2, RoundingMode.HALF_UP);
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private com.awki.riesgo.dto.Proteinuria mapProteinuria(ProteinuriaControl proteinuria) {
        if (proteinuria == null) {
            return null;
        }

        return switch (proteinuria) {
            case NEGATIVA, TRAZAS -> com.awki.riesgo.dto.Proteinuria.NEGATIVO;
            case UNA_CRUZ -> com.awki.riesgo.dto.Proteinuria.UNA_CRUZ;
            case DOS_CRUCES -> com.awki.riesgo.dto.Proteinuria.DOS_CRUCES;
            case TRES_CRUCES -> com.awki.riesgo.dto.Proteinuria.TRES_CRUCES;
        };
    }

    private TipoEdema mapEdemas(EdemasControl edemas) {
        if (edemas == null) {
            return null;
        }

        return switch (edemas) {
            case AUSENTES -> TipoEdema.NINGUNO;
            case MANOS, PIES -> TipoEdema.PIES;
            case CARA -> TipoEdema.CARA;
            case GENERALIZADO -> TipoEdema.GENERALIZADO;
        };
    }

    private com.awki.riesgo.dto.MovimientosFetalesReporte mapMovimientos(
            MovimientosFetalesControl movimientos
    ) {
        if (movimientos == null) {
            return null;
        }

        return switch (movimientos) {
            case PRESENTES_NORMALES -> com.awki.riesgo.dto.MovimientosFetalesReporte.NORMALES;
            case DISMINUIDOS -> com.awki.riesgo.dto.MovimientosFetalesReporte.DISMINUIDOS;
            case AUSENTES -> com.awki.riesgo.dto.MovimientosFetalesReporte.AUSENTES;
        };
    }
}