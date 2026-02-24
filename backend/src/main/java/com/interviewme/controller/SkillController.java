package com.interviewme.controller;

import com.interviewme.dto.skill.CreateSkillDto;
import com.interviewme.dto.skill.SkillDto;
import com.interviewme.dto.skill.UpdateSkillDto;
import com.interviewme.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills/catalog")
@RequiredArgsConstructor
@Slf4j
public class SkillController {

    private final SkillService skillService;

    @GetMapping("/search")
    public ResponseEntity<List<SkillDto>> searchCatalog(@RequestParam("q") String query) {
        log.info("GET /api/v1/skills/catalog/search?q={}", query);
        List<SkillDto> results = skillService.searchActive(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SkillDto>> getAllSkills() {
        log.info("GET /api/v1/skills/catalog");
        List<SkillDto> skills = skillService.findAll();
        return ResponseEntity.ok(skills);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillDto> createSkill(@Valid @RequestBody CreateSkillDto dto) {
        log.info("POST /api/v1/skills/catalog - name: {}", dto.name());
        SkillDto created = skillService.createSkill(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillDto> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSkillDto dto) {
        log.info("PUT /api/v1/skills/catalog/{}", id);
        SkillDto updated = skillService.updateSkill(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillDto> deactivateSkill(@PathVariable Long id) {
        log.info("POST /api/v1/skills/catalog/{}/deactivate", id);
        SkillDto deactivated = skillService.deactivateSkill(id);
        return ResponseEntity.ok(deactivated);
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillDto> reactivateSkill(@PathVariable Long id) {
        log.info("POST /api/v1/skills/catalog/{}/reactivate", id);
        SkillDto reactivated = skillService.reactivateSkill(id);
        return ResponseEntity.ok(reactivated);
    }
}
