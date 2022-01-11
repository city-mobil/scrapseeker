package ru.citymobil.scrapseeker.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import ru.citymobil.scrapseeker.utils.DependencyService

abstract class DependencyServiceInitialization : DefaultTask() {

    private lateinit var dependenciesService: DependencyService

    fun setService(service: DependencyService) {
        dependenciesService = service
    }

    @TaskAction
    fun action() {
        dependenciesService.init(project)
    }
}
