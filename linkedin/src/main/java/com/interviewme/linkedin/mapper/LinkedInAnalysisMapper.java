package com.interviewme.linkedin.mapper;

import com.interviewme.linkedin.dto.LinkedInAnalysisResponse;
import com.interviewme.linkedin.dto.LinkedInAnalysisSummary;
import com.interviewme.linkedin.dto.SectionScoreResponse;
import com.interviewme.linkedin.model.LinkedInAnalysis;
import com.interviewme.linkedin.model.LinkedInSectionScore;
import com.interviewme.linkedin.model.ProfileSection;

import java.util.Collections;
import java.util.List;

public final class LinkedInAnalysisMapper {

    private LinkedInAnalysisMapper() {}

    public static LinkedInAnalysisResponse toResponse(LinkedInAnalysis analysis, List<LinkedInSectionScore> sections) {
        List<SectionScoreResponse> sectionResponses = sections != null
                ? sections.stream().map(LinkedInAnalysisMapper::toSectionResponse).toList()
                : Collections.emptyList();

        return new LinkedInAnalysisResponse(
                analysis.getId(),
                analysis.getProfileId(),
                analysis.getStatus(),
                analysis.getSourceType(),
                analysis.getOverallScore(),
                analysis.getErrorMessage(),
                analysis.getPdfFilename(),
                analysis.getAnalyzedAt(),
                sectionResponses,
                analysis.getCreatedAt()
        );
    }

    public static LinkedInAnalysisSummary toSummary(LinkedInAnalysis analysis) {
        return new LinkedInAnalysisSummary(
                analysis.getId(),
                analysis.getStatus(),
                analysis.getSourceType(),
                analysis.getOverallScore(),
                analysis.getPdfFilename(),
                analysis.getAnalyzedAt(),
                analysis.getCreatedAt()
        );
    }

    public static SectionScoreResponse toSectionResponse(LinkedInSectionScore section) {
        boolean canApply;
        try {
            canApply = ProfileSection.valueOf(section.getSectionName()).canApplyToProfile();
        } catch (IllegalArgumentException e) {
            canApply = false;
        }

        return new SectionScoreResponse(
                section.getId(),
                section.getSectionName(),
                section.getSectionScore(),
                section.getQualityExplanation(),
                section.getSuggestions() != null ? section.getSuggestions() : Collections.emptyList(),
                canApply
        );
    }
}
