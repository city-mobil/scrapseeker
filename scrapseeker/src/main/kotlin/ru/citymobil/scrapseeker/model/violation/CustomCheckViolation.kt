package ru.citymobil.scrapseeker.model.violation

data class CustomCheckViolation(private val violation: String) : GradleLintViolation() {

    override fun print(): String = violation
}
