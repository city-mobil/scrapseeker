package ru.citymobil.scrapseeker.rule

import groovy.lang.Closure
import org.gradle.api.Project
import ru.citymobil.scrapseeker.model.violation.CustomCheckViolation
import ru.citymobil.scrapseeker.model.violation.GradleLintViolation
import ru.citymobil.scrapseeker.utils.DependencyService

class CustomDependencyRule(
    private val dependencyService: DependencyService,
    private val customCheck: Closure<String?>
) : GradleLintRule() {

    override fun apply(project: Project): List<GradleLintViolation> {
        val projectMetaData = dependencyService.metaData(project)
        val dependencies = dependencyService.libraryContents(project)
        val violations = ArrayList<GradleLintViolation>()
        dependencies.forEach { libraryContent ->
            val violationText = customCheck.call(projectMetaData, libraryContent)
            if (violationText != null && violationText.isNotEmpty()) {
                violations.add(CustomCheckViolation(violationText))
            }
        }
        return violations
    }
}
