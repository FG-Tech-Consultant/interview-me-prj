package com.interviewme.linkedin.service;

import com.interviewme.linkedin.model.ProfileSection;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class LinkedInPdfParserService {

    // LinkedIn PDF section headers (case-insensitive patterns)
    private static final Pattern ABOUT_PATTERN = Pattern.compile("(?i)^\\s*About\\s*$", Pattern.MULTILINE);
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(?i)^\\s*Experience\\s*$", Pattern.MULTILINE);
    private static final Pattern EDUCATION_PATTERN = Pattern.compile("(?i)^\\s*Education\\s*$", Pattern.MULTILINE);
    private static final Pattern SKILLS_PATTERN = Pattern.compile("(?i)^\\s*Skills\\s*$", Pattern.MULTILINE);
    private static final Pattern RECOMMENDATIONS_PATTERN = Pattern.compile("(?i)^\\s*Recommendations\\s*$", Pattern.MULTILINE);

    // Ordered section markers for sequential extraction
    private static final String[] SECTION_ORDER = {
            "About", "Experience", "Education", "Skills", "Recommendations"
    };

    public Map<ProfileSection, String> parse(Path pdfPath) {
        log.info("Parsing LinkedIn PDF: {}", pdfPath.getFileName());
        Map<ProfileSection, String> sections = new EnumMap<>(ProfileSection.class);

        // Initialize all sections with empty strings
        for (ProfileSection section : ProfileSection.values()) {
            sections.put(section, "");
        }

        try {
            String fullText = extractText(pdfPath);
            if (fullText == null || fullText.isBlank()) {
                log.warn("PDF is empty or contains no extractable text: {}", pdfPath.getFileName());
                return sections;
            }

            extractSections(fullText, sections);
            log.info("PDF parsed successfully. Sections found: {}",
                    sections.entrySet().stream()
                            .filter(e -> !e.getValue().isBlank())
                            .map(e -> e.getKey().name())
                            .toList());

        } catch (IOException e) {
            log.error("Failed to parse PDF: {}", pdfPath.getFileName(), e);
        }

        return sections;
    }

    private String extractText(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private void extractSections(String fullText, Map<ProfileSection, String> sections) {
        // Extract headline: typically the first few lines contain name and headline
        extractHeadline(fullText, sections);

        // Find section boundaries
        int aboutStart = findSectionStart(fullText, ABOUT_PATTERN);
        int experienceStart = findSectionStart(fullText, EXPERIENCE_PATTERN);
        int educationStart = findSectionStart(fullText, EDUCATION_PATTERN);
        int skillsStart = findSectionStart(fullText, SKILLS_PATTERN);
        int recommendationsStart = findSectionStart(fullText, RECOMMENDATIONS_PATTERN);

        // Extract each section based on boundaries
        if (aboutStart >= 0) {
            int end = findNextSectionStart(experienceStart, educationStart, skillsStart, recommendationsStart, fullText.length());
            sections.put(ProfileSection.ABOUT, extractContent(fullText, aboutStart, end));
        }

        if (experienceStart >= 0) {
            int end = findNextSectionStart(educationStart, skillsStart, recommendationsStart, fullText.length());
            sections.put(ProfileSection.EXPERIENCE, extractContent(fullText, experienceStart, end));
        }

        if (educationStart >= 0) {
            int end = findNextSectionStart(skillsStart, recommendationsStart, fullText.length());
            sections.put(ProfileSection.EDUCATION, extractContent(fullText, educationStart, end));
        }

        if (skillsStart >= 0) {
            int end = findNextSectionStart(recommendationsStart, fullText.length());
            sections.put(ProfileSection.SKILLS, extractContent(fullText, skillsStart, end));
        }

        if (recommendationsStart >= 0) {
            sections.put(ProfileSection.RECOMMENDATIONS, extractContent(fullText, recommendationsStart, fullText.length()));
        }

        // Collect remaining text as OTHER
        collectOtherContent(fullText, sections);
    }

    private void extractHeadline(String fullText, Map<ProfileSection, String> sections) {
        // LinkedIn PDFs typically have the name on line 1 and headline on line 2-3
        String[] lines = fullText.split("\\r?\\n");
        if (lines.length >= 2) {
            // Skip the name (first line), take the next non-empty line as headline
            for (int i = 1; i < Math.min(lines.length, 5); i++) {
                String line = lines[i].trim();
                if (!line.isBlank() && !line.equalsIgnoreCase("Contact")
                        && !line.equalsIgnoreCase("About") && !line.contains("linkedin.com")) {
                    sections.put(ProfileSection.HEADLINE, line);
                    break;
                }
            }
        }
    }

    private int findSectionStart(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.start();
        }
        return -1;
    }

    private int findNextSectionStart(int... candidates) {
        int min = Integer.MAX_VALUE;
        for (int candidate : candidates) {
            if (candidate >= 0 && candidate < min) {
                min = candidate;
            }
        }
        return min;
    }

    private String extractContent(String text, int start, int end) {
        if (start < 0 || start >= end || start >= text.length()) {
            return "";
        }
        // Skip the section header line
        int contentStart = text.indexOf('\n', start);
        if (contentStart < 0 || contentStart >= end) {
            return "";
        }
        return text.substring(contentStart + 1, Math.min(end, text.length())).trim();
    }

    private void collectOtherContent(String fullText, Map<ProfileSection, String> sections) {
        // If no sections were found at all, put everything in OTHER
        boolean anySectionFound = sections.entrySet().stream()
                .filter(e -> e.getKey() != ProfileSection.OTHER && e.getKey() != ProfileSection.HEADLINE)
                .anyMatch(e -> !e.getValue().isBlank());

        if (!anySectionFound) {
            sections.put(ProfileSection.OTHER, fullText.trim());
        }
    }
}
