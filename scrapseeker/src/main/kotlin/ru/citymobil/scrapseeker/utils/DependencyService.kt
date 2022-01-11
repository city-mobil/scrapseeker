package ru.citymobil.scrapseeker.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.artifacts.DefaultResolvedDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import ru.citymobil.scrapseeker.config.ScrapSeekerExtension
import ru.citymobil.scrapseeker.model.InternalDependency
import ru.citymobil.scrapseeker.model.LibraryContent
import ru.citymobil.scrapseeker.model.LibraryDependency
import ru.citymobil.scrapseeker.model.ProjectMetaData
import ru.citymobil.scrapseeker.model.SourceFileContent
import ru.citymobil.scrapseeker.model.SourceFileMetaData
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.jar.JarFile
import java.util.zip.ZipInputStream

class DependencyService {

    companion object {
        private val SOURCE_FILE_EXTENSIONS = listOf(".java", ".kt")

        // ?.* для кейсов когда используется import bla.bla as blabla
        private val KT_IMPORT_REGEX = "import ([a-zA-Z0-9.]+)?.*(\r\n|\n)".toRegex()
        private val JAVA_IMPORT_REGEX = "import ([a-zA-Z0-9.]+);(\r\n|\n)".toRegex()
        private val KT_PAKAGE_REGEX = "package ([a-zA-Z0-9.]+)(\r\n|\n)".toRegex()
        private val JAVA_PACKAGE_REGEX = "package ([a-zA-Z0-9.]+);(\r\n|\n)".toRegex()
        private val PROJECT_NAME_REGEX = "project '(.*?)'".toRegex()

        private const val CLASS_EXTENSION = ".class"
        private const val CLASSES_JAR = "classes.jar"
        private val ARTIFACT_EXTENSIONS = listOf(".aar", ".jar")

        private val VALID_CONFIGURATIONS = listOf(
            "RuntimeClasspath",
            "CompileClasspath",
            "implementation",
            "api"
        ).map { it.toLowerCase(Locale.ROOT) }

        private val FORBIDDEN_CONFIGURATIONS = listOf(
            "AnnotationProcessor"
        ).map { it.toLowerCase(Locale.ROOT) }

        private const val UNINITIALIZED_EXCEPTION_MESSAGE = "Service is not initialized, call init() first"
    }

    private val libraryDependencyCache = HashMap<String, Set<LibraryDependency>>()
    private val libraryContentsCache = HashMap<String, Set<LibraryContent>>()
    private val sourceFilesCache = HashMap<String, List<SourceFileContent>>()
    private val sourceImportsCache = HashMap<String, List<String>>()
    private val libraryClassesCache = HashMap<String, Set<String>>()

    private val projectDependenciesCache = HashMap<String, Set<InternalDependency>>()
    private val sourcePackagesCache = HashMap<String, Set<String>>()
    private var isServiceInitialized = false

    fun init(project: Project) {
        fetchProjectDependencies(project)
        isServiceInitialized = true
    }

    fun libraryDependencies(project: Project): Set<LibraryDependency> {
        var cache = libraryDependencyCache[project.displayName]
        if (cache == null) {
            cache = fetchLibraryDependencies(project)
            libraryDependencyCache[project.displayName] = cache
        }
        return cache
    }

    fun libraryContents(project: Project): Set<LibraryContent> {
        var cache = libraryContentsCache[project.displayName]
        if (cache == null) {
            cache = fetchLibraryContents(libraryDependencies(project))
            libraryContentsCache[project.displayName] = cache
        }
        return cache
    }

    fun sourceFiles(project: Project): List<SourceFileContent> {
        var cache = sourceFilesCache[project.displayName]
        if (cache == null) {
            cache = fetchSourceFiles(project)
            sourceFilesCache[project.displayName] = cache
        }
        return cache
    }

    fun sourceImports(project: Project): List<String> {
        var cache = sourceImportsCache[project.displayName]
        if (cache == null) {
            cache = fetchSourceImports(
                sourceFiles(project).map { item -> item.content }
            )
            sourceImportsCache[project.displayName] = cache
        }
        return cache
    }

    fun libraryClasses(resolvedDependency: DefaultResolvedDependency): Set<String> {
        var cache = libraryClassesCache[fullName(resolvedDependency)]
        if (cache == null) {
            cache = fetchLibraryClasses(resolvedDependency)
            libraryClassesCache[fullName(resolvedDependency)] = cache
        }
        return cache
    }

