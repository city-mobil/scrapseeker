# Плагин для линта зависимостей в gradle

Подключение:
```kotlin
classpath("ru.citymobil:scrapseeker:1.0")
```

Применяется к корневому gradle файлу:

```groovy
// build.gradle
plugins {
    id 'scrap-seeker'
}
// или
apply plugin: 'scrap-seeker'
```
```kotlin
// build.gradle.kts
plugins {
    id("scrap-seeker")
}
// или
apply(plugin = "scrap-seeker")
```

Добавляет к каждому подпроекту таски analyzeProjectDependencies(для линта зависимостей между проектами в
многомодульном приложении) и analyzeLibraryDependencies(для линта внешних зависимостей).
Также в основной проект добавляется таска aggregateDependenciesReport которая аггрегирует результаты
выполнения линта всех подпроектов.

### Конфигурация
customCheck - кастомное правило линта которое добавляется к основным правилам самого плагина,
принимает в себя Closure<String>, входные параметры:

projectMetaData - Метадата проекта(имя, классы,  импорты)

libraryContent - Метадата зависимости(название, классы)

В ответ ждет строку, если та не пустая, то считается что линт провален и в репорте будет выведена эта
строка.

ignoredDependencies - список зависимостей, для которых по каким то причинам мы не хотим проводить проверки
линта.

```groovy
import kotlin.jvm.functions.Function2
import ru.citymobil.plugins.unuseddependencies.model.LibraryContent
import ru.citymobil.plugins.unuseddependencies.model.ProjectMetaData
subprojects { project ->
    scrapSeeker {
        customCheck { projectMetaData, libraryContent -> if(someForbiddenLibrary()) "forbidden" else "" }
        customCheck { projectMetaData, libraryContent -> if(tooManyClasses()) "tooManyClasses" else "" }
        ignoredDependencies(
                // Используется в инфраструктуре юнит тестов, подключается плагином
                "org.jacoco:org.jacoco.ant:0.8.3"
        )

        customCheck(new Function2<ProjectMetaData, LibraryContent, String>(){
            @Override
            String invoke(ProjectMetaData projectMetaData, LibraryContent libraryContent) {
                return if(someForbiddenLibrary()) "forbidden" else ""
            }
        })
    }
}
```

```kotlin
import ru.citymobil.plugins.unuseddependencies.model.ProjectMetaData
import ru.citymobil.plugins.unuseddependencies.model.LibraryContent

subprojects { project ->
    scrapSeeker {
        ignoredDependencies(
            "org.jacoco:org.jacoco.ant:0.8.3"
        )
        customCheck(KotlinClosure2({projectMetaData, libraryContent ->
            if (libraryContent.fullName == "org.jacoco:org.jacoco.ant:0.8.2") {
                "forbidden"
            } else {
                ""
            }
        }))

        customCheck { projectMetaData, libraryContent -> if(someForbiddenLibrary()) "forbidden" else "" }
    }
}
```

## Лицензия
Проект лицензируется под [MIT License](LICENSE)

# Disclaimer

All information and source code are provided AS-IS, without express or implied warranties. Use of the source code or parts of it is at your sole discretion and risk. Citymobil LLC takes reasonable measures to ensure the relevance of the information posted in this repository, but it does not assume responsibility for maintaining or updating this repository or its parts outside the framework established by the company independently and without notifying third parties.
