package ru.citymobil.scrapseeker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import ru.citymobil.scrapseeker.config.ScrapSeekerExtension
import ru.citymobil.scrapseeker.tasks.AggregateDependenciesReport
import ru.citymobil.scrapseeker.tasks.AnalyzeLibraryDependencies
import ru.citymobil.scrapseeker.tasks.AnalyzeProjectDependencies
import ru.citymobil.scrapseeker.tasks.DependencyServiceInitialization
import ru.citymobil.scrapseeker.utils.DependencyService
import ru.citymobil.scrapseeker.utils.ProjectDependenciesProcessor
import java.io.File

class ScrapSeekerPlugin : Plugin<Project> {

    companion object {
        private const val DEPENDENCY_SERVICE_INITIALIZATION = "dependencyServiceInitialization"
        private const val ANALYZE_PROJECT_DEPENDENCIES_TASK_NAME = "analyzeProjectDependencies"
        private const val ANALYZE_LIBRARY_DEPENDENCIES_TASK_NAME = "analyzeLibraryDependencies"
        private const val AGGREGATE_DEPENDENCIES_REPORT_TASK_NAME = "aggregateDependenciesReport"

        private const val ANALYZE_PROJECT_DEPENDENCIES_TASK_OUTPUT_FILE =
            "projectDependenciesLint.txt"
        private const val ANALYZE_LIBRARY_DEPENDENCIES_TASK_OUTPUT_FILE = "projectLibrariesLint.txt"

        private const val EXTENSION_NAME = "scrapSeeker"
    }

    private val dependencyService = DependencyService()
    private val projectDependenciesProcessor = ProjectDependenciesProcessor(dependencyService)

    override fun apply(project: Project) {
        val extension = createExtension(project)
        createTasks(project)
    }

    private fun createExtension(project: Project): ScrapSeekerExtension {
        return project.extensions.create(
            EXTENSION_NAME,
            project.objects
        )
    }

    private fun createTasks(project: Project) {
        val analyzeProjectDependenciesTasks = ArrayList<TaskProvider<AnalyzeProjectDependencies>>()
        val analyzeLibraryDependenciesTasks = ArrayList<TaskProvider<AnalyzeLibraryDependencies>>()
        val analyzeTasksOutput = ArrayList<File>()
        val initTaskProvider = project.tasks.register(
            DEPENDENCY_SERVICE_INITIALIZATION,
            DependencyServiceInitialization::class.java
        )
        initTaskProvider.get().setService(dependencyService)

        val commonDependencyFiles = listOf(
            project.projectDir.resolve("gradle/debug-deps.versions.toml"),
            project.projectDir.resolve("gradle/deps.versions.toml"),
            project.projectDir.resolve("gradle/plugins.versions.toml"),
            project.projectDir.resolve("gradle/test-deps.versions.toml")
        )

        project.subprojects.forEach { subproject ->
            subproject.extensions.create<ScrapSeekerExtension>(
                EXTENSION_NAME,
                project.objects
            )

            val buildFileExists = subproject.buildFile.exists()
            if (buildFileExists) {
                val analyzeProjectDependenciesOutput =
                    subproject.buildDir.resolve(ANALYZE_PROJECT_DEPENDENCIES_TASK_OUTPUT_FILE)
                analyzeTasksOutput.add(analyzeProjectDependenciesOutput)
                val analyzeProjectDependenciesTaskProvider = subproject.tasks.register(
                    ANALYZE_PROJECT_DEPENDENCIES_TASK_NAME,
                    AnalyzeProjectDependencies::class.java
                ) {
                    dependsOn(initTaskProvider)
                    outputFile.set(analyzeProjectDependenciesOutput)
                    dependencyFiles.set(commonDependencyFiles + subproject.buildFile)
                }
                analyzeProjectDependenciesTaskProvider.get()
                    .setProcessor(projectDependenciesProcessor)
                analyzeProjectDependenciesTasks.add(analyzeProjectDependenciesTaskProvider)

                val analyzeLibraryDependenciesOutput =
                    subproject.buildDir.resolve(ANALYZE_LIBRARY_DEPENDENCIES_TASK_OUTPUT_FILE)
                analyzeTasksOutput.add(analyzeLibraryDependenciesOutput)
                val analyzeLibraryDependenciesTaskProvider = subproject.tasks.register(
                    ANALYZE_LIBRARY_DEPENDENCIES_TASK_NAME,
                    AnalyzeLibraryDependencies::class.java
                ) {
                    dependsOn(initTaskProvider)
                    outputFile.set(analyzeLibraryDependenciesOutput)
                    dependencyFiles.set(commonDependencyFiles + subproject.buildFile)
                }
                analyzeLibraryDependenciesTaskProvider.get().setService(dependencyService)
                analyzeLibraryDependenciesTasks.add(analyzeLibraryDependenciesTaskProvider)
            }
        }
        project.tasks.register(
            AGGREGATE_DEPENDENCIES_REPORT_TASK_NAME,
            AggregateDependenciesReport::class.java
        ) {
            dependsOn(analyzeProjectDependenciesTasks + analyzeLibraryDependenciesTasks)
            analyzeTasksOutputs.set(analyzeTasksOutput)
            reportFile.set(project.buildDir.resolve("gradle-lint-report.txt"))
        }
    }
}
