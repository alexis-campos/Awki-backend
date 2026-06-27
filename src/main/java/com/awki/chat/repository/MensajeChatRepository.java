package com.awki.chat.repository;

import com.awki.chat.entity.MensajeChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MensajeChatRepository extends JpaRepository<MensajeChat, UUID> {
    Page<MensajeChat> findByEmbarazoIdOrderByCreatedAtDesc(UUID embarazoId, Pageable pageable);
    List<MensajeChat> findTop50ByEmbarazoIdOrderByCreatedAtDesc(UUID embarazoId);
}
