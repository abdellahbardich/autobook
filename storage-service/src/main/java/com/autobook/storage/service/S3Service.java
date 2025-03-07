package com.autobook.storage.service;

import java.io.InputStream;
import java.net.URL;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
    String uploadFile(String key, MultipartFile file);
    String uploadFile(String key, InputStream inputStream, String contentType);
    byte[] downloadFile(String key);
    void deleteFile(String key);
    URL generatePresignedUrl(String key, long expirationInMinutes);
}