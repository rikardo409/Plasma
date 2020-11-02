/*
 * MIT License
 *
 * Copyright (c) 2020 rajandev17
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.rajankali.plasma.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajankali.core.data.Movie
import com.rajankali.core.network.Failure
import com.rajankali.core.network.Success
import com.rajankali.plasma.data.model.MovieRequest
import com.rajankali.plasma.data.repo.MovieRepo
import com.rajankali.plasma.enums.MediaType
import com.rajankali.plasma.enums.PageState
import com.rajankali.plasma.enums.TimeWindow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class TrendingViewModel @ViewModelInject constructor(private val movieRepo: MovieRepo) : ViewModel() {

    private val _trendingLiveData = MutableLiveData<LinkedHashMap<String, List<Movie>>>()
    val trendingLiveData: LiveData<LinkedHashMap<String, List<Movie>>>
        get() = _trendingLiveData

    private val _pageStateLiveData = MutableLiveData<PageState>()
    val pageStateLiveData: LiveData<PageState>
        get() = _pageStateLiveData

    private val trendingData = linkedMapOf("Trending" to MovieRequest(MediaType.ALL, TimeWindow.WEEK),
            "Trending in Movies" to MovieRequest(MediaType.MOVIE, TimeWindow.DAY),
            "Trending in TV" to MovieRequest(MediaType.TV, TimeWindow.DAY),
            "Trending in Movies this week" to MovieRequest(MediaType.MOVIE, TimeWindow.WEEK),
            "Trending in TV this week" to MovieRequest(MediaType.TV, TimeWindow.WEEK)
    )

    init {
        fetchTrending()
        viewModelScope.launch {
            listOf("Breaking Bad", "Jurassic", "Silicon Valley", "Office",
                    "Wolverine", "Suits", "Friends", "Narcos", "Batman", "Mr.Bean").forEach {
                movieRepo.addSearchTerm(it)
            }
        }
    }

    private var error = true
    private var trendingMap = LinkedHashMap<String, List<Movie>>()

    private fun fetchTrending() = viewModelScope.launch {
        error = true
        _pageStateLiveData.postValue(PageState.LOADING)
        trendingData.forEach {
            trending(it)
        }
        if (error) {
            _pageStateLiveData.postValue(PageState.ERROR)
        } else if (trendingMap.isEmpty()) {
            _pageStateLiveData.postValue(PageState.EMPTY)
        } else {
            _trendingLiveData.postValue(trendingMap)
            _pageStateLiveData.postValue(PageState.DATA)
        }
    }

    private suspend fun trending(entry: Map.Entry<String, MovieRequest>) {
        when (val result = movieRepo.fetchTrendingMovies(page = 1, entry.value.mediaType, entry.value.timeWindow)) {
            is Success -> {
                if (result.data.results.filterNotNull().isNotEmpty()) {
                    error = false
                    trendingMap[entry.key] = result.data.results.filterNotNull()
                }
            }
            is Failure -> {
                if (error) {
                    error = true
                }
            }
        }
    }
}
