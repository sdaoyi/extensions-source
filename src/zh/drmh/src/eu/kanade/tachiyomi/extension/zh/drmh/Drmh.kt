package eu.kanade.tachiyomi.extension.zh.drmh

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import keiyoushi.annotation.Source
import keiyoushi.network.Fetch
import keiyoushi.network.get
import keiyoushi.source.KeiSource
import keiyoushi.source.SMangaUpdate
import keiyoushi.utils.asJsoup
import kotlinx.serialization.json.JsonElement
import okhttp3.Headers
import okhttp3.HttpUrl
import org.jsoup.nodes.Document

@Source
abstract class Drmh :
    KeiSource(),
    ConfigurableSource {

    override fun Headers.Builder.configureHeaders() = set("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")

    private fun parseMangaList(document: Document): MangasPage {
        val elements = document.select("li.hl-list-item>a")
        val manga = elements.map {
            SManga.create().apply {
                title = it.attr("title")
                setUrlWithoutDomain(it.attr("href"))
                thumbnail_url = it.attr("data-original")
            }
        }
        return MangasPage(
            mangas = manga,
            hasNextPage = elements.isNotEmpty(),
        )
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        // -1全部 1完本
        val document = client.get("$baseUrl/comics-catalog/all/ob/hits/st/all/page/$page", Fetch.DOCUMENT).asJsoup()
        return parseMangaList(document)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        val document = client.get("$baseUrl/comics-catalog/all/ob/time/st/all/page/$page", Fetch.DOCUMENT).asJsoup()
        return parseMangaList(document)
    }

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList): MangasPage {
        val document = client.get("$baseUrl/comics-searching/$query/page/$page", Fetch.DOCUMENT).asJsoup()
        return parseMangaList(document)
    }

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        if (!url.toString().startsWith(baseUrl)) return null

        val document = client.get(url, Fetch.DOCUMENT).asJsoup()
        return SManga.create().apply {
            setUrlWithoutDomain(url.toString())
            parseDetails(document)
        }
    }

    private fun SManga.parseDetails(document: Document) {
        title = document.selectFirst("h1.hl-dc-title")?.text() ?: "Unknown"
        thumbnail_url = document.selectFirst("span.hl-item-thumb")?.attr("data-original")
        description = document.select("li.hl-col-xs-12").getOrNull(5)?.text() ?: ""
        author = document.selectFirst("div.hl-data-xs>span>a")?.text()
    }

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val document = client.get(getMangaUrl(manga), Fetch.DOCUMENT).asJsoup()

        manga.parseDetails(document)
        return SMangaUpdate(manga, parseChapterList(document))
    }

    private fun parseChapterList(document: Document): List<SChapter> {
        val chapters = document.select("ul.hl-plays-list>li>a").map {
            SChapter.create().apply {
                setUrlWithoutDomain(it.attr("href"))
                name = it.attr("title")
            }
        }
        return chapters.asReversed()
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val document = client.get(getChapterUrl(chapter), Fetch.DOCUMENT).asJsoup()
        val images = document.select("img.lazy").map { it.attr("data-original") }

        return images.mapIndexed { index, imageUrl ->
            Page(index, imageUrl = imageUrl)
        }
    }

    protected override fun getFilterList(data: JsonElement?) = FilterList(
        Filter.Header("提示：搜尋時篩選無效"),
        StatusFilter(),
    )

    private abstract class UriPartFilter(name: String, values: Array<String>) : Filter.Select<String>(name, values) {
        abstract fun toUriPart(): String
    }

    private class StatusFilter : UriPartFilter("狀態", arrayOf("全部", "連載中", "已完結")) {
        override fun toUriPart() = when (state) {
            1 -> "&continued=true"
            2 -> "&continued=false"
            else -> ""
        }
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {}
}
