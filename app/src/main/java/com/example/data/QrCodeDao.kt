package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QrCodeDao {
    @Query("SELECT * FROM qr_codes ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllQrCodes(): Flow<List<QrCodeEntity>>

    @Query("SELECT * FROM qr_codes WHERE id = :id")
    fun getQrCodeById(id: Long): Flow<QrCodeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQrCode(qrCode: QrCodeEntity): Long

    @Update
    suspend fun updateQrCode(qrCode: QrCodeEntity)

    @Delete
    suspend fun deleteQrCode(qrCode: QrCodeEntity)

    @Query("DELETE FROM qr_codes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE qr_codes SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Long, newName: String)

    @Query("UPDATE qr_codes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
}
