package com.awki.alerta.service;

import com.awki.alerta.dto.ContactoEmergenciaRequest;
import com.awki.alerta.dto.ContactoEmergenciaResponse;
import com.awki.alerta.entity.ContactoEmergencia;
import com.awki.alerta.repository.ContactoEmergenciaRepository;
import com.awki.auth.entity.Paciente;
import com.awki.auth.repository.PacienteRepository;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.exception.ResourceNotFoundException;
import com.awki.exception.TenantViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactoEmergenciaService {

    private final ContactoEmergenciaRepository contactoRepository;
    private final PacienteRepository pacienteRepository;

    @Transactional(readOnly = true)
    public List<ContactoEmergenciaResponse> listarContactos(UsuarioAutenticado user) {
        Paciente paciente = pacienteRepository.findByUsuario_Id(user.id())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", user.id().toString()));

        List<ContactoEmergencia> contactos = contactoRepository.findByPaciente_IdAndActivoTrue(paciente.getId());
        return contactos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContactoEmergenciaResponse agregarContacto(ContactoEmergenciaRequest request, UsuarioAutenticado user) {
        Paciente paciente = pacienteRepository.findByUsuario_Id(user.id())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", user.id().toString()));

        ContactoEmergencia contacto = new ContactoEmergencia();
        contacto.setPaciente(paciente);
        contacto.setNombre(request.nombre());
        contacto.setTelefono(request.telefono());
        contacto.setParentesco(request.parentesco());
        contacto.setCanalPreferido(request.canalPreferido());
        contacto.setActivo(true);

        ContactoEmergencia guardado = contactoRepository.save(contacto);
        log.info("Contacto de emergencia registrado: {} para paciente: {}", guardado.getId(), paciente.getId());

        return toResponse(guardado);
    }

    @Transactional
    public void eliminarContacto(UUID contactoId, UsuarioAutenticado user) {
        Paciente paciente = pacienteRepository.findByUsuario_Id(user.id())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", user.id().toString()));

        ContactoEmergencia contacto = contactoRepository.findById(contactoId)
                .orElseThrow(() -> new ResourceNotFoundException("ContactoEmergencia", contactoId.toString()));

        if (!contacto.getPaciente().getId().equals(paciente.getId())) {
            throw new TenantViolationException("El contacto de emergencia no pertenece a la paciente autenticada");
        }

        // Soft delete
        contacto.setActivo(false);
        contactoRepository.save(contacto);
        log.info("Contacto de emergencia eliminado (inactivo): {} para paciente: {}", contactoId, paciente.getId());
    }

    private ContactoEmergenciaResponse toResponse(ContactoEmergencia c) {
        return new ContactoEmergenciaResponse(
                c.getId(),
                c.getNombre(),
                c.getTelefono(),
                c.getParentesco(),
                c.getCanalPreferido().name(),
                c.isActivo()
        );
    }
}
