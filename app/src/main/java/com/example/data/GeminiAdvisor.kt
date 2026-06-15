package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAdvisor {
    private const val TAG = "GeminiAdvisor"
    
    // We prioritize the recommended gemini-3.5-flash model
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateBusinessAdvice(
        monthName: String,
        totalSales: Double,
        totalCost: Double,
        totalProfit: Double,
        salesCount: Int,
        topCategory: String,
        usahawanBreakdown: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Checks if api key is mock/placeholder or empty
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext """
                💡 *Saranan Pintar AI (Mod Luar Talian)*:
                
                • Tahniah kerana merekodkan **$salesCount** urus niaga pada bulan $monthName!
                • Jualan Kasar anda bernilai **RM ${String.format("%.2f", totalSales)}** dengan keuntungan **RM ${String.format("%.2f", totalProfit)}**.
                • *Tips*: Sila sambungkan **Kunci API Gemini** pada panel Secrets di AI Studio untuk mendapatkan analisa perniagaan digital masa-nyata yang tersusun khas untuk anda!
            """.trimIndent()
        }

        val prompt = """
            Anda adalah Penasihat Perniagaan Pintar (AI Business Consultant) bertaraf profesional khas untuk aplikasi "Sale AI".
            Berikan satu penilaian prestasi perniagaan dan cadangan strategi pertubuhan (maksimum 4-5 bullet points berimpak tinggi) untuk usahawan perniagaan tempatan.
            Tulis maklum balas anda dalam Bahasa Melayu yang sopan, optimistik, bersemangat dan ringkas. Jangan terlalu teknikal, beri fokus kajian kes/langkah praktikal.
            
            Laporan Prestasi Kewangan Bulan ($monthName):
            - Bilangan Jualan Direkod: $salesCount transaksi
            - Jumlah Jualan Kasar: RM ${String.format("%.2f", totalSales)}
            - Jumlah Kos Sediaan: RM ${String.format("%.2f", totalCost)}
            - Untung Bersih: RM ${String.format("%.2f", totalProfit)} (Margin Keuntungan: ${if (totalSales > 0) String.format("%.1f", (totalProfit / totalSales) * 100) else "0"}%)
            - Kategori Jualan Tertinggi: $topCategory
            - Pecahan Jualan Mengikut Usahawan/Kakitangan:
            $usahawanBreakdown
            
            Format laporan anda menggunakan penanda bullet (* atau •) yang kemas beserta emoji yang bersesuaian perenggan demi perenggan. Mulakan terus dengan ulasan, elakkan pembuka jenaka atau ucapan mesra yang berjela-jela.
        """.trimIndent()

        try {
            // Build direct JSON body for Gemini REST payload
            val rootJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partTextObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partTextObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
                
                val generationConfig = JSONObject().apply {
                    put("temperature", 0.7)
                }
                put("generationConfig", generationConfig)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "Koneksi ke Gemini gagal (Ralat HTTP ${response.code}). Sila semak sambungan internet anda."
            }

            val bodyString = response.body?.string() ?: return@withContext "Respon internet daripada AI kosong."
            Log.d(TAG, "Gemini Response Body: $bodyString")
            
            val responseJson = JSONObject(bodyString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                if (content != null) {
                    val parts = content.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val firstPart = parts.getJSONObject(0)
                        return@withContext firstPart.optString("text", "Tiada analisis teks dijana oleh AI.")
                    }
                }
            }
            return@withContext "AI tidak dapat memproses laporan data jualan jualan anda buat masa ini."
        } catch (e: Exception) {
            Log.e(TAG, "Error generating advice: ${e.message}", e)
            return@withContext "Ralat ketika berhubung dengan Sale AI Assistant: ${e.localizedMessage}. Pastikan kunci API Gemini anda sah."
        }
    }
}
