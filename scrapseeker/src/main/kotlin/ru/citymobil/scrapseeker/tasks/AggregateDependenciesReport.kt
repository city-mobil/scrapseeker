package ru.citymobil.scrapseeker.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import ru.citymobil.scrapseeker.config.ScrapSeekerExtension
import java.io.File

abstract class AggregateDependenciesReport : DefaultTask() {

    companion object {
        private const val DEFAULT_FAIL_ON_VIOLATIONS = true
    }

    @get: InputFiles
    abstract val analyzeTasksOutputs: ListProperty<File>

    @get: OutputFile
    abstract val reportFile: Property<File>

    @TaskAction
    fun action() {
        val extension = project.extensions.findByType(ScrapSeekerExtension::class.java)
            ?: project.rootProject.extensions.findByType(ScrapSeekerExtension::class.java)
        val failOnViolations = extension?.failOnViolations
            ?.getOrElse(DEFAULT_FAIL_ON_VIOLATIONS)
            ?: DEFAULT_FAIL_ON_VIOLATIONS
        val allViolations = analyzeTasksOutputs.get()
            .mapNotNull { if (it.exists()) it.readText().split("\n") else null }
            .flatten()
            .filter { it.isNotBlank() }
        if (allViolations.isNotEmpty()) {
            val violationsText =
                "Found ${allViolations.size} lint issues(${reportFile.get().path}):\n${
                    allViolations.joinToString(separator = "\n")
                }"
            reportFile.get().writeText(violationsText)
            if (failOnViolations) {
                throw GradleException(violationsText)
            } else {
                logger.warn(violationsText)
            }
        } else {
            val successText = "Analyzed ${analyzeTasksOutputs.get().size} tasks. Issues not found"
            reportFile.get().writeText(successText)
            logger.info(successText)
        }
    }
}
