package com.interviewme.controller;

import com.interviewme.dto.packages.PublicPackageResponse;
import com.interviewme.service.PackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicPackageController")
class PublicPackageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PackageService packageService;

    @InjectMocks
    private PublicPackageController publicPackageController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(publicPackageController).build();
    }

    @Test
    @DisplayName("GET /api/public/packages/{slug} - returns public package")
    void getPublicPackage_Success() throws Exception {
        PublicPackageResponse response = new PublicPackageResponse(
                "My Package", "Description", "my-pkg",
                "John Doe", "Senior Engineer",
                List.of(), List.of(), List.of());
        when(packageService.getPublicPackage("my-pkg", null)).thenReturn(response);

        mockMvc.perform(get("/api/public/packages/my-pkg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Package"))
                .andExpect(jsonPath("$.profileName").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/public/packages/{slug}?t=token - passes token parameter")
    void getPublicPackage_WithToken() throws Exception {
        PublicPackageResponse response = new PublicPackageResponse(
                "Private Pkg", "Description", "private-pkg",
                "Jane Doe", "Engineer",
                List.of(), List.of(), List.of());
        when(packageService.getPublicPackage("private-pkg", "abc123")).thenReturn(response);

        mockMvc.perform(get("/api/public/packages/private-pkg").param("t", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Private Pkg"));
    }

    @Test
    @DisplayName("GET /api/public/packages/{slug} - throws when not found")
    void getPublicPackage_NotFound() {
        when(packageService.getPublicPackage("unknown", null))
                .thenThrow(new RuntimeException("Package not found"));

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/public/packages/unknown")));
    }
}
