enableFeaturePreview("VERSION_CATALOGS")
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/deps.versions.toml"))
        }
        create("testLibs") {
            from(files("gradle/test-deps.versions.toml"))
        }
        create("pluginLibs") {
            from(files("gradle/plugins.versions.toml"))
        }
    }
}

rootProject.name = "ScrapSeeker"
include(":scrapseeker")
