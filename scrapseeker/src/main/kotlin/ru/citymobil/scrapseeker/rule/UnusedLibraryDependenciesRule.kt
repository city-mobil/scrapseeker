package ru.citymobil.scrapseeker.rule

import org.gradle.api.Project
import ru.citymobil.scrapseeker.model.violation.GradleLintViolation
import ru.citymobil.scrapseeker.model.violation.UnusedLibraryDependency
import ru.citymobil.scrapseeker.utils.DependencyService

class UnusedLibraryDependenciesRule(
    private val dependencyService: DependencyService
) : GradleLintRule() {

    override fun apply(project: Project): List<GradleLintViolation> {
        val projectSourceImports = dependencyService.sourceImports(project)
        val dependentProjectSourceImports = dependencyService.getDependentProjects(project).map { dependencyService.sourceImports(it) }.flatten()
        val sourceImports = projectSourceImports + dependentProjectSourceImports
        val dependencies = dependencyService.libraryContents(project)
        val violations = ArrayList<GradleLintViolation>()
        dependencies.forEach { libraryContent ->
            if (!usesDependency(sourceImports, libraryContent.classNames)) {
                violations.add(
                    UnusedLibraryDependency(project.displayName, libraryContent.fullName)
                )
            }
        }
        return violations
    }

    private fun usesDependency(imports: List<String>, libraryClasses: Set<String>): Boolean {
        val haveClassDependency = libraryClasses.intersect(imports).isNotEmpty()
        if (haveClassDependency) {
            return true
        }
        // check for kotlin direct function/extension import
        val importPackages = imports.map { it.replaceAfterLast('.', "") }
        val libraryClassPackages = libraryClasses.map { it.replaceAfterLast('.', "") }
        return libraryClassPackages.intersect(importPackages).isNotEmpty()
    }
}
