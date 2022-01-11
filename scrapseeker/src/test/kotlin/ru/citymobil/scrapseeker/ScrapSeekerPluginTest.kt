package ru.citymobil.scrapseeker

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ScrapSeekerPluginTest {

    @TempDir
    lateinit var temporaryFolder: File

    private lateinit var rootProject: Project
    private lateinit var subProject1: Project
    private lateinit var subProject2: Project

    @BeforeEach
    fun setup() {
        rootProject = ProjectBuilder.builder()
            .withName("root")
            .withProjectDir(temporaryFolder)
            .build()
        subProject1 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("sub1")
            .withProjectDir(temporaryFolder.resolve("sub1/"))
            .build()
        subProject1.projectDir.mkdirs()
        subProject1.projectDir.resolve("build.gradle").createNewFile()
        subProject2 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("sub2")
            .withProjectDir(temporaryFolder.resolve("sub2/"))
            .build()
        subProject2.projectDir.mkdirs()
        subProject2.projectDir.resolve("build.gradle").createNewFile()

        rootProject.plugins.apply(ScrapSeekerPlugin::class.java)
    }

    @Test
    fun `plugin creates all required tasks`() {
        assert(rootProject.tasks.findByName("aggregateDependenciesReport") != null)
        assert(subProject1.tasks.findByName("analyzeProjectDependencies") != null)
        assert(subProject1.tasks.findByName("analyzeLibraryDependencies") != null)
        assert(subProject2.tasks.findByName("analyzeProjectDependencies") != null)
        assert(subProject2.tasks.findByName("analyzeLibraryDependencies") != null)
    }
}
