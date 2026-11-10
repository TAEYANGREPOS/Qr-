package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.QrCodeDao
import com.example.data.QrCodeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: QrCodeDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.qrCodeDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("QR Pocket", appName)
    }

    @Test
    fun `insert and retrieve qr code entity`() = runBlocking {
        val qr = QrCodeEntity(
            name = "Personal UPI",
            fileName = "qr_test_123.png",
            createdAt = 1000L
        )
        val id = dao.insertQrCode(qr)
        assertTrue(id > 0)

        val list = dao.getAllQrCodes().first()
        assertEquals(1, list.size)
        assertEquals("Personal UPI", list[0].name)
        assertEquals("qr_test_123.png", list[0].fileName)
    }

    @Test
    fun `update qr code name and favorite`() = runBlocking {
        val qr = QrCodeEntity(
            name = "Shop",
            fileName = "shop.png"
        )
        val id = dao.insertQrCode(qr)

        dao.updateName(id, "Grocery Shop")
        dao.updateFavorite(id, true)

        val retrieved = dao.getQrCodeById(id).first()
        assertNotNull(retrieved)
        assertEquals("Grocery Shop", retrieved?.name)
        assertEquals(true, retrieved?.isFavorite)
    }

    @Test
    fun `delete qr code`() = runBlocking {
        val qr = QrCodeEntity(
            name = "Mother",
            fileName = "mom.png"
        )
        val id = dao.insertQrCode(qr)
        dao.deleteById(id)

        val list = dao.getAllQrCodes().first()
        assertTrue(list.isEmpty())
    }
}
