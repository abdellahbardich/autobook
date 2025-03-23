package com.autobook.conversationservice.service;

import com.autobook.conversationservice.dto.ConversationDto;
import com.autobook.conversationservice.entity.Conversation;
import com.autobook.conversationservice.entity.Message;
import com.autobook.conversationservice.repository.ConversationRepository;
import com.autobook.conversationservice.repository.MessageRepository;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.websocket.SendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ConversationService conversationService;

    private Conversation conversation;
    private Message message;
    private Long userId;
    private ConversationDto.CreateConversationRequest createRequest;
    private ConversationDto.MessageRequest messageRequest;

    @BeforeEach
    void setUp() {
        userId = 1L;

        conversation = new Conversation();
        conversation.setConversationId(1L);
        conversation.setUserId(userId);
        conversation.setTitle("Test Conversation");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        message = new Message();
        message.setMessageId(1L);
        message.setConversation(conversation);
        message.setContent("Hello, world!");
        message.setRole(Message.MessageRole.USER);
        message.setCreatedAt(LocalDateTime.now());

        createRequest = new ConversationDto.CreateConversationRequest();
        createRequest.setTitle("Test Conversation");
        createRequest.setInitialMessage("Hello, world!");

        messageRequest = new ConversationDto.MessageRequest();
        messageRequest.setContent("New message");
        messageRequest.setRole(Message.MessageRole.USER);
    }

    @Test
    void createConversation_Success() {
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        ConversationDto.ConversationResponse result = conversationService.createConversation(userId, createRequest);

        assertThat(result).isNotNull();
        assertEquals(conversation.getConversationId(), result.getConversationId());
        assertEquals(conversation.getTitle(), result.getTitle());
        assertEquals(conversation.getCreatedAt(), result.getCreatedAt());
        assertEquals(conversation.getUpdatedAt(), result.getUpdatedAt());

        verify(conversationRepository).save(any(Conversation.class));
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void createConversation_WithNullTitle() {
        createRequest.setTitle(null);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        ConversationDto.ConversationResponse result = conversationService.createConversation(userId, createRequest);

        assertThat(result).isNotNull();
        assertEquals(conversation.getConversationId(), result.getConversationId());
        assertEquals(conversation.getTitle(), result.getTitle());

        verify(conversationRepository).save(any(Conversation.class));
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void getUserConversations_Success() {
        List<Conversation> conversations = Arrays.asList(conversation);
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(anyLong())).thenReturn(conversations);

        List<ConversationDto.ConversationResponse> result = conversationService.getUserConversations(userId);

        assertThat(result).isNotNull();
        assertEquals(1, result.size());
        assertEquals(conversation.getConversationId(), result.get(0).getConversationId());
        assertEquals(conversation.getTitle(), result.get(0).getTitle());
        assertEquals(conversation.getCreatedAt(), result.get(0).getCreatedAt());
        assertEquals(conversation.getUpdatedAt(), result.get(0).getUpdatedAt());

        verify(conversationRepository).findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Test
    void getUserConversations_EmptyList() {
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(anyLong())).thenReturn(List.of());

        List<ConversationDto.ConversationResponse> result = conversationService.getUserConversations(userId);

        assertThat(result).isNotNull();
        assertEquals(0, result.size());

        verify(conversationRepository).findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Test
    void getConversationDetails_Success() {
        List<Message> messages = Arrays.asList(message);
        when(conversationRepository.findByConversationIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(anyLong())).thenReturn(messages);

        ConversationDto.ConversationDetailResponse result = conversationService.getConversationDetails(userId, conversation.getConversationId());

        assertThat(result).isNotNull();
        assertEquals(conversation.getConversationId(), result.getConversationId());
        assertEquals(conversation.getTitle(), result.getTitle());
        assertEquals(conversation.getCreatedAt(), result.getCreatedAt());
        assertEquals(conversation.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(1, result.getMessages().size());
        assertEquals(message.getMessageId(), result.getMessages().get(0).getMessageId());
        assertEquals(message.getContent(), result.getMessages().get(0).getContent());
        assertEquals(message.getRole(), result.getMessages().get(0).getRole());
        assertEquals(message.getCreatedAt(), result.getMessages().get(0).getCreatedAt());

        verify(conversationRepository).findByConversationIdAndUserId(conversation.getConversationId(), userId);
        verify(messageRepository).findByConversationConversationIdOrderByCreatedAtAsc(conversation.getConversationId());
    }

    @Test
    void getConversationDetails_ConversationNotFound() {
        when(conversationRepository.findByConversationIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                conversationService.getConversationDetails(userId, 999L)
        );
        assertEquals("Conversation not found", exception.getMessage());

        verify(conversationRepository).findByConversationIdAndUserId(999L, userId);
        verify(messageRepository, never()).findByConversationConversationIdOrderByCreatedAtAsc(anyLong());
    }

    @Test
    void addMessage_Success() {
        when(conversationRepository.findByConversationIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        ConversationDto.MessageResponse result = conversationService.addMessage(userId, conversation.getConversationId(), messageRequest);

        assertThat(result).isNotNull();
        assertEquals(message.getMessageId(), result.getMessageId());
        assertEquals(message.getContent(), result.getContent());
        assertEquals(message.getRole(), result.getRole());
        assertEquals(message.getCreatedAt(), result.getCreatedAt());

        verify(conversationRepository).findByConversationIdAndUserId(conversation.getConversationId(), userId);
        verify(messageRepository).save(any(Message.class));
        verify(conversationRepository).save(conversation);
    }

    @Test
    void addMessage_ConversationNotFound() {
        when(conversationRepository.findByConversationIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                conversationService.addMessage(userId, 999L, messageRequest)
        );
        assertEquals("Conversation not found", exception.getMessage());

        verify(conversationRepository).findByConversationIdAndUserId(999L, userId);
        verify(messageRepository, never()).save(any(Message.class));
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void deleteConversation_Success() {
        when(conversationRepository.findByConversationIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(conversation));
        doNothing().when(conversationRepository).delete(any(Conversation.class));

        conversationService.deleteConversation(userId, conversation.getConversationId());

        verify(conversationRepository).findByConversationIdAndUserId(conversation.getConversationId(), userId);
        verify(conversationRepository).delete(conversation);
    }

    @Test
    void deleteConversation_ConversationNotFound() {
        when(conversationRepository.findByConversationIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                conversationService.deleteConversation(userId, 999L)
        );
        assertEquals("Conversation not found", exception.getMessage());

        verify(conversationRepository).findByConversationIdAndUserId(999L, userId);
        verify(conversationRepository, never()).delete(any(Conversation.class));
    }


}