// TAMBAHAN untuk Modul Praktikum P4a, Langkah 5: token dienkripsi AES-GCM dengan kunci di Android Keystore.
// Modul menyarankan "misalnya Tink + DataStore"; versi ini memakai Keystore langsung agar tanpa dependensi.
// Tambahkan juga android:allowBackup="false" (atau aturan exclude) di manifest.
// Revisi 2: jangan ganti dengan EncryptedSharedPreferences (androidx.security:security-crypto deprecated sejak 1.1.0).
package id.unsri.musiwatch.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object TokenStore {
    private const val ALIAS = "musiwatch_token_key"
    private const val PREFS = "auth"
    private const val KEY_TOKEN = "access_token"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    fun save(ctx: Context, token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val blob = cipher.iv + cipher.doFinal(token.toByteArray())          // IV 12 byte + ciphertext
        prefs(ctx).edit().putString(KEY_TOKEN, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
    }

    fun load(ctx: Context): String? {
        val b64 = prefs(ctx).getString(KEY_TOKEN, null) ?: return null
        val blob = Base64.decode(b64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob, 0, 12))
        return String(cipher.doFinal(blob, 12, blob.size - 12))
    }

    fun clear(ctx: Context) = prefs(ctx).edit().remove(KEY_TOKEN).apply()

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
