package com.autobook.orchestration.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.autobook.orchestration.exception.ServiceCommunicationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageClient {

    private final RestTemplate restTemplate;

    @Value("${service.storage.url}")
    private String storageServiceUrl;

    public Object uploadBookFile(byte[] file, String title, String description, Long userId, Long bookId, String fileType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(file) {
                @Override
                public String getFilename() {
                    return title.replace(" ", "_") + ".pdf";
                }
            };

            body.add("file", resource);
            body.add("title", title);
            body.add("description", description);
            body.add("userId", userId.toString());
            body.add("bookId", bookId.toString());
            body.add("fileType", fileType);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Object> response = restTemplate.postForEntity(
                    storageServiceUrl + "/book-files",
                    requestEntity,
                    Object.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                return response.getBody();
            } else {
                throw new ServiceCommunicationException("Storage service returned: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error communicating with storage service: {}", e.getMessage());
            throw new ServiceCommunicationException("Failed to communicate with storage service", e);
        }
    }

    public Object getBookFileByBookIdAndType(Long bookId, String fileType) {
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                    storageServiceUrl + "/book-files/book/" + bookId + "/type/" + fileType,
                    Object.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new ServiceCommunicationException("Storage service returned: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error communicating with storage service: {}", e.getMessage());
            throw new ServiceCommunicationException("Failed to communicate with storage service", e);
        }
    }
}