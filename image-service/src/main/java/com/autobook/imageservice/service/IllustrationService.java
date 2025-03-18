package com.autobook.imageservice.service;

import com.autobook.imageservice.client.ConsistoryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IllustrationService {

    private final ConsistoryClient consistoryClient;

    public List<Map<String, String>> generateIllustrations(Map<String, Object> request) {
        try {
            String prompt = (String) request.get("prompt");
            String style = (String) request.get("style");

            if (prompt == null || prompt.isEmpty()) {
                throw new IllegalArgumentException("Prompt is required for illustration generation");
            }

            String subjectPrompt = extractSubjectFromPrompt(prompt);

            List<String> subjectTokens = extractSubjectTokens(subjectPrompt);

            String scenePrompt1 = prompt;
            String scenePrompt2 = style != null ?
                    prompt + " in " + style + " style" : prompt;

            List<String> imagePaths = consistoryClient.generateConsistentImages(
                    subjectPrompt, subjectTokens, style, scenePrompt1, scenePrompt2);

            return imagePaths.stream()
                    .map(path -> Map.of("illustrationPath", path))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error generating illustrations", e);
            throw new RuntimeException("Failed to generate illustrations", e);
        }
    }

    private String extractSubjectFromPrompt(String prompt) {
        String[] words = prompt.split("\\s+");
        List<String> nouns = new ArrayList<>();

        for (String word : words) {
            if (word.length() > 1 && Character.isUpperCase(word.charAt(0))) {
                nouns.add(word.replaceAll("[^a-zA-Z]", ""));
            }
        }

        if (!nouns.isEmpty()) {
            return String.join(" ", nouns);
        }

        int endIndex = Math.min(10, words.length);
        return String.join(" ", Arrays.copyOfRange(words, 0, endIndex));
    }

    private List<String> extractSubjectTokens(String subjectPrompt) {
        return Arrays.asList(subjectPrompt.split("\\s+"));
    }
}