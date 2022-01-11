package ru.citymobil.scrapseeker.rule

import io.mockk.every
import io.mockk.mockk
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.citymobil.scrapseeker.model.LibraryContent
import ru.citymobil.scrapseeker.utils.DependencyService

class UnusedLibraryDependenciesRuleTest {

    private lateinit var project: Project
    private lateinit var dependencyService: DependencyService
    private lateinit var rule: UnusedLibraryDependenciesRule
    private lateinit var externalModuleDependency: DefaultExternalModuleDependency

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().withName("root").build()
        dependencyService = mockk()
        every { dependencyService.getDependentProjects(any()) } returns emptyList()
        externalModuleDependency = mockk()
        rule = UnusedLibraryDependenciesRule(dependencyService)
    }

    @Test
    fun `no violations if every library used`() {
        val dependency = "ru.citymobil.SomeDependency"
        every { dependencyService.sourceImports(any()) } returns listOf(dependency)
        every { dependencyService.libraryContents(any()) } returns setOf(
            LibraryContent("name", externalModuleDependency, setOf(dependency))
        )
        val violations = rule.apply(project)
        assert(violations.isEmpty()) {
            "found violation[${violations.map { it.print() }}] when all libraries used"
        }
    }

    @Test
    fun `found violations if there unused library`() {
        val usedDependency = "ru.citymobil.SomeDependency"
        val unusedDependency = "com.library.SomeUnusedDependency"
        every { dependencyService.sourceImports(any()) } returns listOf(usedDependency)
        every { dependencyService.libraryContents(any()) } returns setOf(
            LibraryContent("usedLibrary", externalModuleDependency, setOf(usedDependency)),
            LibraryContent("unusedLibrary", externalModuleDependency, setOf(unusedDependency))
        )

        val violations = rule.apply(project)
        assert(violations.size == 1) {
            "unexpected violations count(expected 1, but was ${violations.size}): $violations"
        }

        val expectedViolationText =
            "Module root project 'root' declared unused dependency unusedLibrary"
        val realViolationText = violations.first().print()
        assert(expectedViolationText == realViolationText) {
            "expected violation text: $expectedViolationText, but was $realViolationText"
        }
    }
}
