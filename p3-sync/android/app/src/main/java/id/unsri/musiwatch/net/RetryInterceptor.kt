// Sumber: Modul Praktikum P1, Langkah 4 (verbatim + import)
package id.unsri.musiwatch.net

import java.io.IOException
import kotlin.random.Random
import okhttp3.Interceptor
import okhttp3.Response

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    private val retryable = setOf(429, 502, 503, 504)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method !in setOf("GET", "PUT", "DELETE")) return chain.proceed(request)
        var attempt = 0
        while (true) {
            try {
                val response = chain.proceed(request)
                if (response.code !in retryable || attempt >= maxRetries) return response
                response.close()
            } catch (e: IOException) {
                if (attempt >= maxRetries) throw e
            }
            val backoffMs = (500L shl attempt) + Random.nextLong(0, 250)  // 0,5 s, 1 s, 2 s + jitter
            Thread.sleep(backoffMs)   // interceptor berjalan di thread latar OkHttp
            attempt++
        }
    }
}
