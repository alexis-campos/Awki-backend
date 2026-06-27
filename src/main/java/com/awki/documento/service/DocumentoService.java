package com.awki.documento.service;

import com.awki.auth.entity.Medico;
import com.awki.auth.entity.Paciente;
import com.awki.common.enums.RolUsuario;
import com.awki.documento.dto.DocumentoDeleteResponse;
import com.awki.documento.dto.DocumentoResponse;
import com.awki.documento.dto.DocumentoUrlResponse;
import com.awki.documento.entity.DocumentoClinico;
import com.awki.documento.entity.SubidoPor;
import com.awki.documento.entity.TipoDocumento;
import com.awki.documento.repository.DocumentoRepository;
import com.awki.documento.repository.MedicoDocumentoRepository;
import com.awki.documento.repository.PacienteDocumentoRepository;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.service.EmbarazoService;
import com.awki.exception.BusinessRuleException;
import com.awki.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> MIME_TYPES_PERMITIDOS = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final DocumentoRepository documentoRepository;
    private final EmbarazoService embarazoService;
    private final PacienteDocumentoRepository pacienteRepository;
    private final MedicoDocumentoRepository medicoRepository;
    private final DocumentoStorageService storageService;

    @Transactional
    public DocumentoResponse subirDocumento(UUID embarazoId, TipoDocumento tipoDocumento, MultipartFile archivo) {
        validarArchivo(archivo);

        Embarazo embarazo = embarazoService.getEmbarazoEntityById(embarazoId);

        UsuarioActual usuarioActual = obtenerUsuarioActual();
        validarAccesoEmbarazo(embarazo, usuarioActual);

        String storageKey = storageService.subir(archivo, embarazoId);

        DocumentoClinico documento = new DocumentoClinico();
        documento.setEmbarazo(embarazo);
        documento.setSubidoPor(usuarioActual.subidoPor());
        documento.setSubidoPorId(usuarioActual.perfilId());
        documento.setTipoDocumento(tipoDocumento);
        documento.setNombreArchivo(archivo.getOriginalFilename());
        documento.setContentType(archivo.getContentType());
        documento.setTamanoBytes(archivo.getSize());
        documento.setStorageKey(storageKey);

        return toResponse(documentoRepository.save(documento));
    }

    @Transactional(readOnly = true)
    public Page<DocumentoResponse> listarDocumentos(UUID embarazoId, int page, int size) {
        Embarazo embarazo = embarazoService.getEmbarazoEntityById(embarazoId);

        validarAccesoEmbarazo(embarazo, obtenerUsuarioActual());

        return documentoRepository.findByEmbarazo_IdAndEliminadoFalse(
                embarazoId,
                PageRequest.of(Math.max(page, 0), Math.min(size, 100))
        ).map(this::toResponse);
    }

    @Transactional
    public DocumentoDeleteResponse eliminarDocumento(UUID documentoId) {
        DocumentoClinico documento = documentoRepository.findByIdAndEliminadoFalse(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento", documentoId.toString()));

        UsuarioActual usuarioActual = obtenerUsuarioActual();
        validarAccesoEmbarazo(documento.getEmbarazo(), usuarioActual);
        validarPermisoEliminar(documento, usuarioActual);

        documento.setEliminado(true);
        documentoRepository.save(documento);

        return new DocumentoDeleteResponse(
                "Documento eliminado correctamente",
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public DocumentoUrlResponse generarUrl(UUID documentoId) {
        DocumentoClinico documento = documentoRepository.findByIdAndEliminadoFalse(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento", documentoId.toString()));

        validarAccesoEmbarazo(documento.getEmbarazo(), obtenerUsuarioActual());

        return new DocumentoUrlResponse(
                storageService.generarUrlFirmada(documento.getStorageKey()),
                storageService.calcularExpiracionUrl()
        );
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessRuleException("DOCUMENT_FILE_REQUIRED", "El archivo es obligatorio");
        }

        if (archivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException("DOCUMENT_FILE_TOO_LARGE", "El archivo no debe superar 10MB");
        }

        if (!MIME_TYPES_PERMITIDOS.contains(archivo.getContentType())) {
            throw new BusinessRuleException("DOCUMENT_FILE_TYPE_NOT_ALLOWED", "Solo se permiten archivos PDF, JPG o PNG");
        }
    }

    private void validarAccesoEmbarazo(Embarazo embarazo, UsuarioActual usuarioActual) {
        if (usuarioActual.rol() == RolUsuario.PACIENTE && !embarazo.getPacienteId().equals(usuarioActual.perfilId())) {
            throw new BusinessRuleException("DOCUMENT_ACCESS_DENIED", "No tienes acceso a documentos de este embarazo");
        }

        // Validación mínima para hackatón: el médico autenticado puede operar sobre embarazos vinculados.
        // Cuando el módulo de vinculación exponga un validador, este punto se reemplaza por esa regla.
        if (usuarioActual.rol() == RolUsuario.MEDICO && embarazo.getMedicoId() != null
                && !embarazo.getMedicoId().equals(usuarioActual.perfilId())) {
            throw new BusinessRuleException("DOCUMENT_ACCESS_DENIED", "No tienes acceso a documentos de este embarazo");
        }
    }

    private void validarPermisoEliminar(DocumentoClinico documento, UsuarioActual usuarioActual) {
        if (usuarioActual.rol() == RolUsuario.PACIENTE
                && (!documento.getSubidoPorId().equals(usuarioActual.perfilId())
                || documento.getSubidoPor() != SubidoPor.PACIENTE)) {
            throw new BusinessRuleException("DOCUMENT_DELETE_FORBIDDEN", "La paciente solo puede eliminar documentos que subió");
        }
    }

    private UsuarioActual obtenerUsuarioActual() {
        String usuarioId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();

        String rolTexto = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("ROLE_NOT_FOUND", "No se encontró el rol del usuario"))
                .getAuthority()
                .replace("ROLE_", "");

        RolUsuario rol = RolUsuario.valueOf(rolTexto);
        UUID usuarioUuid = UUID.fromString(usuarioId);

        if (rol == RolUsuario.PACIENTE) {
            Paciente paciente = pacienteRepository.findByUsuarioId(usuarioUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente", usuarioId));

            return new UsuarioActual(rol, paciente.getId(), SubidoPor.PACIENTE);
        }

        if (rol == RolUsuario.MEDICO) {
            Medico medico = medicoRepository.findByUsuarioId(usuarioUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Médico", usuarioId));

            return new UsuarioActual(rol, medico.getId(), SubidoPor.MEDICO);
        }

        throw new BusinessRuleException("ROLE_NOT_ALLOWED", "Solo pacientes y médicos pueden gestionar documentos");
    }

    private DocumentoResponse toResponse(DocumentoClinico documento) {
        return new DocumentoResponse(
                documento.getId(),
                documento.getEmbarazo().getId(),
                documento.getSubidoPor(),
                documento.getSubidoPorId(),
                documento.getTipoDocumento(),
                documento.getNombreArchivo(),
                documento.getContentType(),
                documento.getTamanoBytes(),
                documento.getCreatedAt()
        );
    }

    private record UsuarioActual(
            RolUsuario rol,
            UUID perfilId,
            SubidoPor subidoPor
    ) {}
}