package com.example.cinet.data.remote.canvas

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the Canvas Personal Access Token using EncryptedSharedPreferences.
 *
 * The token is sensitive — anyone with it can act as the user against Canvas
 * (read grades, submit assignments, etc.) — so it should never be written to
 * normal SharedPreferences or Firestore. The Android Keystore-backed
 * encryption here keeps the token at-rest-encrypted on the device.
 *
 * One token is stored per device install (not per Firebase user); if the user
 * switches accounts, they re-enter or clear the token.
 */
class CanvasTokenStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Returns the saved token, or null if the user has never connected Canvas. */
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    /** Persists a token. Trims whitespace because users often paste with stray spaces. */
    fun saveToken(rawToken: String) {
        prefs.edit().putString(KEY_TOKEN, rawToken.trim()).apply()
    }

    /** Wipes the saved token immediately so sign-out cannot reuse a stale Canvas session. */
    fun clear() {
        prefs.edit().remove(KEY_TOKEN).commit()
    }

    /** True when a token is currently saved — used to drive the "Connected" UI state. */
    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    companion object {
        private const val PREFS_FILE = "canvas_secure_prefs"
        private const val KEY_TOKEN = "canvas_access_token"
    }
}
