package ru.citymobil.scrapseeker.model

import org.gradle.api.internal.artifacts.dependencies.AbstractExternalModuleDependency

data class LibraryContent(
    val fullName: String,
    val externalModuleDependency: AbstractExternalModuleDependency,
    val classNames: Set<String>
)
