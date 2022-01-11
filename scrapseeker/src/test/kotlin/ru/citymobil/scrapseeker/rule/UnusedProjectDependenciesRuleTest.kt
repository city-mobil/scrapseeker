package ru.citymobil.scrapseeker.rule

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.gradle.api.Project
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.citymobil.scrapseeker.config.ScrapSeekerExtension
import ru.citymobil.scrapseeker.model.ProjectDependencyVerificationModel
import ru.citymobil.scrapseeker.model.violation.TotallyUnusedProjectDependency
import ru.citymobil.scrapseeker.model.violation.UnusedProjectDependency
import ru.citymobil.scrapseeker.utils.ProjectDependenciesProcessor

class UnusedProjectDependenciesRuleTest {

    private companion object {
        const val PROJECT_NAME = "tariffs"
    }

    private val project: Project = mockk()
    private val projectDependenciesProcessor: ProjectDependenciesProcessor = mockk()
    private val unusedExtension: ScrapSeekerExtension = mockk()
    private lateinit var rule: UnusedProjectDependenciesRule

    @BeforeEach
    fun setup() {
        every { project.displayName } returns PROJECT_NAME
        rule = UnusedProjectDependenciesRule(projectDependenciesProcessor)
    }

    @AfterEach
    fun clear() {
        clearAllMocks()
    }

    @Test
    @DisplayName(
        "Given: no ignored dependencies " +
            "When: no change variant of dependency " +
            "Then: get totally unused dependencies"
    )
    fun getTotallyUnusedViolations() {
        val featureApi1 = ProjectDependencyVerificationModel(
            ":feature-api1",
            null
        )
        val featureApi2 = ProjectDependencyVerificationModel(
            ":feature-api2",
            null
        )
        every {
            projectDependenciesProcessor.getUnusedDependenciesForProject(project)
        } returns listOf(featureApi1, featureApi2)

        every {
            project.extensions.findByType(ScrapSeekerExtension::class.java)
        } returns null
        every {
            project.rootProject.extensions.findByType(ScrapSeekerExtension::class.java)
        } returns null

        val violations = rule.apply(project)
        val expectedViolation1 = TotallyUnusedProjectDependency(
            PROJECT_NAME,
            ":feature-api1"
        )
        val expectedViolation2 = TotallyUnusedProjectDependency(
            PROJECT_NAME,
            ":feature-api2"
        )
        val expectedResult = listOf(expectedViolation1, expectedViolation2)
        assertEquals(expectedResult, violations)
    }

    @Test
    @DisplayName(
        "Given: no ignored dependencies " +
            "When: dependency with change variant " +
            "Then: get unused dependencies"
    )
    fun getUnusedDependencyViolations() {
        val featureApi1 = ProjectDependencyVerificationModel(
            ":feature-api1",
            ":common"
        )
        val featureApi2 = ProjectDependencyVerificationModel(
            ":feature-api2",
            ":common"
        )
        every { projectDependenciesProcessor.getUnusedDependenciesForProject(project) } returns listOf(
            featureApi1, featureApi2
        )

        every {
            project.extensions.findByType(ScrapSeekerExtension::class.java)
        } returns null
        every {
            project.rootProject.extensions.findByType(ScrapSeekerExtension::class.java)
        } returns null

        val violations = rule.apply(project)
        val expectedViolation1 = UnusedProjectDependency(
            PROJECT_NAME,
            ":feature-api1",
            ":common"
        )
        val expectedViolation2 = UnusedProjectDependency(
            PROJECT_NAME,
            ":feature-api2",
            ":common"
        )
        val expectedResult = listOf(expectedViolation1, expectedViolation2)
        assertEquals(expectedResult, violations)
    }

    @Test
    @DisplayName(
        "When: dependency is ignored" +
            "Then: violations doesn't contain it"
    )
    fun noIgnoredDependencyInViolations() {
        val ignoredDependencyName = ":feature-api1"
        val featureApi1 = ProjectDependencyVerificationModel(
            ignoredDependencyName,
            ":common"
        )
        val featureApi2 = ProjectDependencyVerificationModel(
            ":feature-api2",
            ":common"
        )
        every { projectDependenciesProcessor.getUnusedDependenciesForProject(project) } returns listOf(
            featureApi1, featureApi2
        )

        every { project.extensions.findByType(ScrapSeekerExtension::class.java) } returns unusedExtension
        every { unusedExtension.ignoredDependencies.get() } returns listOf(ignoredDependencyName)

        val violations = rule.apply(project)
        val expectedViolation2 = UnusedProjectDependency(
            PROJECT_NAME,
            ":feature-api2",
            ":common"
        )
        val expectedResult = listOf(expectedViolation2)
        assertEquals(expectedResult, violations)
    }

    @Test
    @DisplayName(
        "When: no dependencies" +
            "Then: no violations"
    )
    fun noDependenciesNoViolations() {
        every { projectDependenciesProcessor.getUnusedDependenciesForProject(project) } returns listOf()

        every {
            project.extensions.findByType(ScrapSeekerExtension::class.java)
        } returns null
        every {
            project.rootProject.extensions.findByType(ScrapSeekerExtension::class.java)
        } returns null

        val violations = rule.apply(project)
        assertTrue(violations.isEmpty())
    }

    @Test
    @DisplayName(
        "Given: ignored dependency" +
            "When: project hasn't dependencies" +
            "Then: no violations"
    )
    fun ignoredDependencyNoViolations() {
        every {
            projectDependenciesProcessor.getUnusedDependenciesForProject(project)
        } returns listOf()

        every {
            project.extensions.findByType(ScrapSeekerExtension::class.java)
        } returns unusedExtension
        every { unusedExtension.ignoredDependencies.get() } returns listOf(":common")

        val violations = rule.apply(project)
        assertTrue(violations.isEmpty())
    }
}
