package ru.citymobil.scrapseeker.model

data class ProjectMetaData(
    val name: String,
    val sources: List<SourceFileMetaData>
)
