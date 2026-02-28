package com.interviewme.repository;

import com.interviewme.model.PackageProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackageProjectRepository extends JpaRepository<PackageProject, Long> {

    List<PackageProject> findByPackageIdOrderByDisplayOrderAsc(Long packageId);

    Optional<PackageProject> findByPackageIdAndExperienceProjectId(Long packageId, Long experienceProjectId);

    int countByPackageId(Long packageId);

    @Modifying
    @Query("DELETE FROM PackageProject pp WHERE pp.packageId = :packageId AND pp.experienceProjectId = :projectId")
    int deleteByPackageIdAndExperienceProjectId(@Param("packageId") Long packageId, @Param("projectId") Long projectId);

    @Modifying
    @Query("DELETE FROM PackageProject pp WHERE pp.packageId = :packageId")
    void deleteByPackageId(@Param("packageId") Long packageId);
}
