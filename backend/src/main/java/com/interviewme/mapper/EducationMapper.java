package com.interviewme.mapper;

import com.interviewme.dto.education.CreateEducationRequest;
import com.interviewme.dto.education.EducationResponse;
import com.interviewme.dto.education.UpdateEducationRequest;
import com.interviewme.model.Education;

public class EducationMapper {

    public static Education toEntity(CreateEducationRequest request) {
        Education education = new Education();
        education.setDegree(request.degree());
        education.setInstitution(request.institution());
        education.setStartDate(request.startDate());
        education.setEndDate(request.endDate());
        education.setFieldOfStudy(request.fieldOfStudy());
        education.setGpa(request.gpa());
        education.setNotes(request.notes());
        education.setVisibility(request.visibility() != null ? request.visibility() : "private");
        return education;
    }

    public static EducationResponse toResponse(Education entity) {
        return new EducationResponse(
            entity.getId(),
            entity.getTenantId(),
            entity.getProfileId(),
            entity.getDegree(),
            entity.getInstitution(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getFieldOfStudy(),
            entity.getGpa(),
            entity.getNotes(),
            entity.getVisibility(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    public static void updateEntity(Education entity, UpdateEducationRequest request) {
        entity.setDegree(request.degree());
        entity.setInstitution(request.institution());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setFieldOfStudy(request.fieldOfStudy());
        entity.setGpa(request.gpa());
        entity.setNotes(request.notes());
        if (request.visibility() != null) {
            entity.setVisibility(request.visibility());
        }
    }
}
