import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "Semanhua"
    versionCode = 4
    contentWarning = ContentWarning.MIXED
    libVersion = "1.4"

    source {
        name = "爱涩漫画"
        lang = "zh"
        baseUrl {
            mirrors(
                "https://www.semanhua1.click",
            )
        }
    }
}