    fun metaData(project: Project): ProjectMetaData {
        val sourceFiles = sourceFiles(project)
        return ProjectMetaData(
            project.displayName,
            sourceFiles.map { SourceFileMetaData(it.name, it.content, fetchImports(it.content)) }
        )
    }

    fun getProjectDependencies(project: Project): Set<InternalDependency> {
        if (!isServiceInitialized) {
            throw IllegalStateException(UNINITIALIZED_EXCEPTION_MESSAGE)
        }
        return projectDependenciesCache[fetchProjectName(project.displayName)]
            ?: return emptySet()
    }

    fun getDependentProjects(project: Project): List<Project> {
        return getProjectDependencies(project).mapNotNull { internalDependency ->
            project.rootProject.subprojects.find { subProject ->
                fetchProjectName(subProject.displayName) == internalDependency.name
            }
        }
    }

    fun getProjectPackages(dependencyName: String): Set<String> {
        if (!isServiceInitialized) {
            throw IllegalStateException(UNINITIALIZED_EXCEPTION_MESSAGE)
        }
        return sourcePackagesCache[dependencyName]
            ?: emptySet()
    }

    fun findSourceModule(moduleName: String, imports: List<String>): String? {
        if (!isServiceInitialized) {
            throw IllegalStateException(UNINITIALIZED_EXCEPTION_MESSAGE)
        }
        val deps = projectDependenciesCache[moduleName]
        deps?.forEach { projectDependency ->
            val packages = sourcePackagesCache[projectDependency.name]
            packages?.forEach cycle@{ packageItem ->
                imports.forEach { importItem ->
                    if (importItem.startsWith(packageItem)) {
                        return projectDependency.name
                    }
                }
            }
            findSourceModule(projectDependency.name, imports)
        }
        return null
    }

    private fun fetchLibraryDependencies(project: Project): Set<LibraryDependency> {
        val result = HashSet<LibraryDependency>()
        project.configurations.forEach { configuration ->
            if (validConfiguration(configuration)) {
                val resolvedDependencies = configuration
                    .resolvedConfiguration
                    .firstLevelModuleDependencies
                    .mapNotNull { it as? DefaultResolvedDependency }
                    .filter { isDirectDependency(it, project) }
                val dependencies = configuration
                    .allDependencies
                    .mapNotNull { it as? DefaultExternalModuleDependency }
                dependencies.forEach { dependency ->
                    val strictResolvedDependency = resolvedDependencies
                        .find { dependency.matchesStrictly(it.module.id) }
                    if (null != strictResolvedDependency) {
                        result.add(LibraryDependency(dependency, strictResolvedDependency))
                    } else {
                        val candidates = resolvedDependencies
                            .filter { dependency.module == it.module.id.module }
                        if (candidates.size == 1) {
                            result.add(LibraryDependency(dependency, candidates.first()))
                        } else {
                            throw IllegalStateException("Failed to resolve dependency: $dependency, candidates: $candidates")
                        }
                    }
                }
            }
        }
        val extension = project.extensions.findByType(ScrapSeekerExtension::class.java)
            ?: project.rootProject.extensions.findByType(ScrapSeekerExtension::class.java)
        val ignoredDependencies = extension?.ignoredDependencies?.get() ?: emptyList()
        return result
            .filter { !ignoredDependencies.contains(fullName(it.resolvedDependency)) }
            .toSet()
    }

    private fun validConfiguration(configuration: Configuration): Boolean {
        return configuration.isCanBeResolved
            // https://youtrack.jetbrains.com/issue/KT-26834
            && !configuration.name.endsWith("DependenciesMetadata")
            && VALID_CONFIGURATIONS.any { configuration.name.toLowerCase(Locale.ROOT).contains(it) }
            && FORBIDDEN_CONFIGURATIONS.none {
            configuration.name.toLowerCase(Locale.ROOT).contains(it)
        }
    }

    private fun fetchProjectDependencies(project: Project) {
        project.rootProject.subprojects.forEach { subProject ->
            val depsSet = mutableSetOf<InternalDependency>()
            subProject.configurations
                .filter { item -> !item.name.contains("test", true) }
                .forEach { config ->
                    config.allDependencies
                        .withType(ProjectDependency::class.java).forEach { dependency ->
                            val dependencyProject = dependency.dependencyProject
                            if (dependencyProject.displayName != subProject.displayName) {
                                val item = InternalDependency(
                                    fetchProjectName(dependencyProject.displayName),
                                    dependencyProject.rootDir.absolutePath
                                )
                                depsSet.add(item)
                            }
                        }
                }
            projectDependenciesCache[fetchProjectName(subProject.displayName)] = depsSet
            sourcePackagesCache[fetchProjectName(subProject.displayName)] =
                getPackagesSet(subProject)
        }
    }

