package ru.citymobil.scrapseeker.rule

import org.gradle.api.Project
import ru.citymobil.scrapseeker.config.ScrapSeekerExtension
import ru.citymobil.scrapseeker.model.violation.GradleLintViolation
import ru.citymobil.scrapseeker.model.violation.TotallyUnusedProjectDependency
import ru.citymobil.scrapseeker.model.violation.UnusedProjectDependency
import ru.citymobil.scrapseeker.utils.ProjectDependenciesProcessor

class UnusedProjectDependenciesRule(
    private val projectDependenciesProcessor: ProjectDependenciesProcessor
) : GradleLintRule() {

    override fun apply(project: Project): List<GradleLintViolation> {
        val ignoredDependencies = getIgnoredDependencies(project)
        val unusedDeps = projectDependenciesProcessor.getUnusedDependenciesForProject(project)
            .filter { item -> !ignoredDependencies.contains(item.name) }
        val violations = mutableListOf<GradleLintViolation>()
        unusedDeps.forEach {
            val violation = if (it.changeVariant.isNullOrBlank()) {
                TotallyUnusedProjectDependency(
                    project.displayName,
                    it.name
                )
            } else {
                UnusedProjectDependency(
                    project.displayName,
                    it.name,
                    it.changeVariant
                )
            }
            violations.add(violation)
        }
        return violations
    }

    private fun getIgnoredDependencies(project: Project): List<String> {
        val extension = project.extensions.findByType(ScrapSeekerExtension::class.java)
            ?: project.rootProject.extensions.findByType(ScrapSeekerExtension::class.java)
        return extension?.ignoredDependencies?.get() ?: emptyList()
    }
}
