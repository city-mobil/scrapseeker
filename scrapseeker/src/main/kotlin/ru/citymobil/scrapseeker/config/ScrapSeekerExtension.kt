package ru.citymobil.scrapseeker.config

import groovy.lang.Closure
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.KotlinClosure2
import ru.citymobil.scrapseeker.model.LibraryContent
import ru.citymobil.scrapseeker.model.ProjectMetaData

abstract class ScrapSeekerExtension(private val objectFactory: ObjectFactory) {
    abstract val customChecks: ListProperty<Closure<String?>>
    abstract val failOnViolations: Property<Boolean>
    abstract val enabled: Property<Boolean>

    val ignoredDependencies: ListProperty<String> = objectFactory.listProperty(String::class.java)

    fun ignoredDependencies(vararg dependencies: Any) {
        val newDeps = dependencies.map { dependency ->
            (dependency as? Provider<MinimalExternalModuleDependency>)
                ?.let { getDependencyAsString(it) }
                ?: dependency.toString()
        }
        ignoredDependencies.set(ignoredDependencies.get() + newDeps)
    }

    fun customCheck(closure: Closure<String?>) {
        customChecks.add(closure)
    }

    fun customCheck(kotlinClosure: KotlinClosure2<ProjectMetaData, LibraryContent, String>) {
        customCheck(kotlinClosure as Closure<String?>)
    }

    fun customCheck(check: (ProjectMetaData, LibraryContent) -> String) {
        customCheck(object : Closure<String?>(null) {
            override fun call(vararg args: Any?): String {
                return check.invoke(args[0] as ProjectMetaData, args[1] as LibraryContent)
            }
        })
    }

    fun enabled(value: Boolean) {
        enabled.set(value)
    }

    @Suppress("UnstableApiUsage")
    private fun getDependencyAsString(dependencyProvider: Provider<MinimalExternalModuleDependency>): String {
        val dependency = dependencyProvider.get()
        return dependency.module.toString() + ":" + dependency.versionConstraint.toString()
    }
}
