package ru.citymobil.scrapseeker.rule

import org.gradle.api.Project
import ru.citymobil.scrapseeker.model.violation.GradleLintViolation

abstract class GradleLintRule {

    abstract fun apply(project: Project): List<GradleLintViolation>
}
