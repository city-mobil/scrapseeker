package ru.citymobil.scrapseeker.model.violation

class UnusedLibraryDependency(
    private val projectName: String,
    private val dependencyName: String
) : GradleLintViolation() {

    override fun print(): String {
        return "Module $projectName declared unused dependency $dependencyName"
    }
}
