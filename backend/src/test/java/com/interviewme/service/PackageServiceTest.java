package com.interviewme.service;

import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.PackageNotFoundException;
import com.interviewme.common.exception.ProfileNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.packages.*;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PackageService")
class PackageServiceTest {

    @Mock private ContentPackageRepository packageRepository;
    @Mock private PackageSkillRepository packageSkillRepository;
    @Mock private PackageProjectRepository packageProjectRepository;
    @Mock private PackageStoryRepository packageStoryRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private ExperienceProjectRepository experienceProjectRepository;
    @Mock private StoryRepository storyRepository;

    @InjectMocks
    private PackageService packageService;

    private MockedStatic<TenantContext> tenantContextMock;

    private static final Long TENANT_ID = 100L;
    private static final Long PROFILE_ID = 10L;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getCurrentTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    private ContentPackage buildPackage(Long id) {
        ContentPackage pkg = new ContentPackage();
        pkg.setId(id);
        pkg.setTenantId(TENANT_ID);
        pkg.setProfileId(PROFILE_ID);
        pkg.setName("Backend Portfolio");
        pkg.setDescription("My backend work");
        pkg.setSlug("backend-portfolio");
        pkg.setAccessToken("token-123");
        pkg.setIsActive(true);
        pkg.setViewCount(0);
        return pkg;
    }

    @Nested
    @DisplayName("createPackage")
    class CreatePackage {

        @Test
        @DisplayName("creates package with auto-generated slug")
        void createsWithAutoSlug() {
            Profile profile = new Profile();
            profile.setId(PROFILE_ID);
            when(profileRepository.findByIdAndTenantId(PROFILE_ID, TENANT_ID)).thenReturn(Optional.of(profile));
            when(packageRepository.existsBySlugGlobally(anyString())).thenReturn(false);

            ContentPackage saved = buildPackage(1L);
            when(packageRepository.save(any(ContentPackage.class))).thenReturn(saved);

            CreatePackageRequest request = new CreatePackageRequest("Backend Portfolio", "My work", null);

            PackageResponse result = packageService.createPackage(PROFILE_ID, request);

            assertThat(result).isNotNull();
            verify(packageRepository).save(argThat(p ->
                    p.getSlug() != null && p.getAccessToken() != null));
        }

        @Test
        @DisplayName("creates package with custom slug")
        void createsWithCustomSlug() {
            Profile profile = new Profile();
            profile.setId(PROFILE_ID);
            when(profileRepository.findByIdAndTenantId(PROFILE_ID, TENANT_ID)).thenReturn(Optional.of(profile));
            when(packageRepository.existsBySlugGlobally("my-portfolio")).thenReturn(false);

            ContentPackage saved = buildPackage(1L);
            saved.setSlug("my-portfolio");
            when(packageRepository.save(any(ContentPackage.class))).thenReturn(saved);

            CreatePackageRequest request = new CreatePackageRequest("My Portfolio", null, "my-portfolio");

            packageService.createPackage(PROFILE_ID, request);

            verify(packageRepository).save(argThat(p -> p.getSlug().equals("my-portfolio")));
        }

        @Test
        @DisplayName("throws ProfileNotFoundException when profile missing")
        void throwsWhenProfileMissing() {
            when(profileRepository.findByIdAndTenantId(PROFILE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> packageService.createPackage(PROFILE_ID,
                    new CreatePackageRequest("Test", null, null)))
                    .isInstanceOf(ProfileNotFoundException.class);
        }

        @Test
        @DisplayName("throws ValidationException for invalid slug format")
        void throwsForInvalidSlug() {
            Profile profile = new Profile();
            profile.setId(PROFILE_ID);
            when(profileRepository.findByIdAndTenantId(PROFILE_ID, TENANT_ID)).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> packageService.createPackage(PROFILE_ID,
                    new CreatePackageRequest("Test", null, "AB")))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Slug");
        }

        @Test
        @DisplayName("throws ValidationException for reserved slug")
        void throwsForReservedSlug() {
            Profile profile = new Profile();
            profile.setId(PROFILE_ID);
            when(profileRepository.findByIdAndTenantId(PROFILE_ID, TENANT_ID)).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> packageService.createPackage(PROFILE_ID,
                    new CreatePackageRequest("Admin", null, "admin")))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("reserved");
        }

