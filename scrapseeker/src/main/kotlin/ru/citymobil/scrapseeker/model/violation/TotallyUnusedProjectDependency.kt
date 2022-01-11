package ru.citymobil.scrapseeker.model.violation

data class TotallyUnusedProjectDependency(
    val projectName: String,
    val dependencyName: String
) : GradleLintViolation() {

    override fun print(): String {
        return "Module $projectName declares unused dependency $dependencyName"
    }
}
