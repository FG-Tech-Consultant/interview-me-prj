package com.interviewme.repository;

import com.interviewme.model.PackageSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackageSkillRepository extends JpaRepository<PackageSkill, Long> {

    List<PackageSkill> findByPackageIdOrderByDisplayOrderAsc(Long packageId);

    Optional<PackageSkill> findByPackageIdAndUserSkillId(Long packageId, Long userSkillId);

    int countByPackageId(Long packageId);

    @Modifying
    @Query("DELETE FROM PackageSkill ps WHERE ps.packageId = :packageId AND ps.userSkillId = :userSkillId")
    int deleteByPackageIdAndUserSkillId(@Param("packageId") Long packageId, @Param("userSkillId") Long userSkillId);

    @Modifying
    @Query("DELETE FROM PackageSkill ps WHERE ps.packageId = :packageId")
    void deleteByPackageId(@Param("packageId") Long packageId);
}
