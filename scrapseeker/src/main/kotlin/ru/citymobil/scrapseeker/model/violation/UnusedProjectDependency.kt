package ru.citymobil.scrapseeker.model.violation

data class UnusedProjectDependency(
    val projectName: String,
    val dependencyName: String,
    val changeVariant: String
) : GradleLintViolation() {

    override fun print(): String {
        return "Module $projectName doesn't use $dependencyName, replace it with $changeVariant"
    }
}
