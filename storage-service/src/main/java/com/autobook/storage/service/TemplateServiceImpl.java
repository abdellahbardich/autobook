package com.autobook.storage.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.autobook.storage.dto.TemplateDto;
import com.autobook.storage.exception.ResourceNotFoundException;
import com.autobook.storage.model.Template;
import com.autobook.storage.repository.TemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public TemplateDto saveTemplate(MultipartFile file, String name, String description, String templateType, boolean isPremium) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String s3Key = "templates/" + templateType + "/" + UUID.randomUUID() + extension;

        s3Service.uploadFile(s3Key, file);

        Template template = Template.builder()
                .name(name)
                .s3Key(s3Key)
                .description(description)
                .templateType(templateType)
                .status("ACTIVE")
                .isPremium(isPremium)
                .build();

        Template savedTemplate = templateRepository.save(template);
        return mapToTemplateDto(savedTemplate);
    }

    @Override
    public TemplateDto getTemplateById(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));
        return mapToTemplateDto(template);
    }

    @Override
    public List<TemplateDto> getAllActiveTemplates() {
        return templateRepository.findByStatus("ACTIVE").stream()
                .map(this::mapToTemplateDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateDto> getTemplatesByTypeAndStatus(String templateType, String status) {
        return templateRepository.findByTemplateTypeAndStatus(templateType, status).stream()
                .map(this::mapToTemplateDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateDto> getTemplatesByTypeStatusAndPremium(String templateType, String status, boolean isPremium) {
        return templateRepository.findByTemplateTypeAndStatusAndIsPremium(templateType, status, isPremium).stream()
                .map(this::mapToTemplateDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TemplateDto updateTemplate(Long id, String name, String description, String status, boolean isPremium) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        template.setName(name);
        template.setDescription(description);
        template.setStatus(status);
        template.setPremium(isPremium);

        Template updatedTemplate = templateRepository.save(template);
        return mapToTemplateDto(updatedTemplate);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        s3Service.deleteFile(template.getS3Key());
        templateRepository.deleteById(id);
    }

    @Override
    public byte[] downloadTemplate(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        return s3Service.downloadFile(template.getS3Key());
    }

    @Override
    public String getTemplateDownloadUrl(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        return s3Service.generatePresignedUrl(template.getS3Key(), 60).toString();  // 1 hour expiration
    }

    private TemplateDto mapToTemplateDto(Template template) {
        return TemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .s3Key(template.getS3Key())
                .description(template.getDescription())
                .templateType(template.getTemplateType())
                .status(template.getStatus())
                .isPremium(template.isPremium())
                .downloadUrl(s3Service.generatePresignedUrl(template.getS3Key(), 60).toString())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}