package com.autobook.storage.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.autobook.storage.model.Template;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByStatus(String status);
    List<Template> findByTemplateTypeAndStatus(String templateType, String status);
    List<Template> findByTemplateTypeAndStatusAndIsPremium(String templateType, String status, boolean isPremium);
}