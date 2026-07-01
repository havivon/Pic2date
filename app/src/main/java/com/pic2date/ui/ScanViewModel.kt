package com.pic2date.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pic2date.R
import com.pic2date.calendar.CalendarRepository
import com.pic2date.model.EventDraft
import com.pic2date.ocr.OcrService
import com.pic2date.parser.EventParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Phase { IDLE, SCANNING, CONFIRM, SAVED, ERROR }

data class ScanUiState(
    val phase: Phase = Phase.IDLE,
    val draft: EventDraft? = null,
    val errorRes: Int? = null,
    val savedEventId: Long? = null,
    val savedTitle: String = "",
    val imageUri: Uri? = null,
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val ocrService = OcrService(app)
    private val calendarRepository = CalendarRepository(app)

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    /** Runs OCR + parsing for the given image and moves to the confirm screen. */
    fun scan(uri: Uri, mimeType: String?) {
        _state.update { it.copy(phase = Phase.SCANNING, imageUri = uri, errorRes = null) }
        viewModelScope.launch {
            when (val result = ocrService.scan(uri, mimeType)) {
                is OcrService.Result.Success -> {
                    val parsed = withContext(Dispatchers.Default) { EventParser.parse(result.text) }
                    _state.update {
                        it.copy(phase = Phase.CONFIRM, draft = EventDraft.from(parsed))
                    }
                }
                OcrService.Result.NoText ->
                    _state.update { it.copy(phase = Phase.ERROR, errorRes = R.string.error_no_text) }
                is OcrService.Result.Error ->
                    _state.update { it.copy(phase = Phase.ERROR, errorRes = R.string.error_ocr_failed) }
            }
        }
    }

    fun updateDraft(draft: EventDraft) {
        _state.update { it.copy(draft = draft) }
    }

    /** Persists the current draft. Caller must hold WRITE_CALENDAR. */
    fun save() {
        val draft = _state.value.draft ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { calendarRepository.insert(draft) }
            when (result) {
                is CalendarRepository.SaveResult.Success -> _state.update {
                    it.copy(phase = Phase.SAVED, savedEventId = result.eventId, savedTitle = draft.title)
                }
                CalendarRepository.SaveResult.NoCalendar -> _state.update {
                    it.copy(errorRes = R.string.error_no_calendar)
                }
                is CalendarRepository.SaveResult.Error -> _state.update {
                    it.copy(errorRes = R.string.error_save_failed)
                }
            }
        }
    }

    fun calendarRepository(): CalendarRepository = calendarRepository

    fun errorShown() = _state.update { it.copy(errorRes = null) }

    /** Returns to the home screen, clearing any in-progress scan. */
    fun reset() {
        _state.value = ScanUiState()
    }

    override fun onCleared() {
        ocrService.release()
        super.onCleared()
    }
}
