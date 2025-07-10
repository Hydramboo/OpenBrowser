package rj.browser.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import rj.browser.utils.event.Event
import rj.browser.utils.event.event
import rj.browser.utils.okhttp.call

sealed class CheckVersionState {
    object Idle: CheckVersionState()
    object Loading: CheckVersionState()
    data class Success(val serverVersion: Int): CheckVersionState()
    data class Error(val error: String): CheckVersionState()
}

data class AboutUiState(
    val checkVersionState: CheckVersionState = CheckVersionState.Idle
)

const val SERVER_VERSION_URL = "https://dgithub.xyz/RJMultiDev/Updater/raw/refs/heads/user/latest"

class AboutActivityViewModel: ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState

    val httpClient = OkHttpClient()

    fun checkForUpdate() {
        viewModelScope.launch {
            runCatching {
                _uiState.value = uiState.value.copy(checkVersionState = CheckVersionState.Loading)
                val resp = withContext(Dispatchers.IO) {
                    httpClient.call {
                        get()
                        url(SERVER_VERSION_URL)
                    }.execute()
                }
                _uiState.value = when {
                    resp.code != 200 -> uiState.value.copy(checkVersionState = CheckVersionState.Error("Server returned ${resp.code}"))
                    else -> {
                        val version = resp.body.string().toIntOrNull()
                        if (version == null) uiState.value.copy(checkVersionState = CheckVersionState.Error("Invalid version number"))
                        else uiState.value.copy(checkVersionState = CheckVersionState.Success(version))
                    }
                }
            }.onFailure {
                _uiState.value = uiState.value.copy(checkVersionState = CheckVersionState.Error(it.toString()))
            }
        }
    }

}