package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // Format: YYYY-MM-DD
    val amount: Double, // Harga jualan kasar
    val cost: Double, // Modal / Kos barang
    val entrepreneurName: String, // Nama usahawan
    val category: String, // Kategori (Makanan, Pakaian, Runcit, Lain-lain)
    val description: String = "" // Nota tambahan
) {
    val profit: Double
        get() = amount - cost
}
