package com.example.data

import kotlinx.coroutines.flow.Flow

class SaleRepository(private val saleDao: SaleDao) {
    val allSales: Flow<List<SaleItem>> = saleDao.getAllSales()

    suspend fun insert(sale: SaleItem) {
        saleDao.insertSale(sale)
    }

    suspend fun update(sale: SaleItem) {
        saleDao.updateSale(sale)
    }

    suspend fun delete(sale: SaleItem) {
        saleDao.deleteSale(sale)
    }

    suspend fun deleteById(id: Int) {
        saleDao.deleteById(id)
    }

    fun getSalesByMonth(month: String): Flow<List<SaleItem>> = saleDao.getSalesByMonth(month)
}
