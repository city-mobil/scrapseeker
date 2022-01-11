package ru.citymobil.scrapseeker.utils

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedModuleVersion
import org.gradle.api.internal.artifacts.DefaultResolvedDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.citymobil.scrapseeker.config.ScrapSeekerExtension
import ru.citymobil.scrapseeker.model.LibraryDependency
import ru.citymobil.scrapseeker.model.ProjectMetaData
import ru.citymobil.scrapseeker.model.SourceFileContent
import ru.citymobil.scrapseeker.model.SourceFileMetaData
import java.io.File

class DependencyServiceTest {

    @TempDir
    lateinit var temporaryFolder: File

    private lateinit var dependencyService: DependencyService
    private lateinit var project: Project
    private lateinit var externalModuleDependency: DefaultExternalModuleDependency
    private lateinit var resolvedDependency: DefaultResolvedDependency
    private lateinit var configurationContainer: ConfigurationContainer
    private lateinit var configuration: Configuration
    private lateinit var extensionContainer: ExtensionContainer
    private lateinit var resolvedConfiguration: ResolvedConfiguration
    private lateinit var dependencySet: DependencySet
    private lateinit var moduleIdentifier: ModuleIdentifier
    private lateinit var moduleVersionIdentifier: ModuleVersionIdentifier
    private lateinit var resolvedModuleVersion: ResolvedModuleVersion
    private lateinit var projectDependency: DefaultResolvedDependency

    @BeforeEach
    fun setup() {
        dependencyService = DependencyService()
        configurationContainer = mockk()
        extensionContainer = mockk()
        project = mockk()
        every { project.displayName } returns "displayName"
        every { project.rootProject } returns project
        every { project.configurations } returns configurationContainer
        every { project.extensions } returns extensionContainer
        every { project.projectDir } returns temporaryFolder
        every { project.parent } returns null
        every { project.group } returns "group"
        every { project.name } returns "name"
        every { project.version } returns "version"
        externalModuleDependency = mockk()
        resolvedDependency = mockk()
        configuration = mockk()
        resolvedConfiguration = mockk()
        dependencySet = mockk()
        moduleIdentifier = mockk()
        moduleVersionIdentifier = mockk()
        resolvedModuleVersion = mockk()
        val configurationIterator = mockk<MutableIterator<Configuration>>()
        every { configurationContainer.iterator() } returns configurationIterator
        every { configurationIterator.hasNext() } returnsMany listOf(true, false)
        every { configurationIterator.next() } returns configuration
        every { configuration.isCanBeResolved } returns true
        every { configuration.name } returns "implementation"
        every { configuration.resolvedConfiguration } returns resolvedConfiguration
        every { resolvedConfiguration.firstLevelModuleDependencies } returns setOf(
            resolvedDependency
        )
        every { configuration.allDependencies } returns dependencySet
        val dependencyIterator = mockk<MutableIterator<Dependency>>()
        every { dependencySet.iterator() } returns dependencyIterator
        every { dependencyIterator.hasNext() } returnsMany listOf(true, false)
        every { dependencyIterator.next() } returns externalModuleDependency
        every { extensionContainer.findByType(any<Class<ScrapSeekerExtension>>()) } returns null
        every { externalModuleDependency.matchesStrictly(any()) } returns true
        every { externalModuleDependency.module } returns moduleIdentifier
        every { resolvedDependency.module } returns resolvedModuleVersion
        every { resolvedModuleVersion.id } returns moduleVersionIdentifier
        every { moduleVersionIdentifier.module } returns moduleIdentifier
        every { resolvedDependency.moduleGroup } returns "group"
        every { resolvedDependency.moduleName } returns "name"
        every { resolvedDependency.moduleVersion } returns "version"
        every { resolvedDependency.allModuleArtifacts } returns emptySet()
        projectDependency = mockk<DefaultResolvedDependency>()
        every { projectDependency.moduleGroup } returns "group"
        every { projectDependency.moduleName } returns "name"
        every { projectDependency.moduleVersion } returns "version"
        every { projectDependency.name } returns "group:name:version"
        every { resolvedDependency.parents } returns setOf(projectDependency)
    }

    @Test
    fun `fetch library dependencies skip unresolvable configurations`() {
        every { configuration.isCanBeResolved } returns false

        val dependencies = dependencyService.libraryDependencies(project)
        assert(dependencies.isEmpty())
    }

    @Test
    fun `fetch library dependencies skip metadataDependencies configurations`() {
        every { configuration.name } returns "implementationDependenciesMetadata"

        val dependencies = dependencyService.libraryDependencies(project)
        assert(dependencies.isEmpty())
    }

