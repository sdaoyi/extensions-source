import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "Drmh"
    versionCode = 4
    contentWarning = ContentWarning.MIXED
    libVersion = "1.4"

    source {
        name = "大人漫画"
        lang = "zh"
        baseUrl {
            mirrors(
                "https://drmh3.com",
            )
        }
    }
}
