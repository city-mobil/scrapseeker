package ru.citymobil.scrapseeker.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import ru.citymobil.scrapseeker.config.ScrapSeekerExtension
import ru.citymobil.scrapseeker.rule.GradleLintRule
import ru.citymobil.scrapseeker.rule.UnusedProjectDependenciesRule
import ru.citymobil.scrapseeker.utils.ProjectDependenciesProcessor
import java.io.File

abstract class AnalyzeProjectDependencies : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:InputFiles
    abstract val dependencyFiles: ListProperty<File>

    private lateinit var projectDependenciesProcessor: ProjectDependenciesProcessor

    fun setProcessor(processor: ProjectDependenciesProcessor) {
        projectDependenciesProcessor = processor
    }

    @TaskAction
    fun action() {
        val extension = project.extensions.findByType(ScrapSeekerExtension::class.java)
            ?: project.rootProject.extensions.findByType(ScrapSeekerExtension::class.java)
        val enabled = extension?.enabled?.getOrElse(true) ?: true
        val violations = if (enabled) {
            buildRules().map { item -> item.apply(project) }.flatten()
        } else {
            emptyList()
        }
        if (violations.isNotEmpty()) {
            outputFile.get().asFile.writeText(violations.joinToString(separator = "\n") { it.print() })
        } else {
            outputFile.get().asFile.writeText("")
        }
    }

    private fun buildRules(): List<GradleLintRule> {
        return listOf(
            UnusedProjectDependenciesRule(
                projectDependenciesProcessor
            )
        )
    }
}
