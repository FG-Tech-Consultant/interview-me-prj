package com.interviewme.exports.repository;

import com.interviewme.exports.model.ExportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportTemplateRepository extends JpaRepository<ExportTemplate, Long> {

    List<ExportTemplate> findByTypeAndIsActiveTrue(String type);

    List<ExportTemplate> findByIsActiveTrue();
}
