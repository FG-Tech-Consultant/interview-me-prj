package com.interviewme.repository;

import com.interviewme.model.PackageStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackageStoryRepository extends JpaRepository<PackageStory, Long> {

    List<PackageStory> findByPackageIdOrderByDisplayOrderAsc(Long packageId);

    Optional<PackageStory> findByPackageIdAndStoryId(Long packageId, Long storyId);

    int countByPackageId(Long packageId);

    @Modifying
    @Query("DELETE FROM PackageStory ps WHERE ps.packageId = :packageId AND ps.storyId = :storyId")
    int deleteByPackageIdAndStoryId(@Param("packageId") Long packageId, @Param("storyId") Long storyId);

    @Modifying
    @Query("DELETE FROM PackageStory ps WHERE ps.packageId = :packageId")
    void deleteByPackageId(@Param("packageId") Long packageId);
}
