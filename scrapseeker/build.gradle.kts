plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("kotlin")
    `kotlin-dsl`
}

gradlePlugin {
    plugins.register("scrap-seeker") {
        id = "scrap-seeker"
        implementationClass = "ru.citymobil.scrapseeker.ScrapSeekerPlugin"
    }
}

repositories {
    mavenCentral()
    google()
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

group = "ru.citymobil"
version = "1.0"

dependencies {
    implementation(pluginLibs.kotlin)
    implementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(platform(testLibs.junitBom))
    testImplementation(testLibs.jupiterModule)
    testImplementation(testLibs.mockk)
}