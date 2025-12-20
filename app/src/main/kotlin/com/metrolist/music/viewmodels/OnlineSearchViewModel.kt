package com.metrolist.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.pages.SearchSummaryPage
import com.metrolist.music.constants.EnablePersonalizedSearchKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.models.ItemsPage
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import com.metrolist.music.db.DatabaseDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val database: DatabaseDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = URLDecoder.decode(savedStateHandle.get<String>("query")!!, "UTF-8")
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            val enablePersonalizedSearch = context.dataStore.get(EnablePersonalizedSearchKey, false)

            filter.collect { filter ->
                if (filter == null) {
                    if (summaryPage == null) {
                        val localResults = if (enablePersonalizedSearch) {
                            database.searchSongs(query, 5).first() +
                                    database.searchArtists(query, 5).first() +
                                    database.searchAlbums(query, 5).first() +
                                    database.searchPlaylists(query, 5).first()
                        } else {
                            emptyList()
                        }

                        YouTube
                            .searchSummary(query)
                            .onSuccess {
                                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                                summaryPage =
                                    it.copy(
                                        sections = it.sections.map { section ->
                                            section.copy(
                                                items = (localResults.filterIsInstance<YTItem>() + section.items)
                                                    .distinctBy { item -> item.id }
                                            )
                                        }
                                    ).filterExplicit(
                                        hideExplicit,
                                    ).filterVideoSongs(hideVideoSongs)
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        val localResults = if (enablePersonalizedSearch) {
                            when (filter) {
                                YouTube.SearchFilter.SONG -> database.searchSongs(query).first()
                                YouTube.SearchFilter.ARTIST -> database.searchArtists(query).first()
                                YouTube.SearchFilter.ALBUM -> database.searchAlbums(query).first()
                                YouTube.SearchFilter.PLAYLIST -> database.searchPlaylists(query).first()
                                else -> emptyList()
                            }
                        } else {
                            emptyList()
                        }

                        YouTube
                            .search(query, filter)
                            .onSuccess { result ->
                                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                                viewStateMap[filter.value] =
                                    ItemsPage(
                                        (localResults.filterIsInstance<YTItem>() + result.items)
                                            .distinctBy { it.id }
                                            .filterExplicit(
                                                hideExplicit,
                                            )
                                            .filterVideoSongs(hideVideoSongs),
                                        result.continuation,
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val newItems = searchResult.items
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + newItems).distinctBy { it.id },
                    searchResult.continuation
                )
            }
        }
    }
}
