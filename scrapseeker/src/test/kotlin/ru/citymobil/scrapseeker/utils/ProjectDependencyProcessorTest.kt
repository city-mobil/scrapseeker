package ru.citymobil.scrapseeker.utils

import io.mockk.every
import io.mockk.mockk
import org.gradle.api.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.citymobil.scrapseeker.model.InternalDependency
import ru.citymobil.scrapseeker.model.ProjectDependencyVerificationModel

class ProjectDependencyProcessorTest {

    private companion object {
        const val PROJECT_NAME = "tariffs"
        const val IMPORT_CLASSA = "com.example.test.classA"
        const val IMPORT_CLASSB = "com.example.test.classB"
        const val IMPORT_CLASSC = "com.example.test.classC"
    }

    private val dependencyService: DependencyService = mockk()
    private val project: Project = mockk()
    private val processor = ProjectDependenciesProcessor(dependencyService)

    @BeforeEach
    fun before() {
        every { project.displayName } returns PROJECT_NAME
    }

    @Test
    @DisplayName(
        "When: project hasn't dependencies" +
            "Then: empty result"
    )
    fun noDependencies() {
        every { dependencyService.getProjectDependencies(project) } returns emptySet()
        every { dependencyService.sourceImports(project) } returns getSourceImports()
        every { dependencyService.getProjectPackages(any()) } returns emptySet()
        val unusedDependencies = processor.getUnusedDependenciesForProject(project)
        assertTrue(unusedDependencies.isEmpty())
    }

    @Test
    @DisplayName(
        "When: all dependencies are used" +
            "Then: empty result"
    )
    fun allDependenciesAreUsedEmptyResult() {
        val feature1Name = "feature-api1"
        val feature2Name = "feature-api2"
        val dependency1 = InternalDependency(
            feature1Name,
            "feature-api1/src"
        )
        val dependency2 = InternalDependency(
            feature2Name,
            "feature-api2/src"
        )
        val featureApi1Packages = setOf(
            "com.example.feature1.data",
            "com.example.feature1.domain",
            "com.example.feature1.presentation"
        )
        val featureApi2Packages = setOf(
            "com.example.feature2.data",
            "com.example.feature2.domain",
            "com.example.feature2.presentation"
        )
        val sourceImports = listOf(
            "com.example.test.ClassA",
            "com.example.feature1.domain.TestInteractor",
            "com.example.feature2.domain.TestInteractor"
        )
        every { dependencyService.getProjectDependencies(project) } returns setOf(
            dependency1,
            dependency2
        )
        every { dependencyService.sourceImports(project) } returns sourceImports
        every { dependencyService.getProjectPackages(feature1Name) } returns featureApi1Packages
        every { dependencyService.getProjectPackages(feature2Name) } returns featureApi2Packages
        every { dependencyService.findSourceModule(any(), any()) } returns ""
        val unusedDependencies = processor.getUnusedDependenciesForProject(project)
        assertTrue(unusedDependencies.isEmpty())
    }

    @Test
    @DisplayName(
        "Given: no transitive dependencies" +
            "When: project doesn't have imports from dependency" +
            "Then: dependency is unused"
    )
    fun totallyUnusedDependency() {
        val feature1Name = "feature-api1"
        val feature2Name = "feature-api2"
        val dependency1 = InternalDependency(
            feature1Name,
            "feature-api1/src"
        )
        val dependency2 = InternalDependency(
            feature2Name,
            "feature-api2/src"
        )
        val featureApi1Packages = setOf(
            "com.example.feature1.data",
            "com.example.feature1.domain",
            "com.example.feature1.presentation"
        )
        val featureApi2Packages = setOf(
            "com.example.feature2.data",
            "com.example.feature2.domain",
            "com.example.feature2.presentation"
        )
        val sourceImports = listOf(
            "com.example.test.ClassA",
            "com.example.test.ClassB",
            "com.example.feature1.domain.TestInteractor"
        )
        every { dependencyService.getProjectDependencies(project) } returns setOf(
            dependency1,
            dependency2
        )
        every { dependencyService.sourceImports(project) } returns sourceImports
        every { dependencyService.getProjectPackages(feature1Name) } returns featureApi1Packages
        every { dependencyService.getProjectPackages(feature2Name) } returns featureApi2Packages
        every { dependencyService.findSourceModule(any(), any()) } returns null
        val unusedDependencies = processor.getUnusedDependenciesForProject(project)
        val expectedResult = listOf(
            ProjectDependencyVerificationModel(
                feature2Name,
                null
            )
        )
        assertEquals(expectedResult, unusedDependencies)
    }

    @Test
    @DisplayName(
        "Given: transitive dependencies exist" +
            "When: project doesn't have imports from dependency" +
            "Then: dependency is unused with change offer"
    )
    fun unusedDependency() {
        val feature1Name = "feature-api1"
        val feature2Name = "feature-api2"
        val commonName = "common"
        val dependency1 = InternalDependency(
            feature1Name,
            "feature-api1/src"
        )
        val dependency2 = InternalDependency(
            feature2Name,
            "feature-api2/src"
        )
        val featureApi1Packages = setOf(
            "com.example.feature1.data",
            "com.example.feature1.domain",
            "com.example.feature1.presentation"
        )
        val featureApi2Packages = setOf(
            "com.example.feature2.data",
            "com.example.feature2.domain",
            "com.example.feature2.presentation"
        )
        val sourceImports = listOf(
            "com.example.test.ClassA",
            "com.example.test.ClassB",
            "com.example.feature1.domain.TestInteractor"
        )
        every { dependencyService.getProjectDependencies(project) } returns setOf(
            dependency1,
            dependency2
        )
        every { dependencyService.sourceImports(project) } returns sourceImports
        every { dependencyService.getProjectPackages(feature1Name) } returns featureApi1Packages
        every { dependencyService.getProjectPackages(feature2Name) } returns featureApi2Packages
        every { dependencyService.findSourceModule(any(), any()) } returns commonName
        val unusedDependencies = processor.getUnusedDependenciesForProject(project)
        val expectedResult = listOf(
            ProjectDependencyVerificationModel(
                feature2Name,
                commonName
            )
        )
        assertEquals(expectedResult, unusedDependencies)
    }

    private fun getSourceImports(): List<String> {
        return listOf(
            IMPORT_CLASSA,
            IMPORT_CLASSB,
            IMPORT_CLASSC
        )
    }
}
