package com.autobook.conversationservice.repository;

import com.autobook.conversationservice.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<Conversation> findByConversationIdAndUserId(Long conversationId, Long userId);
}
