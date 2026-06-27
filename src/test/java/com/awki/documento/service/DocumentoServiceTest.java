package com.awki.documento.service;

import com.awki.auth.entity.Paciente;
import com.awki.common.enums.RolUsuario;
import com.awki.documento.entity.TipoDocumento;
import com.awki.documento.repository.DocumentoRepository;
import com.awki.documento.repository.MedicoDocumentoRepository;
import com.awki.documento.repository.PacienteDocumentoRepository;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.service.EmbarazoService;
import com.awki.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private EmbarazoService embarazoService;

    @Mock
    private PacienteDocumentoRepository pacienteRepository;

    @Mock
    private MedicoDocumentoRepository medicoRepository;

    @Mock
    private DocumentoStorageService storageService;

    @InjectMocks
    private DocumentoService documentoService;

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void subirDocumento_conMimeNoPermitido_debeLanzarBusinessRuleException() {
        UUID embarazoId = UUID.randomUUID();

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "documento.txt",
                "text/plain",
                "contenido".getBytes()
        );

        assertThatThrownBy(() -> documentoService.subirDocumento(
                embarazoId,
                TipoDocumento.OTRO,
                archivo
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Solo se permiten archivos PDF, JPG o PNG")
                .extracting("errorCode")
                .isEqualTo("DOCUMENT_FILE_TYPE_NOT_ALLOWED");

        verifyNoInteractions(documentoRepository);
        verifyNoInteractions(embarazoService);
    }

    @Test
    void subirDocumento_conArchivoMayorA10MB_debeLanzarBusinessRuleException() {
        UUID embarazoId = UUID.randomUUID();

        MockMultipartFile archivo = mock(MockMultipartFile.class);
        when(archivo.isEmpty()).thenReturn(false);
        when(archivo.getSize()).thenReturn(10L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> documentoService.subirDocumento(
                embarazoId,
                TipoDocumento.ECOGRAFIA,
                archivo
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El archivo no debe superar 10MB")
                .extracting("errorCode")
                .isEqualTo("DOCUMENT_FILE_TOO_LARGE");

        verifyNoInteractions(documentoRepository);
        verifyNoInteractions(embarazoService);
    }

    @Test
    void subirDocumento_conPdfValido_debeGuardarMetadata() {
        UUID usuarioId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        UUID embarazoId = UUID.randomUUID();

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "ecografia.pdf",
                "application/pdf",
                "contenido".getBytes()
        );

        Embarazo embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", embarazoId);
        embarazo.setPacienteId(pacienteId);

        Paciente paciente = new Paciente();
        ReflectionTestUtils.setField(paciente, "id", pacienteId);

        autenticar(usuarioId, RolUsuario.PACIENTE.name());

        when(embarazoService.getEmbarazoEntityById(embarazoId)).thenReturn(embarazo);
        when(pacienteRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(paciente));
        when(storageService.subir(archivo, embarazoId)).thenReturn("documentos/key.pdf");
        when(documentoRepository.save(any())).thenAnswer(invocation -> {
            com.awki.documento.entity.DocumentoClinico documento = invocation.getArgument(0);
            ReflectionTestUtils.setField(documento, "id", UUID.randomUUID());
            return documento;
        });

        documentoService.subirDocumento(embarazoId, TipoDocumento.ECOGRAFIA, archivo);

        verify(documentoRepository).save(argThat(documento ->
                documento.getEmbarazo().getId().equals(embarazoId)
                        && documento.getTipoDocumento() == TipoDocumento.ECOGRAFIA
                        && documento.getNombreArchivo().equals("ecografia.pdf")
                        && documento.getContentType().equals("application/pdf")
                        && documento.getStorageKey().equals("documentos/key.pdf")
        ));
    }

    private void autenticar(UUID usuarioId, String rol) {
        var authentication = new UsernamePasswordAuthenticationToken(
                usuarioId.toString(),
                null,
                List.of(() -> "ROLE_" + rol)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
