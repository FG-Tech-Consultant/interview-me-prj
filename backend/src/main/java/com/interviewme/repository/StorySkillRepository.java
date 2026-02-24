package com.interviewme.repository;

import com.interviewme.model.StorySkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorySkillRepository extends JpaRepository<StorySkill, Long> {

    List<StorySkill> findByStoryId(Long storyId);

    Optional<StorySkill> findByStoryIdAndUserSkillId(Long storyId, Long userSkillId);

    void deleteByStoryId(Long storyId);

    List<StorySkill> findByUserSkillId(Long userSkillId);
}