    private fun fetchLibraryContents(libraryDependencies: Set<LibraryDependency>): Set<LibraryContent> {
        return libraryDependencies.map {
            val name = fullName(it.resolvedDependency)
            LibraryContent(
                name,
                it.externalModuleDependency,
                libraryClasses(it.resolvedDependency)
            )
        }.toSet()
    }

    private fun fullName(resolvedDependency: DefaultResolvedDependency): String {
        return "%s:%s:%s".format(
            resolvedDependency.moduleGroup,
            resolvedDependency.moduleName,
            resolvedDependency.moduleVersion
        )
    }

    private fun fullName(project: Project): String {
        return "%s:%s:%s".format(
            project.group,
            project.name,
            project.version
        )
    }

    private fun fetchSourceFiles(project: Project): List<SourceFileContent> {
        val sourceDirectory = project.projectDir.resolve("src")
        if (!sourceDirectory.exists()) return emptyList()
        val result = ArrayList<SourceFileContent>()
        sourceDirectory.walkTopDown().forEach { file ->
            if (file.isFile && file.name.endsWith(SourceExtension.JAVA.ext)) {
                val content = SourceFileContent(file.name, SourceExtension.JAVA, file.readText())
                result.add(content)
            } else if (file.isFile && file.name.endsWith(SourceExtension.KOTLIN.ext)) {
                val content = SourceFileContent(file.name, SourceExtension.KOTLIN, file.readText())
                result.add(content)
            }
        }
        return result
    }

    private fun fetchSourceImports(sourceFiles: List<String>): List<String> {
        return sourceFiles.map { fetchImports(it) }.flatten().distinct()
    }

    private fun fetchImports(text: String): List<String> {
        val ktImports = KT_IMPORT_REGEX.findAll(text).map { it.groupValues[1] }.toList()
        val javaImports = JAVA_IMPORT_REGEX.findAll(text).map { it.groupValues[1] }.toList()
        return (ktImports + javaImports).distinct()
    }

    private fun fetchLibraryClasses(resolvedDependency: DefaultResolvedDependency): Set<String> {
        return resolvedDependency.allModuleArtifacts.map { fetchClasses(it.file) }.flatten().toSet()
    }

    private fun fetchClasses(artifactFile: File): Set<String> {
        if (ARTIFACT_EXTENSIONS.none { artifactFile.name.endsWith(it) }) {
            return emptySet()
        }
        val result = HashSet<String>()
        val jarFile = JarFile(artifactFile)
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val name = entry.name
            if (name.endsWith(CLASS_EXTENSION)) {
                val className = name
                    .replace(CLASS_EXTENSION, "")
                    .replace("/", ".")
                result.add(className)
            }
            if (name == CLASSES_JAR) {
                val inputStream = jarFile.getInputStream(entry)
                inputStream.use { input -> result.addAll(fetchClasses(input)) }
            }
        }
        return result
    }

    private fun fetchClasses(inputStream: InputStream): Set<String> {
        val result = HashSet<String>()
        val zipInputStream = ZipInputStream(inputStream)
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            val name = entry.name
            if (name.endsWith(CLASS_EXTENSION)) {
                val className = name
                    .replace(CLASS_EXTENSION, "")
                    .replace("/", ".")
                result.add(className)
            }
            entry = zipInputStream.nextEntry
        }
        return result
    }

    private fun isDirectDependency(
        dependency: DefaultResolvedDependency,
        project: Project
    ): Boolean {
        return dependency.parents.any { it.name == fullName(project) }
    }

    private fun fetchKotlinPackage(text: String): String {
        return KT_PAKAGE_REGEX.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun fetchJavaPackage(text: String): String {
        return JAVA_PACKAGE_REGEX.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun fetchProjectName(text: String): String {
        return PROJECT_NAME_REGEX.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun getPackagesSet(project: Project): Set<String> {
        val kotlinPackages = sourceFiles(project)
            .filter { item -> item.extension == SourceExtension.KOTLIN }
            .map { fetchKotlinPackage(it.content) }
        val javaPackages = sourceFiles(project)
            .filter { item -> item.extension == SourceExtension.JAVA }
            .map { fetchJavaPackage(it.content) }
        val packagesSet = mutableSetOf<String>()
        packagesSet.addAll(kotlinPackages)
        packagesSet.addAll(javaPackages)
        return packagesSet
    }
}
