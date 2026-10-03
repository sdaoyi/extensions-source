import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "Rouman5"
    versionCode = 4
    contentWarning = ContentWarning.MIXED
    libVersion = "1.6"

    source {
        name = "肉漫屋"
        lang = "zh"
        baseUrl {
            mirrors(
                "https://rman8.com",
            )
        }
    }
}
