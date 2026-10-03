package eu.kanade.tachiyomi.extension.zh.niaoniaohm

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

@Source
abstract class Niaoniaohm :
    HttpSource(),
    ConfigurableSource {

    override val supportsLatest = true

    private val preferences: SharedPreferences = getPreferences()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/comics/all/ob/hits/st/all/page/$page", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val elements = document.select("ul.col_3_1>li")
        val manga = elements.map {
            SManga.create().apply {
                title = it.selectFirst("a")!!.attr("title")
                thumbnail_url = it.selectFirst("img")!!.attr("src")
                url = it.selectFirst("a")!!.attr("href")
            }
        }
        return MangasPage(
            mangas = manga,
            hasNextPage = elements.isNotEmpty(),
        )
    }

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/comics/all/ob/time/st/all/page/$page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = GET("$baseUrl/search/$query/page/$page", headers)

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val elements = document.select("div.imgBox li>a.ImgA")

        val manga = elements.map {
            SManga.create().apply {
                title = it.attr("title")
                thumbnail_url = it.selectFirst("img")?.attr("src")
                url = it.attr("href")
            }
        }
        val pages = document.select("div.pagination-wrap li:last-child")
        if (pages.size == 0) {
            return MangasPage(manga, false)
        }
        val nextPage = pages[0].text()
        return MangasPage(manga, nextPage == "下一页")
    }

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val document = response.asJsoup()
        title = document.selectFirst("div#Cover img")!!.attr("title")
        thumbnail_url = document.selectFirst("div#Cover img")!!.attr("src")
        description = document.selectFirst("p.txtDesc")!!.text()
        author = document.selectFirst("p.txtItme")!!.text()
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = document.select("div#list li>a").map {
            SChapter.create().apply {
                url = it.attr("href")
                name = it.text()
            }
        }

        return chapters
    }

    override fun pageListParse(response: Response): List<Page> {
        val images = response.asJsoup().select("div.img-wrap>img").map { it.attr("data-src") }

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
