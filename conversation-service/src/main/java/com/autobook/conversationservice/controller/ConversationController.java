package com.autobook.conversationservice.controller;

import com.autobook.conversationservice.dto.ConversationDto;
import com.autobook.conversationservice.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationDto.ConversationResponse> createConversation(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @Valid @RequestBody ConversationDto.CreateConversationRequest request) {
        return new ResponseEntity<>(conversationService.createConversation(userId, request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ConversationDto.ConversationResponse>> getUserConversations(
            @RequestHeader("X-Auth-User-Id") Long userId) {
        return ResponseEntity.ok(conversationService.getUserConversations(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDto.ConversationDetailResponse> getConversationDetails(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long conversationId) {
        return ResponseEntity.ok(conversationService.getConversationDetails(userId, conversationId));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ConversationDto.MessageResponse> addMessage(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long conversationId,
            @Valid @RequestBody ConversationDto.MessageRequest messageRequest) {
        return new ResponseEntity<>(conversationService.addMessage(userId, conversationId, messageRequest), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("id") Long conversationId) {
        conversationService.deleteConversation(userId, conversationId);
        return ResponseEntity.noContent().build();
    }
}