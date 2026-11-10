package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AddQrDialog
import com.example.ui.DeleteConfirmDialog
import com.example.ui.EditQrDialog
import com.example.ui.MainVaultScreen
import com.example.ui.QrVaultViewModel
import com.example.ui.QrViewerScreen
import com.example.ui.theme.QrPocketTheme

class MainActivity : ComponentActivity() {

    private val viewModel: QrVaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QrPocketTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QrPocketApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun QrPocketApp(viewModel: QrVaultViewModel) {
    val qrCodes by viewModel.qrCodes.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val viewerTarget = uiState.selectedQrForViewer

    if (viewerTarget != null) {
        val imageFile = viewModel.getQrFile(viewerTarget.fileName)
        QrViewerScreen(
            qrCode = viewerTarget,
            imageFile = imageFile,
            onBack = { viewModel.closeViewer() },
            onShare = { viewModel.shareQrCode(viewerTarget) },
            onRename = { viewModel.requestRename(viewerTarget) },
            onDelete = { viewModel.requestDelete(viewerTarget) },
            onToggleFavorite = { viewModel.toggleFavorite(viewerTarget) }
        )
    } else {
        MainVaultScreen(
            qrCodes = qrCodes,
            errorMessage = uiState.errorMessage,
            onClearError = { viewModel.clearError() },
            onPhotoPicked = { uri -> viewModel.onPhotoPicked(uri) },
            onSelectQr = { qr -> viewModel.openViewer(qr) },
            onToggleFavorite = { qr -> viewModel.toggleFavorite(qr) },
            onShareQr = { qr -> viewModel.shareQrCode(qr) },
            onRenameQr = { qr -> viewModel.requestRename(qr) },
            onDeleteQr = { qr -> viewModel.requestDelete(qr) },
            getQrFile = { fileName -> viewModel.getQrFile(fileName) }
        )
    }

    // Add QR dialog
    if (uiState.showAddDialog && uiState.pendingImageUri != null) {
        AddQrDialog(
            imageUri = uiState.pendingImageUri!!,
            isSaving = uiState.isSaving,
            onDismiss = { viewModel.dismissAddDialog() },
            onSave = { name -> viewModel.saveNewQr(name) }
        )
    }

    // Rename dialog
    uiState.qrToRename?.let { qr ->
        EditQrDialog(
            qrCode = qr,
            onDismiss = { viewModel.dismissRename() },
            onConfirm = { newName -> viewModel.confirmRename(qr.id, newName) }
        )
    }

    // Delete dialog
    uiState.qrToDelete?.let { qr ->
        DeleteConfirmDialog(
            qrCode = qr,
            onDismiss = { viewModel.dismissDelete() },
            onConfirm = { viewModel.confirmDelete() }
        )
    }
}
