package eu.kanade.tachiyomi.extension.zh.semanhua

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

@Source abstract class Semanhua :
    HttpSource(),
    ConfigurableSource {

    override val supportsLatest = true

    private val preferences: SharedPreferences = getPreferences()

    override fun headersBuilder() = super.headersBuilder()
        .set("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")

    private fun getItems(response: Response): MangasPage {
        val document = response.asJsoup()
        val elements = document.select("ul.manga-list-2>li>div>a")
        val manga = elements.map {
            SManga.create().apply {
                title = it.attr("title")
                url = it.attr("href")
                thumbnail_url = it.selectFirst("img")?.attr("data-original")
            }
        }
        return MangasPage(
            mangas = manga,
            hasNextPage = elements.isNotEmpty(),
        )
    }

    private fun getSearchItems(response: Response): MangasPage {
        val document = response.asJsoup()

        val elements = document.select("ul.book-list > li > div.book-list-cover>a ")

        val manga = elements.map {
            SManga.create().apply {
                title = it.attr("title")
                url = it.attr("href")
                thumbnail_url = it.selectFirst("img")?.attr("data-original") ?: ""
            }
        }

        val result = MangasPage(
            mangas = manga,
            hasNextPage = false,
        )

        return result
    }

    // -1全部 1完本
    override fun popularMangaRequest(page: Int) = GET("$baseUrl/booklist?page=$page&cate=全部&end=-1", headers)
    override fun popularMangaParse(response: Response): MangasPage = getItems(response)

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/booklist?page=$page&cate=全部&end=1", headers)
    override fun latestUpdatesParse(response: Response): MangasPage = getItems(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = GET("$baseUrl/search?keyword=$query", headers)
    override fun searchMangaParse(response: Response): MangasPage = getSearchItems(response)

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val document = response.asJsoup()
        title = document.selectFirst("p.detail-main-info-title")?.text() ?: "Unknown"
        thumbnail_url = document.selectFirst("div.detail-main-cover>img")?.attr("data-original")
        description = document.selectFirst("p.detail-desc")?.text()
        author = document.select("p.detail-main-info-author").getOrNull(2)?.text() ?: ""
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = document.select("#detail-list-select>li>a").map {
            SChapter.create().apply {
                url = it.attr("href")
                name = it.attr("title")
            }
        }
        return chapters.asReversed()
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val images = document.select("#cp_img>img").map { it.attr("data-original") }

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
