package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.QrCodeEntity
import com.example.data.QrCodeRepository
import com.example.data.QrStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class VaultUiState(
    val selectedQrForViewer: QrCodeEntity? = null,
    val pendingImageUri: Uri? = null,
    val showAddDialog: Boolean = false,
    val qrToRename: QrCodeEntity? = null,
    val qrToDelete: QrCodeEntity? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class QrVaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: QrCodeRepository

    val qrCodes: StateFlow<List<QrCodeEntity>>

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val storageManager = QrStorageManager(application)
        repository = QrCodeRepository(database.qrCodeDao(), storageManager)

        qrCodes = repository.allQrCodes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun onPhotoPicked(uri: Uri?) {
        if (uri == null) return
        _uiState.update {
            it.copy(
                pendingImageUri = uri,
                showAddDialog = true,
                errorMessage = null
            )
        }
    }

    fun dismissAddDialog() {
        _uiState.update {
            it.copy(
                pendingImageUri = null,
                showAddDialog = false,
                isSaving = false,
                errorMessage = null
            )
        }
    }

    fun saveNewQr(name: String) {
        val uri = _uiState.value.pendingImageUri ?: return
        val finalName = name.trim().ifEmpty { "My Payment QR" }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.addQrCode(finalName, uri)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        showAddDialog = false,
                        pendingImageUri = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save QR code: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun openViewer(qrCode: QrCodeEntity) {
        _uiState.update { it.copy(selectedQrForViewer = qrCode) }
    }

    fun closeViewer() {
        _uiState.update { it.copy(selectedQrForViewer = null) }
    }

    fun requestRename(qrCode: QrCodeEntity) {
        _uiState.update { it.copy(qrToRename = qrCode) }
    }

    fun dismissRename() {
        _uiState.update { it.copy(qrToRename = null) }
    }

    fun confirmRename(id: Long, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return

        viewModelScope.launch {
            repository.renameQrCode(id, cleanName)
            _uiState.update { state ->
                val updatedViewer = if (state.selectedQrForViewer?.id == id) {
                    state.selectedQrForViewer.copy(name = cleanName)
                } else {
                    state.selectedQrForViewer
                }
                state.copy(qrToRename = null, selectedQrForViewer = updatedViewer)
            }
        }
    }

    fun requestDelete(qrCode: QrCodeEntity) {
        _uiState.update { it.copy(qrToDelete = qrCode) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(qrToDelete = null) }
    }

    fun confirmDelete() {
        val target = _uiState.value.qrToDelete ?: return
        viewModelScope.launch {
            repository.deleteQrCode(target)
            _uiState.update { state ->
                val updatedViewer = if (state.selectedQrForViewer?.id == target.id) null else state.selectedQrForViewer
                state.copy(qrToDelete = null, selectedQrForViewer = updatedViewer)
            }
        }
    }

    fun toggleFavorite(qrCode: QrCodeEntity) {
        viewModelScope.launch {
            val newFav = !qrCode.isFavorite
            repository.toggleFavorite(qrCode.id, newFav)
            _uiState.update { state ->
                val updatedViewer = if (state.selectedQrForViewer?.id == qrCode.id) {
                    state.selectedQrForViewer.copy(isFavorite = newFav)
                } else {
                    state.selectedQrForViewer
                }
                state.copy(selectedQrForViewer = updatedViewer)
            }
        }
    }

    fun shareQrCode(qrCode: QrCodeEntity) {
        repository.shareQrCode(qrCode.fileName, qrCode.name)
    }

    fun getQrFile(fileName: String): File {
        return repository.getFile(fileName)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
