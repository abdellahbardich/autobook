package com.autobook.orchestration.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.autobook.orchestration.dto.PdfGenerationRequestDto;
import com.autobook.orchestration.exception.ServiceCommunicationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdfAssemblyClient {

    private final RestTemplate restTemplate;

    @Value("${service.pdf-assembly.url}")
    private String pdfAssemblyServiceUrl;

    public Object generatePdf(PdfGenerationRequestDto request) {
        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(
                    pdfAssemblyServiceUrl + "/generate-pdf",
                    request,
                    Object.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new ServiceCommunicationException("PDF assembly service returned: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error communicating with PDF assembly service: {}", e.getMessage());
            throw new ServiceCommunicationException("Failed to communicate with PDF assembly service", e);
        }
    }
}