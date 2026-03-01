package com.interviewme.linkedin.service;

import com.interviewme.linkedin.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class LinkedInZipParserService {

    public LinkedInImportData parseZip(InputStream zipInputStream) {
        List<String> warnings = new ArrayList<>();
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("linkedin-import-");
            extractZip(zipInputStream, tempDir);

            LinkedInProfileData profile = parseProfile(tempDir, warnings);
            List<LinkedInPositionData> positions = parsePositions(tempDir, warnings);
            List<LinkedInEducationData> education = parseEducation(tempDir, warnings);
            List<String> skills = parseSkills(tempDir, warnings);
            List<String> languages = parseLanguages(tempDir, warnings);
            List<LinkedInProjectData> projects = parseProjects(tempDir, warnings);
            List<LinkedInCertificationData> certifications = parseCertifications(tempDir, warnings);

            log.info("LinkedIn ZIP parsed: profile={}, positions={}, education={}, skills={}, languages={}, projects={}, certifications={}",
                    profile != null, positions.size(), education.size(), skills.size(),
                    languages.size(), projects.size(), certifications.size());

            return new LinkedInImportData(profile, positions, education, skills, languages, projects, certifications, warnings);
        } catch (IOException e) {
            log.error("Failed to parse LinkedIn ZIP", e);
            warnings.add("Failed to parse ZIP file: " + e.getMessage());
            return new LinkedInImportData(null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), warnings);
        } finally {
            if (tempDir != null) {
                cleanupTempDir(tempDir);
            }
        }
    }

    private void extractZip(InputStream zipInputStream, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                String name = entry.getName();
                // Handle nested directories in ZIP (e.g., "linkedin-export/Profile.csv")
                Path fileName = Path.of(name).getFileName();
                Path targetPath = targetDir.resolve(fileName);

                // Security: prevent path traversal
                if (!targetPath.normalize().startsWith(targetDir.normalize())) {
                    log.warn("Skipping potentially unsafe ZIP entry: {}", name);
                    continue;
                }

                Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private LinkedInProfileData parseProfile(Path dir, List<String> warnings) {
        Path file = findCsvFile(dir, "Profile");
        if (file == null) {
            warnings.add("Profile.csv not found in ZIP");
            return null;
        }

        try {
            List<CSVRecord> records = parseCsvFile(file);
            if (records.isEmpty()) {
                warnings.add("Profile.csv is empty");
                return null;
            }

            CSVRecord record = records.getFirst();
            return new LinkedInProfileData(
                    getField(record, "First Name"),
                    getField(record, "Last Name"),
                    getField(record, "Headline"),
                    getField(record, "Summary"),
                    getField(record, "Geo Location"),
                    getField(record, "Email Address")
            );
        } catch (IOException e) {
            warnings.add("Failed to parse Profile.csv: " + e.getMessage());
            return null;
        }
    }

    private List<LinkedInPositionData> parsePositions(Path dir, List<String> warnings) {
        Path file = findCsvFile(dir, "Positions");
        if (file == null) {
            warnings.add("Positions.csv not found in ZIP");
            return List.of();
        }

        try {
            List<CSVRecord> records = parseCsvFile(file);
            return records.stream()
                    .map(r -> new LinkedInPositionData(
                            getField(r, "Company Name"),
                            getField(r, "Title"),
                            getField(r, "Description"),
                            getField(r, "Location"),
                            getField(r, "Started On"),
                            getField(r, "Finished On")
                    ))
                    .toList();
        } catch (IOException e) {
            warnings.add("Failed to parse Positions.csv: " + e.getMessage());
            return List.of();
        }
    }

    private List<LinkedInEducationData> parseEducation(Path dir, List<String> warnings) {
        Path file = findCsvFile(dir, "Education");
        if (file == null) {
            warnings.add("Education.csv not found in ZIP");
            return List.of();
        }

        try {
            List<CSVRecord> records = parseCsvFile(file);
            return records.stream()
                    .map(r -> new LinkedInEducationData(
                            getField(r, "School Name"),
                            getField(r, "Degree Name"),
                            getField(r, "Notes"),
                            getField(r, "Start Date"),
                            getField(r, "End Date"),
                            getField(r, "Notes"),
                            getField(r, "Activities")
                    ))
                    .toList();
        } catch (IOException e) {
            warnings.add("Failed to parse Education.csv: " + e.getMessage());
            return List.of();
        }
    }

    private List<String> parseSkills(Path dir, List<String> warnings) {
        Path file = findCsvFile(dir, "Skills");
        if (file == null) {
            warnings.add("Skills.csv not found in ZIP");
            return List.of();
        }

        try {
            List<CSVRecord> records = parseCsvFile(file);
            return records.stream()
                    .map(r -> getField(r, "Name"))
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        } catch (IOException e) {
            warnings.add("Failed to parse Skills.csv: " + e.getMessage());
            return List.of();
        }
    }

    private List<String> parseLanguages(Path dir, List<String> warnings) {
        Path file = findCsvFile(dir, "Languages");
        if (file == null) {
            warnings.add("Languages.csv not found in ZIP");
            return List.of();
        }

        try {
            List<CSVRecord> records = parseCsvFile(file);
            return records.stream()
                    .map(r -> getField(r, "Name"))
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        } catch (IOException e) {
            warnings.add("Failed to parse Languages.csv: " + e.getMessage());
            return List.of();
        }
    }

    private List<LinkedInProjectData> parseProjects(Path dir, List<String> warnings) {
        Path file = findCsvFile(dir, "Projects");
        if (file == null) {
            warnings.add("Projects.csv not found in ZIP");
            return List.of();
        }

        try {
            List<CSVRecord> records = parseCsvFile(file);
            return records.stream()
                    .map(r -> new LinkedInProjectData(
                            getField(r, "Title"),
                            getField(r, "Description"),
                            getField(r, "Url"),
                            getField(r, "Started On"),
                            getField(r, "Finished On")
                    ))
                    .toList();
        } catch (IOException e) {
            warnings.add("Failed to parse Projects.csv: " + e.getMessage());
            return List.of();
        }
    }

    private List<LinkedInCertificationData> parseCertifications(Path dir, List<String> warnings) {
        Path file = findCsvFile(dir, "Certifications");
        if (file == null) {
            warnings.add("Certifications.csv not found in ZIP");
            return List.of();
        }

        try {
            List<CSVRecord> records = parseCsvFile(file);
            return records.stream()
                    .map(r -> new LinkedInCertificationData(
                            getField(r, "Name"),
                            getField(r, "Authority"),
                            getField(r, "Url"),
                            getField(r, "Started On"),
                            getField(r, "Finished On")
                    ))
                    .toList();
        } catch (IOException e) {
            warnings.add("Failed to parse Certifications.csv: " + e.getMessage());
            return List.of();
        }
    }

    private Path findCsvFile(Path dir, String baseName) {
        // Try exact name first, then case-insensitive
        Path exact = dir.resolve(baseName + ".csv");
        if (Files.exists(exact)) return exact;

        try (var stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(baseName + ".csv"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private List<CSVRecord> parseCsvFile(Path file) throws IOException {
        // Read bytes first to handle BOM
        byte[] bytes = Files.readAllBytes(file);
        String content = removeBom(new String(bytes, StandardCharsets.UTF_8));

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (CSVParser parser = CSVParser.parse(new StringReader(content), format)) {
            return parser.getRecords();
        }
    }

    private String removeBom(String content) {
        if (content != null && !content.isEmpty() && content.charAt(0) == '\uFEFF') {
            return content.substring(1);
        }
        return content;
    }

    private String getField(CSVRecord record, String header) {
        try {
            if (record.isMapped(header)) {
                String value = record.get(header);
                return (value != null && !value.isBlank()) ? value.trim() : null;
            }
        } catch (IllegalArgumentException e) {
            // Header not found
        }
        return null;
    }

    private void cleanupTempDir(Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete temp file: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to cleanup temp directory: {}", dir, e);
        }
    }
}
