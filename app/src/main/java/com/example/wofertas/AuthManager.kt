package com.example.wofertas

import android.content.Context
import android.content.SharedPreferences
import com.example.wofertas.utils.AppLogger
import com.example.wofertas.utils.Constants

/**
 * Gerencia sessão do usuário usando SharedPreferences.
 * Migrado de IDs Long para String (MongoDB ObjectId).
 */
object AuthManager {

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

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
            prefs(context).edit()
                .putString(Constants.KEY_TOKEN,   token)
                .putString(Constants.KEY_USER_ID, userId)
                .putString(Constants.KEY_EMAIL,   email)
                .putString(Constants.KEY_TIPO,    tipo)
                .putString("user_name",           nome)
                .putString("user_image",          foto)
                .apply()
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
    fun getTipo(context: Context):   String? = prefs(context).getString(Constants.KEY_TIPO,    null)
    fun getNome(context: Context):   String? = prefs(context).getString("user_name",           "Usuário")
    fun getFoto(context: Context):   String? = prefs(context).getString("user_image",          null)

    // ── Estado ────────────────────────────────────────────────────────────────

    fun isLoggedIn(context: Context): Boolean = !getToken(context).isNullOrEmpty()
    fun isUsuario(context: Context):  Boolean = getTipo(context)?.equals(Constants.TIPO_USUARIO, true) == true
    fun isMercado(context: Context):  Boolean = getTipo(context)?.equals(Constants.TIPO_MERCADO, true) == true

    // ── Logout ────────────────────────────────────────────────────────────────

    fun clearSession(context: Context) {
        try {
            prefs(context).edit().clear().apply()
            AppLogger.debug("Session cleared")
        } catch (e: Exception) {
            AppLogger.error("Error clearing session", e)
        }
    }
}
