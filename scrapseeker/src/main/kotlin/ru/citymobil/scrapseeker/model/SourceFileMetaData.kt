package ru.citymobil.scrapseeker.model

data class SourceFileMetaData(
    val name: String,
    val content: String,
    val imports: List<String>
)
