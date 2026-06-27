package com.awki.riesgo.service;

import com.awki.common.enums.NivelRiesgo;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.repository.EmbarazoRepository;
import com.awki.exception.ResourceNotFoundException;
import com.awki.riesgo.dto.ReporteSintomaRequest;
import com.awki.riesgo.dto.ReporteSintomaResponse;
import com.awki.riesgo.dto.ResultadoRiesgo;
import com.awki.riesgo.dto.SintomaDetectado;
import com.awki.riesgo.entity.ReporteSintoma;
import com.awki.riesgo.repository.ReporteSintomaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteSintomaService {

    private final ReporteSintomaRepository repository;
    private final EmbarazoRepository embarazoRepository;
    private final MotorRiesgoService motorRiesgoService;

    @Transactional
    public ReporteSintomaResponse registrarReporte(ReporteSintomaRequest req) {
        Embarazo embarazo = embarazoRepository.findById(req.embarazoId())
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", req.embarazoId().toString()));

        List<SintomaDetectado> sintomasAEnviar = new ArrayList<>();
        if (req.sintomasClinicos() != null) {
            sintomasAEnviar = req.sintomasClinicos().stream()
                    .map(SintomaDetectado::new)
                    .collect(Collectors.toList());
        }

        // Evaluar riesgo si hay síntomas clínicos
        boolean esCritico = false;
        if (!sintomasAEnviar.isEmpty()) {
            ResultadoRiesgo res = motorRiesgoService.evaluarDesdeSintomas(req.embarazoId(), sintomasAEnviar).join();
            if (res.nivel() == NivelRiesgo.ROJO || res.nivel() == NivelRiesgo.AMARILLO) {
                esCritico = true;
            }
        }

        ReporteSintoma reporte = new ReporteSintoma();
        reporte.setEmbarazo(embarazo);
        reporte.setBienestar(req.bienestar());
        reporte.setMovimientos(req.movimientos());
        reporte.setHinchazon(req.hinchazon());
        reporte.setSintomasDetalle(req.sintomasDetalle());
        reporte.setNotas(req.notas());
        reporte.setEsCritico(esCritico);
        
        List<String> clinicosStrs = req.sintomasClinicos() != null ?
                req.sintomasClinicos().stream().map(Enum::name).collect(Collectors.toList()) : 
                new ArrayList<>();
        reporte.setSintomasClinicos(clinicosStrs);

        reporte = repository.save(reporte);

        return mapToResponse(reporte);
    }

    @Transactional(readOnly = true)
    public List<ReporteSintomaResponse> listarHistorial(UUID embarazoId) {
        return repository.findByEmbarazoIdOrderByCreatedAtDesc(embarazoId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ReporteSintomaResponse mapToResponse(ReporteSintoma r) {
        return new ReporteSintomaResponse(
                r.getId(),
                r.getEmbarazo().getId(),
                r.getBienestar(),
                r.getMovimientos(),
                r.getHinchazon(),
                r.getSintomasDetalle(),
                r.getNotas(),
                r.isEsCritico(),
                r.getSintomasClinicos(),
                r.getCreatedAt()
        );
    }
}
