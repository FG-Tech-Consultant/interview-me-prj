package com.interviewme.repository;

import com.interviewme.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findTop10ByNameContainingIgnoreCaseAndIsActiveTrueOrderByName(String query);

    List<Skill> findByIsActiveTrueOrderByName();

    Optional<Skill> findByNameIgnoreCase(String name);
}
