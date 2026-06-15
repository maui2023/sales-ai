package com.example.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import com.example.ReminderReceiver

class SaleViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = SaleRepository(database.saleDao)
    
    val allSales: StateFlow<List<SaleItem>> = repository.allSales
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Active filters
    val activeMonth = MutableStateFlow(sdfMonth.format(Date()))
    val selectedDate = MutableStateFlow(sdfDate.format(Date()))

    // Shared preferences for setting persistence
    private val prefs = application.getSharedPreferences("sale_ai_prefs", Context.MODE_PRIVATE)

    val isDailyReminderEnabled = MutableStateFlow(prefs.getBoolean("daily_reminder", true))

    // Dynamic sales state mappings
    val activeMonthSales: StateFlow<List<SaleItem>> = combine(allSales, activeMonth) { sales, month ->
        sales.filter { it.date.startsWith(month) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDateSales: StateFlow<List<SaleItem>> = combine(allSales, selectedDate) { sales, date ->
        sales.filter { it.date == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Advisor State variables
    val aiAdviceText = MutableStateFlow<String>("")
    val isAiLoading = MutableStateFlow<Boolean>(false)

    init {
        // Enforce daily reminder alarm at boot/startup
        syncDailyReminderAlarm(isDailyReminderEnabled.value)
    }

    // Add, Edit, Delete database triggers
    fun addSale(sale: SaleItem) {
        viewModelScope.launch {
            repository.insert(sale)
            // Reset AI advice when data changes to prompt a fresh analysis
            aiAdviceText.value = ""
        }
    }

    fun deleteSale(sale: SaleItem) {
        viewModelScope.launch {
            repository.delete(sale)
            aiAdviceText.value = ""
        }
    }

    fun deleteSaleById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            aiAdviceText.value = ""
        }
    }

    fun updateSale(sale: SaleItem) {
        viewModelScope.launch {
            repository.update(sale)
            aiAdviceText.value = ""
        }
    }

    // Trigger Daily Reminder alarm sync
    fun toggleDailyReminder(enabled: Boolean) {
        isDailyReminderEnabled.value = enabled
        prefs.edit().putBoolean("daily_reminder", enabled).apply()
        syncDailyReminderAlarm(enabled)
    }

    private fun syncDailyReminderAlarm(enabled: Boolean) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1224,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (enabled) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                // Set the notification at 8:00 PM (20:00) nightly
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1) // Schedule for tomorrow instead
                }
            }
            // Set inexact repeating alarm (safe, efficient, and auto-cancelled by OS if requested)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } else {
            alarmManager.cancel(pendingIntent)
        }
    }

    // Request Advice from Gemini AI Advisor
    fun generateAiAdvice(monthSales: List<SaleItem>) {
        if (monthSales.isEmpty()) {
            aiAdviceText.value = "💡 Sila masukkan data jualan jualan terlebih dahulu untuk membolehkan Sale AI membuat penilaian prestasi perniagaan anda."
            return
        }

        viewModelScope.launch {
            isAiLoading.value = true
            
            val monthLabel = activeMonth.value
            val totalSales = monthSales.sumOf { it.amount }
            val totalCost = monthSales.sumOf { it.cost }
            val totalProfit = totalSales - totalCost
            val count = monthSales.size
            
            // Extract top category
            val topCategory = monthSales.groupBy { it.category }
                .maxByOrNull { it.value.sumOf { item -> item.amount } }?.key ?: "Tiada"

            // Get breakdown by usahawan
            val rawBreakdown = monthSales.groupBy { it.entrepreneurName }
                .map { (name, items) ->
                    "  • $name: RM ${String.format("%.2f", items.sumOf { it.amount })} (Kos: RM ${String.format("%.2f", items.sumOf { it.cost })}, Untung: RM ${String.format("%.2f", items.sumOf { it.profit })})"
                }.joinToString("\n")

            val advice = GeminiAdvisor.generateBusinessAdvice(
                monthName = monthLabel,
                totalSales = totalSales,
                totalCost = totalCost,
                totalProfit = totalProfit,
                salesCount = count,
                topCategory = topCategory,
                usahawanBreakdown = rawBreakdown
            )
            
            aiAdviceText.value = advice
            isAiLoading.value = false
        }
    }
}
