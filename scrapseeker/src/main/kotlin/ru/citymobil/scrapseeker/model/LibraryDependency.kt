package ru.citymobil.scrapseeker.model

import org.gradle.api.internal.artifacts.DefaultResolvedDependency
import org.gradle.api.internal.artifacts.dependencies.AbstractExternalModuleDependency

data class LibraryDependency(
    val externalModuleDependency: AbstractExternalModuleDependency,
    val resolvedDependency: DefaultResolvedDependency
)
