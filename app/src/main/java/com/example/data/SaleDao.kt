package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY date DESC, id DESC")
    fun getAllSales(): Flow<List<SaleItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleItem)

    @Update
    suspend fun updateSale(sale: SaleItem)

    @Delete
    suspend fun deleteSale(sale: SaleItem)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM sales WHERE date LIKE :month || '%'")
    fun getSalesByMonth(month: String): Flow<List<SaleItem>> // format month: YYYY-MM
}