    @Test
    fun `fetch library dependencies throw exception if there few resolved dependency candidates`() {
        val resolvedCandidates = setOf<DefaultResolvedDependency>(mockk(), mockk(), mockk())
        every { resolvedConfiguration.firstLevelModuleDependencies } returns resolvedCandidates
        every { externalModuleDependency.matchesStrictly(any()) } returns false
        resolvedCandidates.forEach { candidate ->
            every { candidate.module } returns resolvedModuleVersion
            every { candidate.parents } returns setOf(projectDependency)
        }

        try {
            dependencyService.libraryDependencies(project)
            assert(false)
        } catch (exc: Exception) {
            assert(exc is IllegalStateException)
        }
    }

    @Test
    fun `fetch library dependencies filtered out by ignored dependencies extension`() {
        val unfilteredDependency = mockk<DefaultExternalModuleDependency>()
        val unfilteredResolvedDependency = mockk<DefaultResolvedDependency>()
        val unfilteredModuleIdentifier = mockk<ModuleIdentifier>()
        val unfilteredModuleVersionIdentifier = mockk<ModuleVersionIdentifier>()
        val unfilteredResolvedModuleVersion = mockk<ResolvedModuleVersion>()
        every { unfilteredDependency.module } returns unfilteredModuleIdentifier
        every { unfilteredResolvedModuleVersion.id } returns unfilteredModuleVersionIdentifier
        every {
            unfilteredDependency.matchesStrictly(
                refEq(
                    unfilteredModuleVersionIdentifier,
                    inverse = true
                )
            )
        } returns false
        every { unfilteredDependency.matchesStrictly(refEq(unfilteredModuleVersionIdentifier)) } returns true
        every { unfilteredResolvedDependency.moduleGroup } returns "unfiltered"
        every { unfilteredResolvedDependency.moduleName } returns "unfiltered"
        every { unfilteredResolvedDependency.moduleVersion } returns "unfiltered"
        every { unfilteredResolvedDependency.module } returns unfilteredResolvedModuleVersion
        every { unfilteredResolvedDependency.parents } returns setOf(projectDependency)

        val filteredDependency = mockk<DefaultExternalModuleDependency>()
        val filteredResolvedDependency = mockk<DefaultResolvedDependency>()
        val filteredModuleIdentifier = mockk<ModuleIdentifier>()
        val filteredModuleVersionIdentifier = mockk<ModuleVersionIdentifier>()
        val filteredResolvedModuleVersion = mockk<ResolvedModuleVersion>()
        every { filteredDependency.module } returns filteredModuleIdentifier
        every { filteredResolvedModuleVersion.id } returns filteredModuleVersionIdentifier
        every {
            filteredDependency.matchesStrictly(
                refEq(
                    filteredModuleVersionIdentifier,
                    inverse = true
                )
            )
        } returns false
        every { filteredDependency.matchesStrictly(refEq(filteredModuleVersionIdentifier)) } returns true
        every { filteredResolvedDependency.moduleGroup } returns "filtered"
        every { filteredResolvedDependency.moduleName } returns "filtered"
        every { filteredResolvedDependency.moduleVersion } returns "filtered"
        every { filteredResolvedDependency.module } returns filteredResolvedModuleVersion
        every { filteredResolvedDependency.parents } returns setOf(projectDependency)

        val dependencyIterator = mockk<MutableIterator<Dependency>>()
        every { dependencySet.iterator() } returns dependencyIterator
        every { dependencyIterator.hasNext() } returnsMany listOf(true, true, false)
        every { dependencyIterator.next() } returnsMany listOf(
            unfilteredDependency,
            filteredDependency
        )
        every { resolvedConfiguration.firstLevelModuleDependencies } returns setOf(
            unfilteredResolvedDependency,
            filteredResolvedDependency
        )

        val property = mockk<ListProperty<String>>()
        every { property.get() } returns listOf("filtered:filtered:filtered")
        val extension = mockk<ScrapSeekerExtension>()
        every { extension.ignoredDependencies } returns property
        every { extensionContainer.findByType(any<Class<ScrapSeekerExtension>>()) } returns extension

        val dependencies = dependencyService.libraryDependencies(project)
        val expectedDependencies =
            setOf(LibraryDependency(unfilteredDependency, unfilteredResolvedDependency))

        assert(dependencies == expectedDependencies) {
            "expected $expectedDependencies dependencies, but was $dependencies"
        }
    }

    @Test
    fun `fetch library dependencies uses cache`() {
        dependencyService.libraryDependencies(project)
        verify(exactly = 1) { project.configurations }
        dependencyService.libraryDependencies(project)
        verify(exactly = 1) { project.configurations }
    }

    @Test
    fun `fetch library contents uses cache`() {
        dependencyService.libraryContents(project)
        verify(exactly = 1) { project.configurations }
        dependencyService.libraryContents(project)
        verify(exactly = 1) { project.configurations }
    }

