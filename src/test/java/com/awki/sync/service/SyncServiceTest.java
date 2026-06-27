package com.awki.sync.service;

import com.awki.alerta.dto.CrearAlertaInternaRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock private DispositivoSyncRepository dispositivoSyncRepository;
    @Mock private SyncItemProcesadoRepository syncItemProcesadoRepository;
    @Mock private AuthService authService;
    @Mock private EmbarazoService embarazoService;
    @Mock private ChatService chatService;
    @Mock private AlertaService alertaService;

    @InjectMocks
    private SyncService syncService;

    private UUID usuarioId;
    private UUID pacienteId;
    private UUID embarazoId;
    private Paciente paciente;
    private Embarazo embarazo;
    private UsuarioAutenticado usuarioPaciente;
    private UsuarioAutenticado usuarioMedico;
    private static final String DEVICE_ID = "device-abc-123";

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
        embarazoId = UUID.randomUUID();

        paciente = new Paciente();
        ReflectionTestUtils.setField(paciente, "id", pacienteId);

        embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", embarazoId);
        embarazo.setPacienteId(pacienteId);

        usuarioPaciente = new UsuarioAutenticado(usuarioId, "PACIENTE", null);
        usuarioMedico = new UsuarioAutenticado(usuarioId, "MEDICO", UUID.randomUUID());
    }

    // ─── procesarBatch ───────────────────────────────────────────────────────

    @Test
    void procesarBatch_fallaConRolNoPermitido() {
        OfflineBatchRequest request = new OfflineBatchRequest(DEVICE_ID, List.of());
        assertThatThrownBy(() -> syncService.procesarBatch(request, usuarioMedico))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(authService);
    }

    @Test
    void procesarBatch_itemsVaciosRetornaResumenCero() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);

        OfflineBatchRequest request = new OfflineBatchRequest(DEVICE_ID, List.of());
        OfflineBatchResponse response = syncService.procesarBatch(request, usuarioPaciente);

        assertThat(response.procesados()).isZero();
        assertThat(response.descartados()).isZero();
        assertThat(response.conflictos()).isZero();
        assertThat(response.alertasGeneradasRetroactivamente()).isZero();
        verifyNoInteractions(dispositivoSyncRepository);
    }

    @Test
    void procesarBatch_itemMasAntiguoDe30DiasEsDescartado() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);

        LocalDateTime antiguo = LocalDateTime.now().minusDays(31);
        OfflineMensajeItem item = new OfflineMensajeItem(embarazoId, "consulta normal", antiguo);
        OfflineBatchRequest request = new OfflineBatchRequest(DEVICE_ID, List.of(item));

        OfflineBatchResponse response = syncService.procesarBatch(request, usuarioPaciente);

        assertThat(response.descartados()).isEqualTo(1);
        assertThat(response.procesados()).isZero();
        verifyNoInteractions(chatService, alertaService, syncItemProcesadoRepository);
    }

    @Test
    void procesarBatch_itemDuplicadoEsContadoComoConflicto() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);
        LocalDateTime ts = LocalDateTime.now().minusHours(1);
        when(syncItemProcesadoRepository.existsByDeviceIdAndPacienteIdAndOfflineTimestamp(
                DEVICE_ID, pacienteId, ts)).thenReturn(true);

        OfflineMensajeItem item = new OfflineMensajeItem(embarazoId, "mensaje duplicado", ts);
        OfflineBatchRequest request = new OfflineBatchRequest(DEVICE_ID, List.of(item));

        OfflineBatchResponse response = syncService.procesarBatch(request, usuarioPaciente);

        assertThat(response.conflictos()).isEqualTo(1);
        assertThat(response.procesados()).isZero();
        verifyNoInteractions(chatService, alertaService);
    }

    @Test
    void procesarBatch_itemValidoSinAlarmaSeProcesaSinAlerta() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);
        when(embarazoService.getEmbarazoEntityById(embarazoId)).thenReturn(embarazo);
        when(syncItemProcesadoRepository.existsByDeviceIdAndPacienteIdAndOfflineTimestamp(
                any(), any(), any())).thenReturn(false);

        MensajeChat mensajeGuardado = new MensajeChat();
        mensajeGuardado.setAlarmaProbable(false);
        when(chatService.guardarMensajeOffline(any(), anyString(), any(), anyString()))
                .thenReturn(mensajeGuardado);
        when(dispositivoSyncRepository.findByPacienteIdAndDeviceId(any(), anyString()))
                .thenReturn(Optional.empty());
        when(dispositivoSyncRepository.save(any())).thenReturn(new DispositivoSync());
        when(syncItemProcesadoRepository.save(any())).thenReturn(new SyncItemProcesado());

        LocalDateTime ts = LocalDateTime.now().minusMinutes(10);
        OfflineMensajeItem item = new OfflineMensajeItem(embarazoId, "me siento bien hoy", ts);
        OfflineBatchRequest request = new OfflineBatchRequest(DEVICE_ID, List.of(item));

        OfflineBatchResponse response = syncService.procesarBatch(request, usuarioPaciente);

        assertThat(response.procesados()).isEqualTo(1);
        assertThat(response.alertasGeneradasRetroactivamente()).isZero();
        verifyNoInteractions(alertaService);
    }

    @Test
    void procesarBatch_itemConAlarmaCreaAlertaRetroactiva() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);
        when(embarazoService.getEmbarazoEntityById(embarazoId)).thenReturn(embarazo);
        when(syncItemProcesadoRepository.existsByDeviceIdAndPacienteIdAndOfflineTimestamp(
                any(), any(), any())).thenReturn(false);

        MensajeChat mensajeConAlarma = new MensajeChat();
        mensajeConAlarma.setAlarmaProbable(true);
        when(chatService.guardarMensajeOffline(any(), anyString(), any(), anyString()))
                .thenReturn(mensajeConAlarma);
        when(dispositivoSyncRepository.findByPacienteIdAndDeviceId(any(), anyString()))
                .thenReturn(Optional.empty());
        when(dispositivoSyncRepository.save(any())).thenReturn(new DispositivoSync());
        when(syncItemProcesadoRepository.save(any())).thenReturn(new SyncItemProcesado());

        LocalDateTime ts = LocalDateTime.now().minusHours(2);
        OfflineMensajeItem item = new OfflineMensajeItem(embarazoId, "tengo sangrado fuerte", ts);
        OfflineBatchRequest request = new OfflineBatchRequest(DEVICE_ID, List.of(item));

        OfflineBatchResponse response = syncService.procesarBatch(request, usuarioPaciente);

        assertThat(response.procesados()).isEqualTo(1);
        assertThat(response.alertasGeneradasRetroactivamente()).isEqualTo(1);

        ArgumentCaptor<CrearAlertaInternaRequest> reqCaptor = ArgumentCaptor.forClass(CrearAlertaInternaRequest.class);
        ArgumentCaptor<LocalDateTime> tsCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(alertaService).crearAlertaRetroactivaDesdeSync(reqCaptor.capture(), tsCaptor.capture());

        CrearAlertaInternaRequest alertaReq = reqCaptor.getValue();
        assertThat(alertaReq.tipoAlerta()).isEqualTo(TipoAlerta.SINTOMA_CRITICO_IA);
        assertThat(alertaReq.origen()).isEqualTo(OrigenAlerta.SYNC_OFFLINE);
        assertThat(tsCaptor.getValue()).isEqualTo(ts);
    }

    @Test
    void procesarBatch_itemsOrdenadosPorTimestampAscendente() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);
        when(embarazoService.getEmbarazoEntityById(embarazoId)).thenReturn(embarazo);
        when(syncItemProcesadoRepository.existsByDeviceIdAndPacienteIdAndOfflineTimestamp(
                any(), any(), any())).thenReturn(false);

        MensajeChat mensajeSinAlarma = new MensajeChat();
        mensajeSinAlarma.setAlarmaProbable(false);
        when(chatService.guardarMensajeOffline(any(), anyString(), any(), anyString()))
                .thenReturn(mensajeSinAlarma);
        when(dispositivoSyncRepository.findByPacienteIdAndDeviceId(any(), anyString()))
                .thenReturn(Optional.empty());
        when(dispositivoSyncRepository.save(any())).thenReturn(new DispositivoSync());
        when(syncItemProcesadoRepository.save(any())).thenReturn(new SyncItemProcesado());

        LocalDateTime mas_tarde = LocalDateTime.now().minusMinutes(5);
        LocalDateTime mas_temprano = LocalDateTime.now().minusHours(3);
        OfflineMensajeItem item1 = new OfflineMensajeItem(embarazoId, "mensaje reciente", mas_tarde);
        OfflineMensajeItem item2 = new OfflineMensajeItem(embarazoId, "mensaje antiguo", mas_temprano);

        OfflineBatchRequest request = new OfflineBatchRequest(DEVICE_ID, List.of(item1, item2));
        OfflineBatchResponse response = syncService.procesarBatch(request, usuarioPaciente);

        assertThat(response.procesados()).isEqualTo(2);

        ArgumentCaptor<DispositivoSync> dsCaptor = ArgumentCaptor.forClass(DispositivoSync.class);
        verify(dispositivoSyncRepository).save(dsCaptor.capture());
        assertThat(dsCaptor.getValue().getUltimaSincronizacion()).isEqualTo(mas_tarde);
    }

    @Test
    void procesarBatch_embarazoDeOtroPacienteLanzaTenantViolation() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);
        when(syncItemProcesadoRepository.existsByDeviceIdAndPacienteIdAndOfflineTimestamp(
                any(), any(), any())).thenReturn(false);

        Embarazo embarazoAjeno = new Embarazo();
        ReflectionTestUtils.setField(embarazoAjeno, "id", embarazoId);
        embarazoAjeno.setPacienteId(UUID.randomUUID());
        when(embarazoService.getEmbarazoEntityById(embarazoId)).thenReturn(embarazoAjeno);

        LocalDateTime ts = LocalDateTime.now().minusMinutes(10);
        OfflineMensajeItem item = new OfflineMensajeItem(embarazoId, "mensaje", ts);
        OfflineBatchRequest request = new OfflineBatchRequest(DEVICE_ID, List.of(item));

        assertThatThrownBy(() -> syncService.procesarBatch(request, usuarioPaciente))
                .isInstanceOf(TenantViolationException.class);
    }

    @Test
    void procesarBatch_embarazoNoEncontradoLanzaNotFound() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);
        when(syncItemProcesadoRepository.existsByDeviceIdAndPacienteIdAndOfflineTimestamp(
                any(), any(), any())).thenReturn(false);
        when(embarazoService.getEmbarazoEntityById(embarazoId))
                .thenThrow(new ResourceNotFoundException("Embarazo", embarazoId.toString()));

        LocalDateTime ts = LocalDateTime.now().minusMinutes(10);
        OfflineMensajeItem item = new OfflineMensajeItem(embarazoId, "mensaje", ts);
        OfflineBatchRequest request = new OfflineBatchRequest(DEVICE_ID, List.of(item));

        assertThatThrownBy(() -> syncService.procesarBatch(request, usuarioPaciente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void procesarBatch_actualizaDispositivoExistente() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);
        when(embarazoService.getEmbarazoEntityById(embarazoId)).thenReturn(embarazo);
        when(syncItemProcesadoRepository.existsByDeviceIdAndPacienteIdAndOfflineTimestamp(
                any(), any(), any())).thenReturn(false);

        MensajeChat mensajeSinAlarma = new MensajeChat();
        mensajeSinAlarma.setAlarmaProbable(false);
        when(chatService.guardarMensajeOffline(any(), anyString(), any(), anyString()))
                .thenReturn(mensajeSinAlarma);
        when(syncItemProcesadoRepository.save(any())).thenReturn(new SyncItemProcesado());

        DispositivoSync existente = new DispositivoSync();
        existente.setPacienteId(pacienteId);
        existente.setDeviceId(DEVICE_ID);
        existente.setUltimaSincronizacion(LocalDateTime.now().minusDays(1));
        when(dispositivoSyncRepository.findByPacienteIdAndDeviceId(pacienteId, DEVICE_ID))
                .thenReturn(Optional.of(existente));
        when(dispositivoSyncRepository.save(any())).thenReturn(existente);

        LocalDateTime ts = LocalDateTime.now().minusMinutes(5);
        OfflineBatchRequest request = new OfflineBatchRequest(DEVICE_ID,
                List.of(new OfflineMensajeItem(embarazoId, "texto ok", ts)));

        syncService.procesarBatch(request, usuarioPaciente);

        ArgumentCaptor<DispositivoSync> captor = ArgumentCaptor.forClass(DispositivoSync.class);
        verify(dispositivoSyncRepository).save(captor.capture());
        assertThat(captor.getValue().getUltimaSincronizacion()).isEqualTo(ts);
    }

    // ─── obtenerEstado ────────────────────────────────────────────────────────

    @Test
    void obtenerEstado_fallaConRolNoPermitido() {
        assertThatThrownBy(() -> syncService.obtenerEstado(DEVICE_ID, usuarioMedico))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(authService);
    }

    @Test
    void obtenerEstado_deviceSinRegistroRetornaNullTimestamp() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);
        when(dispositivoSyncRepository.findByPacienteIdAndDeviceId(pacienteId, DEVICE_ID))
                .thenReturn(Optional.empty());

        SyncEstadoResponse response = syncService.obtenerEstado(DEVICE_ID, usuarioPaciente);

        assertThat(response.deviceId()).isEqualTo(DEVICE_ID);
        assertThat(response.ultimaSincronizacion()).isNull();
    }

    @Test
    void obtenerEstado_deviceConRegistroRetornaTimestamp() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId)).thenReturn(paciente);

        LocalDateTime ultimaSync = LocalDateTime.now().minusHours(5);
        DispositivoSync ds = new DispositivoSync();
        ds.setDeviceId(DEVICE_ID);
        ds.setPacienteId(pacienteId);
        ds.setUltimaSincronizacion(ultimaSync);
        when(dispositivoSyncRepository.findByPacienteIdAndDeviceId(pacienteId, DEVICE_ID))
                .thenReturn(Optional.of(ds));

        SyncEstadoResponse response = syncService.obtenerEstado(DEVICE_ID, usuarioPaciente);

        assertThat(response.deviceId()).isEqualTo(DEVICE_ID);
        assertThat(response.ultimaSincronizacion()).isEqualTo(ultimaSync);
    }

    @Test
    void obtenerEstado_pacienteNoEncontradoLanzaNotFound() {
        when(authService.getPacienteEntityByUsuarioId(usuarioId))
                .thenThrow(new ResourceNotFoundException("Paciente para usuario", usuarioId.toString()));

        assertThatThrownBy(() -> syncService.obtenerEstado(DEVICE_ID, usuarioPaciente))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
