package ru.citymobil.scrapseeker.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import ru.citymobil.scrapseeker.config.ScrapSeekerExtension
import ru.citymobil.scrapseeker.rule.CustomDependencyRule
import ru.citymobil.scrapseeker.rule.GradleLintRule
import ru.citymobil.scrapseeker.rule.UnusedLibraryDependenciesRule
import ru.citymobil.scrapseeker.utils.DependencyService
import java.io.File

abstract class AnalyzeLibraryDependencies : DefaultTask() {

    @get: OutputFile
    abstract val outputFile: Property<File>

    @get:InputFiles
    abstract val dependencyFiles: ListProperty<File>

    private lateinit var dependenciesService: DependencyService

    fun setService(service: DependencyService) {
        dependenciesService = service
    }

    @TaskAction
    fun action() {
        val extension = project.extensions.findByType(ScrapSeekerExtension::class.java)
            ?: project.rootProject.extensions.findByType(ScrapSeekerExtension::class.java)
        val enabled = extension?.enabled?.getOrElse(true) ?: true
        val violations = if (enabled) {
            buildRules(extension).map { it.apply(project) }.flatten()
        } else {
            emptyList()
        }
        if (violations.isNotEmpty()) {
            outputFile.get().writeText(violations.joinToString(separator = "\n") { it.print() })
        } else {
            outputFile.get().writeText("")
        }
    }

    private fun buildRules(extension: ScrapSeekerExtension?): List<GradleLintRule> {
        val rules = ArrayList<GradleLintRule>()
        rules.add(UnusedLibraryDependenciesRule(dependenciesService))
        extension?.customChecks?.get()?.forEach { customCheck ->
            rules.add(CustomDependencyRule(dependenciesService, customCheck))
        }
        return rules
    }
}
