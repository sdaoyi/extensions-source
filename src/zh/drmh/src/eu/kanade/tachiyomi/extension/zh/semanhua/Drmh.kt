package eu.kanade.tachiyomi.extension.zh.drmh

import android.content.SharedPreferences
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.annotation.Source
import keiyoushi.utils.getPreferences
import okhttp3.Request
import okhttp3.Response

@Source abstract class Drmh :
    HttpSource(),
    ConfigurableSource {

    override val supportsLatest = true

    private val preferences: SharedPreferences = getPreferences()

    override fun headersBuilder() = super.headersBuilder()
        .set("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")

    private fun getItems(response: Response): MangasPage {
        val document = response.asJsoup()
        val elements = document.select("li.hl-list-item>a")
        val manga = elements.map {
            SManga.create().apply {
                title = it.attr("title")
                url = it.attr("href")
                thumbnail_url = it.attr("data-original")
            }
        }
        return MangasPage(
            mangas = manga,
            hasNextPage = elements.isNotEmpty(),
        )
    }

    // -1全部 1完本
    override fun popularMangaRequest(page: Int) = GET("$baseUrl/comics-catalog/all/ob/hits/st/all/page/$page", headers)
    override fun popularMangaParse(response: Response): MangasPage = getItems(response)

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/comics-catalog/all/ob/time/st/all/page/$page", headers)
    override fun latestUpdatesParse(response: Response): MangasPage = getItems(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = GET("$baseUrl/comics-searching/$query/page/$page", headers)
    override fun searchMangaParse(response: Response): MangasPage = getItems(response)

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val document = response.asJsoup()
        title = document.selectFirst("h1.hl-dc-title")?.text() ?: "Unknown"
        thumbnail_url = document.selectFirst("span.hl-item-thumb")?.attr("data-original")
        description = document.select("li.hl-col-xs-12").getOrNull(5)?.text() ?: ""
        author = document.selectFirst("div.hl-data-xs>span>a")?.text()
//        author = document.select("p.detail-main-info-author").getOrNull(2)?.text() ?: ""
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = document.select("ul.hl-plays-list>li>a").map {
            SChapter.create().apply {
                url = it.attr("href")
                name = it.attr("title")
            }
        }
        return chapters.asReversed()
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val images = document.select("img.lazy").map { it.attr("data-original") }

        return images.mapIndexed { index, imageUrl ->
            Page(index, imageUrl = imageUrl)
        }
    }

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException()

    override fun getFilterList() = FilterList(
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
