package com.autobook.conversationservice.service;

import com.autobook.conversationservice.dto.ConversationDto;
import com.autobook.conversationservice.entity.Conversation;
import com.autobook.conversationservice.entity.Message;
import com.autobook.conversationservice.repository.ConversationRepository;
import com.autobook.conversationservice.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public ConversationDto.ConversationResponse createConversation(Long userId, ConversationDto.CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(request.getTitle() != null ? request.getTitle() : "New Conversation");

        Conversation savedConversation = conversationRepository.save(conversation);

        Message message = new Message();
        message.setConversation(savedConversation);
        message.setContent(request.getInitialMessage());
        message.setRole(Message.MessageRole.USER);
        messageRepository.save(message);

        return new ConversationDto.ConversationResponse(
                savedConversation.getConversationId(),
                savedConversation.getTitle(),
                savedConversation.getCreatedAt(),
                savedConversation.getUpdatedAt()
        );
    }

    public List<ConversationDto.ConversationResponse> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);

        return conversations.stream()
                .map(conversation -> new ConversationDto.ConversationResponse(
                        conversation.getConversationId(),
                        conversation.getTitle(),
                        conversation.getCreatedAt(),
                        conversation.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    public ConversationDto.ConversationDetailResponse getConversationDetails(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        List<Message> messages = messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(conversationId);

        List<ConversationDto.MessageResponse> messageResponses = messages.stream()
                .map(message -> new ConversationDto.MessageResponse(
                        message.getMessageId(),
                        message.getContent(),
                        message.getRole(),
                        message.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new ConversationDto.ConversationDetailResponse(
                conversation.getConversationId(),
                conversation.getTitle(),
                messageResponses,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    @Transactional
    public ConversationDto.MessageResponse addMessage(Long userId, Long conversationId, ConversationDto.MessageRequest messageRequest) {
        Conversation conversation = conversationRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setContent(messageRequest.getContent());
        message.setRole(messageRequest.getRole());

        Message savedMessage = messageRepository.save(message);

        conversationRepository.save(conversation);

        return new ConversationDto.MessageResponse(
                savedMessage.getMessageId(),
                savedMessage.getContent(),
                savedMessage.getRole(),
                savedMessage.getCreatedAt()
        );
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        conversationRepository.delete(conversation);
    }

    public void sendBookGenerationRequest(Long conversationId, Long messageId, Object bookRequest) {
        kafkaTemplate.send("book-generation-requests", conversationId.toString(), bookRequest);
    }
}