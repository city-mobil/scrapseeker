package ru.citymobil.scrapseeker.utils

import org.gradle.api.Project
import ru.citymobil.scrapseeker.model.ProjectDependencyVerificationModel

class ProjectDependenciesProcessor(
    private val dependencyService: DependencyService
) {

    fun getUnusedDependenciesForProject(project: Project): List<ProjectDependencyVerificationModel> {
        val deps = dependencyService.getProjectDependencies(project)
        val imports = dependencyService.sourceImports(project)
        if (!deps.isNullOrEmpty()) {
            val unusedDeps = deps.toMutableSet()
            deps.forEach { projectDependency ->
                val packages = dependencyService.getProjectPackages(projectDependency.name)
                packages.forEach cycle@{ packageItem ->
                    imports.forEach { importItem ->
                        if (importItem.startsWith(packageItem)) {
                            unusedDeps.remove(projectDependency)
                            return@cycle
                        }
                    }
                }
            }
            val resultList = mutableListOf<ProjectDependencyVerificationModel>()
            unusedDeps.forEach {
                val result = dependencyService.findSourceModule(it.name, imports)
                val model = ProjectDependencyVerificationModel(
                    it.name,
                    result
                )
                resultList.add(model)
            }
            return resultList
        }
        return emptyList()
    }
}
