package ru.citymobil.scrapseeker.rule

import groovy.lang.Closure
import io.mockk.every
import io.mockk.mockk
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.citymobil.scrapseeker.model.LibraryContent
import ru.citymobil.scrapseeker.model.ProjectMetaData
import ru.citymobil.scrapseeker.model.violation.CustomCheckViolation
import ru.citymobil.scrapseeker.utils.DependencyService

class CustomDependencyRuleTest {

    private lateinit var project: Project
    private lateinit var dependencyService: DependencyService
    private lateinit var rule: CustomDependencyRule
    private lateinit var externalModuleDependency: DefaultExternalModuleDependency
    private lateinit var customCheck: Closure<String?>

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().withName("root").build()
        dependencyService = mockk()
        externalModuleDependency = mockk()
        customCheck = mockk()
        rule = CustomDependencyRule(dependencyService, customCheck)
        val dependency = "ru.citymobil.SomeDependency"
        every { dependencyService.metaData(any()) } returns ProjectMetaData("root", emptyList())
        every { dependencyService.libraryContents(any()) } returns setOf(
            LibraryContent("name", externalModuleDependency, setOf(dependency))
        )
    }

    @Test
    fun `if custom rule return empty string there no violation`() {
        every { customCheck.call(*anyVararg()) } returns ""
        val violations = rule.apply(project)
        assert(violations.isEmpty())
    }

    @Test
    fun `if custom rule return non empty string there is violation`() {
        every { customCheck.call(*anyVararg()) } returns "violation"
        val expectedViolations = listOf(CustomCheckViolation("violation"))
        val violations = rule.apply(project)
        assert(violations == expectedViolations) {
            "expected $expectedViolations but was $violations"
        }
    }
}
