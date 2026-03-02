package com.interviewme.service;

import com.interviewme.linkedin.dto.*;
import com.interviewme.linkedin.model.ProfileSection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts LinkedIn ZIP import data into ProfileSection map format
 * suitable for the AI analysis prompt.
 */
@Service
@Slf4j
public class ZipDataToSectionsService {

    public Map<ProfileSection, String> convertToSections(LinkedInImportData importData) {
        log.info("Converting LinkedIn ZIP data to profile sections");

        Map<ProfileSection, String> sections = new EnumMap<>(ProfileSection.class);

        // Initialize all sections with empty strings
        for (ProfileSection section : ProfileSection.values()) {
            sections.put(section, "");
        }

        if (importData == null) {
            return sections;
        }

        // HEADLINE
        if (importData.profile() != null && importData.profile().headline() != null) {
            sections.put(ProfileSection.HEADLINE, importData.profile().headline());
        }

        // ABOUT
        if (importData.profile() != null && importData.profile().summary() != null) {
            sections.put(ProfileSection.ABOUT, importData.profile().summary());
        }

        // EXPERIENCE
        String experienceText = buildExperienceFromPositions(importData.positions());
        if (!experienceText.isBlank()) {
            sections.put(ProfileSection.EXPERIENCE, experienceText);
        }

        // EDUCATION
        String educationText = buildEducationFromData(importData.education());
        if (!educationText.isBlank()) {
            sections.put(ProfileSection.EDUCATION, educationText);
        }

        // SKILLS
        if (importData.skills() != null && !importData.skills().isEmpty()) {
            sections.put(ProfileSection.SKILLS, String.join("\n", importData.skills()));
        }

        // OTHER (projects, certifications, languages)
        String otherText = buildOtherSection(importData);
        if (!otherText.isBlank()) {
            sections.put(ProfileSection.OTHER, otherText);
        }

        log.info("ZIP data converted. Sections with content: {}",
                sections.entrySet().stream()
                        .filter(e -> !e.getValue().isBlank())
                        .map(e -> e.getKey().name())
                        .toList());

        return sections;
    }

    private String buildExperienceFromPositions(List<LinkedInPositionData> positions) {
        if (positions == null || positions.isEmpty()) {
            return "";
        }

        return positions.stream()
                .map(p -> {
                    StringBuilder sb = new StringBuilder();
                    if (p.title() != null) sb.append(p.title());
                    if (p.companyName() != null) sb.append(" at ").append(p.companyName());

                    String dateRange = "";
                    if (p.startDate() != null) {
                        dateRange = p.startDate();
                        if (p.endDate() != null) {
                            dateRange += " - " + p.endDate();
                        } else {
                            dateRange += " - Present";
                        }
                    }
                    if (!dateRange.isEmpty()) {
                        sb.append(" (").append(dateRange).append(")");
                    }
                    sb.append("\n");

                    if (p.location() != null && !p.location().isBlank()) {
                        sb.append("Location: ").append(p.location()).append("\n");
                    }
                    if (p.description() != null && !p.description().isBlank()) {
                        sb.append(p.description()).append("\n");
                    }

                    return sb.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    private String buildEducationFromData(List<LinkedInEducationData> educationList) {
        if (educationList == null || educationList.isEmpty()) {
            return "";
        }

        return educationList.stream()
                .map(e -> {
                    StringBuilder sb = new StringBuilder();
                    if (e.degreeName() != null) sb.append(e.degreeName());
                    if (e.schoolName() != null) sb.append(" - ").append(e.schoolName());

                    String dateRange = "";
                    if (e.startDate() != null) {
                        dateRange = e.startDate();
                        if (e.endDate() != null) {
                            dateRange += " - " + e.endDate();
                        }
                    }
                    if (!dateRange.isEmpty()) {
                        sb.append(" (").append(dateRange).append(")");
                    }
                    sb.append("\n");

                    if (e.notes() != null && !e.notes().isBlank()) {
                        sb.append(e.notes()).append("\n");
                    }

                    return sb.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    private String buildOtherSection(LinkedInImportData importData) {
        StringBuilder sb = new StringBuilder();

        // Languages
        if (importData.languages() != null && !importData.languages().isEmpty()) {
            sb.append("Languages: ").append(String.join(", ", importData.languages())).append("\n\n");
        }

        // Projects
        if (importData.projects() != null && !importData.projects().isEmpty()) {
            sb.append("Projects:\n");
            for (LinkedInProjectData project : importData.projects()) {
                if (project.title() != null) sb.append("- ").append(project.title());
                if (project.description() != null) sb.append(": ").append(project.description());
                sb.append("\n");
            }
            sb.append("\n");
        }

        // Certifications
        if (importData.certifications() != null && !importData.certifications().isEmpty()) {
            sb.append("Certifications:\n");
            for (LinkedInCertificationData cert : importData.certifications()) {
                if (cert.name() != null) sb.append("- ").append(cert.name());
                if (cert.authority() != null) sb.append(" by ").append(cert.authority());
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }
}
