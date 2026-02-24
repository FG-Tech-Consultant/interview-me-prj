package com.interviewme.repository;

import com.interviewme.model.ExperienceProjectSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperienceProjectSkillRepository extends JpaRepository<ExperienceProjectSkill, Long> {

    List<ExperienceProjectSkill> findByExperienceProjectId(Long projectId);

    Optional<ExperienceProjectSkill> findByExperienceProjectIdAndUserSkillId(Long projectId, Long userSkillId);

    void deleteByExperienceProjectId(Long projectId);
}
