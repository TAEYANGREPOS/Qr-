package com.example.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.io.File

class QrCodeRepository(
    private val dao: QrCodeDao,
    private val storageManager: QrStorageManager
) {
    val allQrCodes: Flow<List<QrCodeEntity>> = dao.getAllQrCodes()

    fun getQrCodeById(id: Long): Flow<QrCodeEntity?> = dao.getQrCodeById(id)

    suspend fun addQrCode(name: String, sourceUri: Uri): Long {
        val fileName = storageManager.saveImageFromUri(sourceUri)
        val entity = QrCodeEntity(
            name = name.trim(),
            fileName = fileName,
            createdAt = System.currentTimeMillis()
        )
        return dao.insertQrCode(entity)
    }

    suspend fun renameQrCode(id: Long, newName: String) {
        dao.updateName(id, newName.trim())
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteQrCode(entity: QrCodeEntity) {
        storageManager.deleteFile(entity.fileName)
        dao.deleteQrCode(entity)
    }

    fun getFile(fileName: String): File = storageManager.getFile(fileName)

    fun shareQrCode(fileName: String, name: String) = storageManager.shareQrCode(fileName, name)
}
