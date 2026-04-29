package com.interviewme.repository;

import com.interviewme.model.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    List<UserSkill> findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(Long profileId);

    Optional<UserSkill> findByIdAndTenantIdAndDeletedAtIsNull(Long id, Long tenantId);

    @Query("SELECT us FROM UserSkill us WHERE us.profileId = :profileId AND us.skillId = :skillId AND us.deletedAt IS NULL")
    Optional<UserSkill> findByProfileIdAndSkillIdAndDeletedAtIsNull(
            @Param("profileId") Long profileId,
            @Param("skillId") Long skillId);

    List<UserSkill> findByProfileIdAndVisibilityAndDeletedAtIsNull(Long profileId, String visibility);

    @Query("SELECT us FROM UserSkill us WHERE us.profileId = :profileId AND us.tenantId = :tenantId AND us.deletedAt IS NULL ORDER BY us.skill.category ASC, us.skill.name ASC")
    List<UserSkill> findByProfileIdAndTenantId(
            @Param("profileId") Long profileId,
            @Param("tenantId") Long tenantId);

    @Query("SELECT us FROM UserSkill us JOIN FETCH us.skill WHERE us.tenantId = :tenantId AND us.skillId IN :skillIds AND us.deletedAt IS NULL")
    List<UserSkill> findByTenantIdAndSkillIdInAndDeletedAtIsNull(
            @Param("tenantId") Long tenantId,
            @Param("skillIds") List<Long> skillIds);
}
