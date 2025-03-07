package com.autobook.storage.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.autobook.storage.dto.TemplateDto;
import com.autobook.storage.service.TemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<TemplateDto> uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("templateType") String templateType,
            @RequestParam(value = "isPremium", defaultValue = "false") boolean isPremium) {

        TemplateDto templateDto = templateService.saveTemplate(file, name, description, templateType, isPremium);
        return new ResponseEntity<>(templateDto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateDto> getTemplate(@PathVariable Long id) {
        TemplateDto templateDto = templateService.getTemplateById(id);
        return ResponseEntity.ok(templateDto);
    }

    @GetMapping
    public ResponseEntity<List<TemplateDto>> getAllActiveTemplates() {
        List<TemplateDto> templates = templateService.getAllActiveTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/type/{type}/status/{status}")
    public ResponseEntity<List<TemplateDto>> getTemplatesByTypeAndStatus(
            @PathVariable String type,
            @PathVariable String status) {
        List<TemplateDto> templates = templateService.getTemplatesByTypeAndStatus(type, status);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/type/{type}/status/{status}/premium/{premium}")
    public ResponseEntity<List<TemplateDto>> getTemplatesByTypeStatusAndPremium(
            @PathVariable String type,
            @PathVariable String status,
            @PathVariable boolean premium) {
        List<TemplateDto> templates = templateService.getTemplatesByTypeStatusAndPremium(type, status, premium);
        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateDto> updateTemplate(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("status") String status,
            @RequestParam("isPremium") boolean isPremium) {

        TemplateDto updatedTemplateDto = templateService.updateTemplate(id, name, description, status, isPremium);
        return ResponseEntity.ok(updatedTemplateDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long id) {
        byte[] templateData = templateService.downloadTemplate(id);
        TemplateDto templateDto = templateService.getTemplateById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", templateDto.getName() + ".template");
        headers.setContentLength(templateData.length);

        return new ResponseEntity<>(templateData, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/url")
    public ResponseEntity<String> getTemplateUrl(@PathVariable Long id) {
        String url = templateService.getTemplateDownloadUrl(id);
        return ResponseEntity.ok(url);
    }
}