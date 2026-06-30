package com.example.wofertas

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.wofertas.network.ApiClient
import com.example.wofertas.utils.AppLogger
import com.example.wofertas.utils.Constants
import java.util.Locale

/**
 * Gerencia sessão do usuário usando SharedPreferences.
 * Migrado de IDs Long para String (MongoDB ObjectId).
 */
object AuthManager {

    private fun prefs(ctx: Context): SharedPreferences {
        val appContext = ctx.applicationContext
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                Constants.PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            AppLogger.error("Encrypted preferences unavailable; using private fallback", e)
            appContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // ── Salvar sessão após login ──────────────────────────────────────────────

    fun saveSession(
        context: Context,
        token:   String,
        userId:  String,
        email:   String,
        tipo:    String,
        nome:    String? = null,    // Adicionado para cache local
        foto:    String? = null     // Adicionado para cache local
    ) {
        try {
            val tipoNormalizado = normalizeTipo(tipo) ?: tipo.trim().uppercase(Locale.ROOT)
            prefs(context).edit()
                .putString(Constants.KEY_TOKEN,   token)
                .putString(Constants.KEY_USER_ID, userId)
                .putString(Constants.KEY_EMAIL,   email)
                .putString(Constants.KEY_TIPO,    tipoNormalizado)
                .putString("user_name",           nome)
                .putString("user_image",          foto)
                .apply()
            ApiClient.invalidateAuthService()
            AppLogger.debug("Session saved for user: $userId")
        } catch (e: Exception) {
            AppLogger.error("Error saving session", e)
        }
    }

    // ── Novo: Atualizar Perfil Localmente ─────────────────────────────────────
    // Resolve o erro "Unresolved reference: updateLocalProfile"

    fun updateLocalProfile(context: Context, novoNome: String, novaFoto: String? = null) {
        try {
            val editor = prefs(context).edit()
            editor.putString("user_name", novoNome)
            if (novaFoto != null) {
                editor.putString("user_image", novaFoto)
            }
            editor.apply()
            AppLogger.debug("Profile updated locally: $novoNome")
        } catch (e: Exception) {
            AppLogger.error("Error updating local profile", e)
        }
    }

    // ── Leitura ───────────────────────────────────────────────────────────────

    fun getToken(context: Context):  String? = prefs(context).getString(Constants.KEY_TOKEN,   null)
    fun getUserId(context: Context): String? = prefs(context).getString(Constants.KEY_USER_ID, null)
    fun getEmail(context: Context):  String? = prefs(context).getString(Constants.KEY_EMAIL,   null)
    fun getTipo(context: Context):   String? = normalizeTipo(prefs(context).getString(Constants.KEY_TIPO, null))
    fun getNome(context: Context):   String? = prefs(context).getString("user_name",           "Usuário")
    fun getFoto(context: Context):   String? = prefs(context).getString("user_image",          null)

    // ── Estado ────────────────────────────────────────────────────────────────

    fun isLoggedIn(context: Context): Boolean =
        !getToken(context).isNullOrEmpty() && getTipo(context) != null
    fun isUsuario(context: Context):  Boolean = getTipo(context)?.equals(Constants.TIPO_USUARIO, true) == true
    fun isMercado(context: Context):  Boolean = getTipo(context)?.equals(Constants.TIPO_MERCADO, true) == true

    // ── Logout ────────────────────────────────────────────────────────────────

    fun clearSession(context: Context) {
        val appContext = context.applicationContext
        try {
            val cleared = prefs(appContext).edit().clear().commit()
            AppLogger.debug("Encrypted session cleared: $cleared")
        } catch (e: Exception) {
            AppLogger.error("Error clearing encrypted session", e)
        }

        try {
            val fallbackCleared = appContext
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
            AppLogger.debug("Fallback session cleared: $fallbackCleared")
        } catch (e: Exception) {
            AppLogger.error("Error clearing fallback session", e)
        }

        ApiClient.invalidateAuthService()
    }

    private fun normalizeTipo(tipo: String?): String? {
        val value = tipo?.trim()?.uppercase(Locale.ROOT) ?: return null
        return when (value) {
            Constants.TIPO_USUARIO, "CLIENTE", "USER" -> Constants.TIPO_USUARIO
            Constants.TIPO_MERCADO, "SUPERMERCADO", "MARKET" -> Constants.TIPO_MERCADO
            else -> null
        }
    }
}
