package com.autobook.storage.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.autobook.storage.dto.TemplateDto;

public interface TemplateService {
    TemplateDto saveTemplate(MultipartFile file, String name, String description, String templateType, boolean isPremium);
    TemplateDto getTemplateById(Long id);
    List<TemplateDto> getAllActiveTemplates();
    List<TemplateDto> getTemplatesByTypeAndStatus(String templateType, String status);
    List<TemplateDto> getTemplatesByTypeStatusAndPremium(String templateType, String status, boolean isPremium);
    TemplateDto updateTemplate(Long id, String name, String description, String status, boolean isPremium);
    void deleteTemplate(Long id);
    byte[] downloadTemplate(Long id);
    String getTemplateDownloadUrl(Long id);
}