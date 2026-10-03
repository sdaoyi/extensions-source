import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "Niaoniaohm"
    versionCode = 4
    contentWarning = ContentWarning.MIXED
    libVersion = "1.4"

    source {
        name = "鸟鸟韩漫"
        lang = "zh"
        baseUrl {
            mirrors(
                "https://nnhanman6.com",
                "https://nnhm95.com",
                "https://nnhm93.com",
                "https://nnhm92.com",
                "https://nnhm91.com",
                "https://nnhm81.com",
            )
        }
    }
}