        @Test
        @DisplayName("throws ValidationException for taken slug")
        void throwsForTakenSlug() {
            Profile profile = new Profile();
            profile.setId(PROFILE_ID);
            when(profileRepository.findByIdAndTenantId(PROFILE_ID, TENANT_ID)).thenReturn(Optional.of(profile));
            when(packageRepository.existsBySlugGlobally("taken-slug")).thenReturn(true);

            assertThatThrownBy(() -> packageService.createPackage(PROFILE_ID,
                    new CreatePackageRequest("Test", null, "taken-slug")))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already in use");
        }
    }

    @Nested
    @DisplayName("updatePackage")
    class UpdatePackage {

        @Test
        @DisplayName("updates package successfully")
        void updatesSuccessfully() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageRepository.save(any(ContentPackage.class))).thenReturn(pkg);

            UpdatePackageRequest request = new UpdatePackageRequest("New Name", "New Desc", true, 1L);

            PackageResponse result = packageService.updatePackage(1L, request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("throws PackageNotFoundException when missing")
        void throwsWhenMissing() {
            when(packageRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> packageService.updatePackage(99L,
                    new UpdatePackageRequest("n", "d", true, 1L)))
                    .isInstanceOf(PackageNotFoundException.class);
        }

        @Test
        @DisplayName("throws OptimisticLockException on conflict")
        void throwsOnOptimisticLock() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageRepository.save(any(ContentPackage.class)))
                    .thenThrow(new OptimisticLockingFailureException("conflict"));

            assertThatThrownBy(() -> packageService.updatePackage(1L,
                    new UpdatePackageRequest("n", "d", true, 1L)))
                    .isInstanceOf(OptimisticLockException.class);
        }
    }

    @Nested
    @DisplayName("deletePackage")
    class DeletePackage {

        @Test
        @DisplayName("soft deletes package and cleans associations")
        void softDeletesWithCleanup() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageRepository.save(any(ContentPackage.class))).thenReturn(pkg);

            packageService.deletePackage(1L);

            assertThat(pkg.getDeletedAt()).isNotNull();
            verify(packageSkillRepository).deleteByPackageId(1L);
            verify(packageProjectRepository).deleteByPackageId(1L);
            verify(packageStoryRepository).deleteByPackageId(1L);
        }
    }

    @Nested
    @DisplayName("addSkill")
    class AddSkill {

        @Test
        @DisplayName("adds public skill to package")
        void addsPublicSkill() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageSkillRepository.countByPackageId(1L)).thenReturn(0);

            UserSkill userSkill = new UserSkill();
            userSkill.setId(50L);
            userSkill.setVisibility("public");
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(50L, TENANT_ID))
                    .thenReturn(Optional.of(userSkill));
            when(packageSkillRepository.findByPackageIdAndUserSkillId(1L, 50L))
                    .thenReturn(Optional.empty());

            packageService.addSkill(1L, new AddPackageItemRequest(50L, 0));

            verify(packageSkillRepository).save(any(PackageSkill.class));
        }

        @Test
        @DisplayName("throws ValidationException for private skill")
        void throwsForPrivateSkill() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageSkillRepository.countByPackageId(1L)).thenReturn(0);

            UserSkill userSkill = new UserSkill();
            userSkill.setId(50L);
            userSkill.setVisibility("private");
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(50L, TENANT_ID))
                    .thenReturn(Optional.of(userSkill));

            assertThatThrownBy(() -> packageService.addSkill(1L, new AddPackageItemRequest(50L, null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("public");
        }

        @Test
        @DisplayName("throws ValidationException when max skills reached")
        void throwsWhenMaxSkillsReached() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageSkillRepository.countByPackageId(1L)).thenReturn(20);

            assertThatThrownBy(() -> packageService.addSkill(1L, new AddPackageItemRequest(50L, null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Maximum");
        }

        @Test
        @DisplayName("throws ValidationException for duplicate skill")
        void throwsForDuplicateSkill() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageSkillRepository.countByPackageId(1L)).thenReturn(0);

            UserSkill userSkill = new UserSkill();
            userSkill.setId(50L);
            userSkill.setVisibility("public");
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(50L, TENANT_ID))
                    .thenReturn(Optional.of(userSkill));
            when(packageSkillRepository.findByPackageIdAndUserSkillId(1L, 50L))
                    .thenReturn(Optional.of(new PackageSkill()));

            assertThatThrownBy(() -> packageService.addSkill(1L, new AddPackageItemRequest(50L, null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("addProject")
    class AddProject {

        @Test
        @DisplayName("throws ValidationException for private project")
        void throwsForPrivateProject() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageProjectRepository.countByPackageId(1L)).thenReturn(0);

            ExperienceProject project = new ExperienceProject();
            project.setId(60L);
            project.setVisibility("private");
            when(experienceProjectRepository.findByIdAndDeletedAtIsNull(60L))
                    .thenReturn(Optional.of(project));

            assertThatThrownBy(() -> packageService.addProject(1L, new AddPackageItemRequest(60L, null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("public");
        }
    }

    @Nested
    @DisplayName("addStory")
    class AddStory {

        @Test
        @DisplayName("throws ValidationException for private story")
        void throwsForPrivateStory() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageStoryRepository.countByPackageId(1L)).thenReturn(0);

            Story story = new Story();
            story.setId(70L);
            story.setVisibility("private");
            when(storyRepository.findByIdAndDeletedAtIsNull(70L)).thenReturn(Optional.of(story));

            assertThatThrownBy(() -> packageService.addStory(1L, new AddPackageItemRequest(70L, null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("public");
        }
    }

    @Nested
    @DisplayName("removeSkill")
    class RemoveSkill {

        @Test
        @DisplayName("removes skill from package")
        void removesSkill() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageSkillRepository.deleteByPackageIdAndUserSkillId(1L, 50L)).thenReturn(1);

            packageService.removeSkill(1L, 50L);

            verify(packageSkillRepository).deleteByPackageIdAndUserSkillId(1L, 50L);
        }

        @Test
        @DisplayName("throws ValidationException when skill not in package")
        void throwsWhenNotFound() {
            ContentPackage pkg = buildPackage(1L);
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageSkillRepository.deleteByPackageIdAndUserSkillId(1L, 50L)).thenReturn(0);

            assertThatThrownBy(() -> packageService.removeSkill(1L, 50L))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("regenerateToken")
    class RegenerateToken {

        @Test
        @DisplayName("regenerates access token")
        void regeneratesToken() {
            ContentPackage pkg = buildPackage(1L);
            String oldToken = pkg.getAccessToken();
            when(packageRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pkg));
            when(packageRepository.save(any(ContentPackage.class))).thenAnswer(inv -> inv.getArgument(0));

            PackageResponse result = packageService.regenerateToken(1L);

            verify(packageRepository).save(argThat(p ->
                    p.getAccessToken() != null && !p.getAccessToken().equals(oldToken)));
        }
    }

    @Nested
    @DisplayName("getPublicPackage")
    class GetPublicPackage {

        @Test
        @DisplayName("throws PackageNotFoundException when package not found by slug")
        void throwsWhenNotFound() {
            when(packageRepository.findBySlugGlobally("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> packageService.getPublicPackage("nonexistent", "token"))
                    .isInstanceOf(PackageNotFoundException.class);
        }

        @Test
        @DisplayName("throws PackageNotFoundException when package is inactive")
        void throwsWhenInactive() {
            ContentPackage pkg = buildPackage(1L);
            pkg.setIsActive(false);
            when(packageRepository.findBySlugGlobally("inactive-pkg")).thenReturn(Optional.of(pkg));

            assertThatThrownBy(() -> packageService.getPublicPackage("inactive-pkg", "token"))
                    .isInstanceOf(PackageNotFoundException.class);
        }

        @Test
        @DisplayName("throws PackageNotFoundException for invalid token")
        void throwsForInvalidToken() {
            ContentPackage pkg = buildPackage(1L);
            pkg.setAccessToken("correct-token");
            when(packageRepository.findBySlugGlobally("my-pkg")).thenReturn(Optional.of(pkg));

            assertThatThrownBy(() -> packageService.getPublicPackage("my-pkg", "wrong-token"))
                    .isInstanceOf(PackageNotFoundException.class);
        }

        @Test
        @DisplayName("throws PackageNotFoundException for expired token")
        void throwsForExpiredToken() {
            ContentPackage pkg = buildPackage(1L);
            pkg.setAccessToken("token-123");
            pkg.setTokenExpiresAt(Instant.now().minusSeconds(3600));
            when(packageRepository.findBySlugGlobally("my-pkg")).thenReturn(Optional.of(pkg));

            assertThatThrownBy(() -> packageService.getPublicPackage("my-pkg", "token-123"))
                    .isInstanceOf(PackageNotFoundException.class)
                    .hasMessageContaining("expired");
        }
    }
}