    @Test
    fun `fetch source files return empty list if there no src directory`() {
        val sourceFiles = dependencyService.sourceFiles(project)
        assert(sourceFiles.isEmpty())
    }

    @Test
    fun `fetch source files return empty list if there no src files`() {
        temporaryFolder.resolve("src").mkdirs()
        val sourceFiles = dependencyService.sourceFiles(project)
        assert(sourceFiles.isEmpty())
    }

    @Test
    fun `fetch source files return all source files in src directory`() {
        val sourceFolder = temporaryFolder.resolve("src")
        sourceFolder.mkdirs()
        val kotlinContent = "KOTLIN"
        val javaContent = "JAVA"
        val noSourceContent = "RESOURCE"
        sourceFolder.resolve("Kotlin.kt").writeText(kotlinContent)
        sourceFolder.resolve("Java.java").writeText(javaContent)
        sourceFolder.resolve("Res.xml").writeText(noSourceContent)

        val sourceFiles = dependencyService.sourceFiles(project)
        val expected = listOf(
            SourceFileContent("Kotlin.kt", SourceExtension.KOTLIN, kotlinContent),
            SourceFileContent("Java.java", SourceExtension.JAVA, javaContent)
        )
        assert(sourceFiles == expected) {
            "expected $expected but was $sourceFiles"
        }
    }

    @Test
    fun `fetch source files uses cache`() {
        dependencyService.sourceFiles(project)
        verify(exactly = 1) { project.projectDir }
        dependencyService.sourceFiles(project)
        verify(exactly = 1) { project.projectDir }
    }

    @Test
    fun `fetch source imports return all imports from source files in src directory`() {
        val sourceFolder = temporaryFolder.resolve("src")
        sourceFolder.mkdirs()
        val kotlinContent = """
            import ru.citymobil.Kotlin1
            import ru.citymobil.Kotlin2

            class Kotlin {}
        """.trimIndent()
        val javaContent = """
            import ru.citymobil.Java1;
            import ru.citymobil.Java2;

            public class Java {}
        """.trimIndent()
        sourceFolder.resolve("Kotlin.kt").writeText(kotlinContent)
        sourceFolder.resolve("Java.java").writeText(javaContent)

        val sourceImports = dependencyService.sourceImports(project)
        val expected = listOf(
            "ru.citymobil.Kotlin1",
            "ru.citymobil.Kotlin2",
            "ru.citymobil.Java1",
            "ru.citymobil.Java2"
        )
        assert(sourceImports == expected) {
            "expected $expected but was $sourceImports"
        }
    }

    @Test
    fun `fetch source imports uses cache`() {
        dependencyService.sourceImports(project)
        verify(exactly = 1) { project.projectDir }
        dependencyService.sourceImports(project)
        verify(exactly = 1) { project.projectDir }
    }

    @Test
    fun `fetch meta data returns meta data with empty sources when there no source directory`() {
        val metaData = dependencyService.metaData(project)
        val expectedMetaData = ProjectMetaData(project.displayName, emptyList())
        assert(metaData == expectedMetaData) {
            "expected $expectedMetaData, but was $metaData"
        }
    }

    @Test
    fun `fetch meta data returns all sources in source directory`() {
        val sourceFolder = temporaryFolder.resolve("src")
        sourceFolder.mkdirs()
        val kotlinContent = """
            import ru.citymobil.Kotlin1
            import ru.citymobil.Kotlin2

            class Kotlin {}
        """.trimIndent()
        val javaContent = """
            import ru.citymobil.Java1;
            import ru.citymobil.Java2;

            public class Java {}
        """.trimIndent()
        val kotlinImports = listOf("ru.citymobil.Kotlin1", "ru.citymobil.Kotlin2")
        val javaImports = listOf("ru.citymobil.Java1", "ru.citymobil.Java2")
        sourceFolder.resolve("Kotlin.kt").writeText(kotlinContent)
        sourceFolder.resolve("Java.java").writeText(javaContent)

        val metaData = dependencyService.metaData(project)
        val expectedMetaData = ProjectMetaData(
            project.displayName,
            listOf(
                SourceFileMetaData("Kotlin.kt", kotlinContent, kotlinImports),
                SourceFileMetaData("Java.java", javaContent, javaImports)
            )
        )
        assert(metaData == expectedMetaData) {
            "expected $expectedMetaData, but was $metaData"
        }
    }

    @Test
    fun `fetch meta data uses cache`() {
        dependencyService.metaData(project)
        verify(exactly = 1) { project.projectDir }
        dependencyService.metaData(project)
        verify(exactly = 1) { project.projectDir }
    }
}
