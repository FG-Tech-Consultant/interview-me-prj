package com.interviewme.controller;

import com.interviewme.dto.skill.AddUserSkillDto;
import com.interviewme.dto.skill.UpdateUserSkillDto;
import com.interviewme.dto.skill.UserSkillDto;
import com.interviewme.service.UserSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/skills/user")
@RequiredArgsConstructor
@Slf4j
public class UserSkillController {

    private final UserSkillService userSkillService;

    @GetMapping("/{profileId}")
    public ResponseEntity<Map<String, List<UserSkillDto>>> getUserSkills(@PathVariable Long profileId) {
        log.info("GET /api/v1/skills/user/{}", profileId);
        Map<String, List<UserSkillDto>> skills = userSkillService.getSkillsByProfile(profileId);
        return ResponseEntity.ok(skills);
    }

    @PostMapping("/{profileId}")
    public ResponseEntity<UserSkillDto> addUserSkill(
            @PathVariable Long profileId,
            @Valid @RequestBody AddUserSkillDto dto) {
        log.info("POST /api/v1/skills/user/{} - skillId: {}", profileId, dto.skillId());
        UserSkillDto created = userSkillService.addSkill(profileId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<UserSkillDto> getUserSkillById(@PathVariable Long id) {
        log.info("GET /api/v1/skills/user/detail/{}", id);
        UserSkillDto skill = userSkillService.getSkillById(id);
        return ResponseEntity.ok(skill);
    }

    @PutMapping("/detail/{id}")
    public ResponseEntity<UserSkillDto> updateUserSkill(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserSkillDto dto) {
        log.info("PUT /api/v1/skills/user/detail/{}", id);
        UserSkillDto updated = userSkillService.updateSkill(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/detail/{id}")
    public ResponseEntity<Void> deleteUserSkill(@PathVariable Long id) {
        log.info("DELETE /api/v1/skills/user/detail/{}", id);
        userSkillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{profileId}/public")
    public ResponseEntity<List<UserSkillDto>> getPublicSkills(@PathVariable Long profileId) {
        log.info("GET /api/v1/skills/user/{}/public", profileId);
        List<UserSkillDto> skills = userSkillService.getPublicSkillsByProfile(profileId);
        return ResponseEntity.ok(skills);
    }
}
