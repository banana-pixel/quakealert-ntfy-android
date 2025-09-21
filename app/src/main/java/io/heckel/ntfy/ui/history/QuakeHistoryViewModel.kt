package io.heckel.ntfy.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.heckel.ntfy.history.QuakeHistoryRepository
import io.heckel.ntfy.history.QuakeReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuakeHistoryViewModel(
    private val repository: QuakeHistoryRepository = QuakeHistoryRepository()
) : ViewModel() {
    private val _reports = MutableLiveData<List<QuakeReport>>(emptyList())
    val reports: LiveData<List<QuakeReport>> = _reports

    private val _uiState = MutableLiveData(HistoryUiState())
    val uiState: LiveData<HistoryUiState> = _uiState

    private var hasLoaded = false

    fun loadInitial() {
        if (hasLoaded) {
            return
        }
        fetchReports()
    }

    fun refresh() {
        fetchReports(forceRefresh = true)
    }

    private fun fetchReports(forceRefresh: Boolean = false) {
        _uiState.postValue(HistoryUiState(loading = true))
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.fetchReports()
                _reports.postValue(result)
                _uiState.postValue(HistoryUiState())
                hasLoaded = true
            } catch (e: Exception) {
                _uiState.postValue(
                    HistoryUiState(
                        loading = false,
                        errorMessage = e.message ?: e.javaClass.simpleName
                    )
                )
                if (!forceRefresh) {
                    hasLoaded = false
                }
            }
        }
    }

    data class HistoryUiState(
        val loading: Boolean = false,
        val errorMessage: String? = null
    )
}
