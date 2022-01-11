package ru.citymobil.scrapseeker.model

import ru.citymobil.scrapseeker.utils.SourceExtension

data class SourceFileContent(
    val name: String,
    val extension: SourceExtension,
    val content: String
)
