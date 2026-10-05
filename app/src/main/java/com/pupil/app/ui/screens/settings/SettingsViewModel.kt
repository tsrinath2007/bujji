package com.pupil.app.ui.screens.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.PupilApplication
import com.pupil.app.core.llm.ModelFileInfo
import com.pupil.app.core.offline.OfflineProofHelper
import com.pupil.app.core.revision.SpacedRevisionManager
import com.pupil.app.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.pupil.app.core.prefs.OnboardingPreferences

data class SettingsUiState(
    val userName: String = "Student",
    val userEmail: String = "student@bujji.app",
    val userPhotoUri: String? = null,
    val soundEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val showEditNameDialog: Boolean = false,
    val showClearDialog: Boolean = false,
    val isClearing: Boolean = false,
    val actionMessage: String? = null
)

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: StudyRepository = StudyRepository(application)
    private val prefs = OnboardingPreferences(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            userName = prefs.userName,
            userEmail = prefs.userEmail,
            userPhotoUri = prefs.userPhotoUri
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotBlank()) {
            prefs.userName = trimmed
            _uiState.value = _uiState.value.copy(
                userName = trimmed,
                showEditNameDialog = false,
                actionMessage = "Name updated"
            )
        }
    }

    fun updatePhoto(uriString: String?) {
        prefs.userPhotoUri = uriString
        _uiState.value = _uiState.value.copy(
            userPhotoUri = uriString,
            actionMessage = if (uriString != null) "Profile photo updated" else "Profile photo removed"
        )
    }

    fun showEditNameDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showEditNameDialog = show)
    }

    fun toggleSound(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(soundEnabled = enabled)
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    fun signOut(onSignedOut: () -> Unit) {
        prefs.isAuthenticated = false
        onSignedOut()
    }

    fun showClearConfirmDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showClearDialog = show)
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearing = true, showClearDialog = false)
            try {
                repository.clearAllData()
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    actionMessage = "All study sessions and progress cleared."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    actionMessage = "Failed to clear data: ${e.message}"
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}
