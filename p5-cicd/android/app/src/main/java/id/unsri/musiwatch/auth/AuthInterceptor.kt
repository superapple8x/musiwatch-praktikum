// TAMBAHAN untuk Modul Praktikum P4a, Langkah 5: sisipkan header Authorization; saat 401 arahkan ke login
// TANPA menghapus outbox. Jangan pernah mencetak token ke Logcat.
package id.unsri.musiwatch.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val onUnauthorized: () -> Unit,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = if (token != null)
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        else chain.request()
        val response = chain.proceed(request)
        if (response.code == 401) onUnauthorized()
        return response
    }
}
