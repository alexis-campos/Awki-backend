package com.awki.sync.service;

import com.awki.alerta.dto.CrearAlertaInternaRequest;
import com.awki.alerta.entity.NivelUrgencia;
import com.awki.alerta.entity.OrigenAlerta;
import com.awki.alerta.entity.TipoAlerta;
import com.awki.alerta.service.AlertaService;
import com.awki.auth.entity.Paciente;
import com.awki.auth.service.AuthService;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.chat.entity.MensajeChat;
import com.awki.chat.service.ChatService;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.service.EmbarazoService;
import com.awki.exception.BusinessRuleException;
import com.awki.exception.ResourceNotFoundException;
import com.awki.exception.TenantViolationException;
import com.awki.sync.dto.OfflineBatchRequest;
import com.awki.sync.dto.OfflineBatchResponse;
import com.awki.sync.dto.OfflineMensajeItem;
import com.awki.sync.dto.SyncEstadoResponse;
import com.awki.sync.entity.DispositivoSync;
import com.awki.sync.entity.SyncItemProcesado;
import com.awki.sync.repository.DispositivoSyncRepository;
import com.awki.sync.repository.SyncItemProcesadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private static final int DIAS_LIMITE_OFFLINE = 30;

    private final DispositivoSyncRepository dispositivoSyncRepository;
    private final SyncItemProcesadoRepository syncItemProcesadoRepository;
    private final AuthService authService;
    private final EmbarazoService embarazoService;
    private final ChatService chatService;
    private final AlertaService alertaService;

    @Transactional
    public OfflineBatchResponse procesarBatch(OfflineBatchRequest request, UsuarioAutenticado usuario) {
        if (!"PACIENTE".equals(usuario.rol())) {
            throw new BusinessRuleException("FORBIDDEN", "Solo las pacientes pueden enviar batches offline");
        }

        Paciente paciente = authService.getPacienteEntityByUsuarioId(usuario.id());

        List<OfflineMensajeItem> itemsOrdenados = request.items().stream()
                .sorted(Comparator.comparing(OfflineMensajeItem::offlineTimestamp))
                .toList();

        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_LIMITE_OFFLINE);

        int procesados = 0;
        int descartados = 0;
        int conflictos = 0;
        int alertasRetroactivas = 0;
        LocalDateTime ultimoTimestamp = null;

        for (OfflineMensajeItem item : itemsOrdenados) {
            if (item.offlineTimestamp().isBefore(limite)) {
                log.debug("[Sync] Item descartado por antigüedad: deviceId={} ts={}", request.deviceId(), item.offlineTimestamp());
                descartados++;
                continue;
            }

            if (syncItemProcesadoRepository.existsByDeviceIdAndPacienteIdAndOfflineTimestamp(
                    request.deviceId(), paciente.getId(), item.offlineTimestamp())) {
                log.debug("[Sync] Item duplicado ignorado: deviceId={} ts={}", request.deviceId(), item.offlineTimestamp());
                conflictos++;
                continue;
            }

            Embarazo embarazo = embarazoService.getEmbarazoEntityById(item.embarazoId());

            if (!embarazo.getPacienteId().equals(paciente.getId())) {
                throw new TenantViolationException("El embarazo no pertenece a la paciente autenticada");
            }

            MensajeChat mensaje = chatService.guardarMensajeOffline(
                    item.embarazoId(), item.contenido(), item.offlineTimestamp(), request.deviceId());

            try {
                SyncItemProcesado sip = new SyncItemProcesado();
                sip.setDeviceId(request.deviceId());
                sip.setPacienteId(paciente.getId());
                sip.setOfflineTimestamp(item.offlineTimestamp());
                syncItemProcesadoRepository.save(sip);
            } catch (DataIntegrityViolationException e) {
                // Concurrencia: ya fue procesado por otra request simultánea — idempotente
                log.debug("[Sync] Item ya registrado por concurrencia: deviceId={} ts={}", request.deviceId(), item.offlineTimestamp());
                conflictos++;
                continue;
            }

            if (mensaje.isAlarmaProbable()) {
                String descripcion = "Síntoma crítico detectado offline: " + truncar(item.contenido(), 200);
                alertaService.crearAlertaRetroactivaDesdeSync(
                        new CrearAlertaInternaRequest(
                                item.embarazoId(),
                                TipoAlerta.SINTOMA_CRITICO_IA,
                                NivelUrgencia.ROJO,
                                descripcion,
                                List.of("SINTOMA_CRITICO_IA_OFFLINE"),
                                OrigenAlerta.SYNC_OFFLINE
                        ),
                        item.offlineTimestamp()
                );
                alertasRetroactivas++;
            }

            procesados++;
            if (ultimoTimestamp == null || item.offlineTimestamp().isAfter(ultimoTimestamp)) {
                ultimoTimestamp = item.offlineTimestamp();
            }
        }

        if (ultimoTimestamp != null) {
            final LocalDateTime ts = ultimoTimestamp;
            DispositivoSync ds = dispositivoSyncRepository
                    .findByPacienteIdAndDeviceId(paciente.getId(), request.deviceId())
                    .orElseGet(() -> {
                        DispositivoSync nuevo = new DispositivoSync();
                        nuevo.setPacienteId(paciente.getId());
                        nuevo.setDeviceId(request.deviceId());
                        return nuevo;
                    });
            ds.setUltimaSincronizacion(ts);
            dispositivoSyncRepository.save(ds);
        }

        log.info("[Sync] Batch completado. deviceId={} procesados={} descartados={} conflictos={} alertasRetroactivas={}",
                request.deviceId(), procesados, descartados, conflictos, alertasRetroactivas);

        return new OfflineBatchResponse(procesados, descartados, conflictos, alertasRetroactivas);
    }

    @Transactional(readOnly = true)
    public SyncEstadoResponse obtenerEstado(String deviceId, UsuarioAutenticado usuario) {
        if (!"PACIENTE".equals(usuario.rol())) {
            throw new BusinessRuleException("FORBIDDEN", "Solo las pacientes pueden consultar el estado de sincronización");
        }

        Paciente paciente = authService.getPacienteEntityByUsuarioId(usuario.id());

        LocalDateTime ultimaSincronizacion = dispositivoSyncRepository
                .findByPacienteIdAndDeviceId(paciente.getId(), deviceId)
                .map(DispositivoSync::getUltimaSincronizacion)
                .orElse(null);

        return new SyncEstadoResponse(deviceId, ultimaSincronizacion);
    }

    private String truncar(String texto, int maxLen) {
        if (texto == null) return "";
        return texto.length() <= maxLen ? texto : texto.substring(0, maxLen - 3) + "...";
    }
}
