package com.example.wofertas

import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

object ChatbotLauncher {

    private const val ASSISTANT_FAB_TAG = "wofertas_assistant_fab"

    fun install(activity: AppCompatActivity) {
        if (!AuthManager.isLoggedIn(activity)) return
        if (activity is ChatbotActivity) return

        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (content.findViewWithTag<View>(ASSISTANT_FAB_TAG) != null) return

        val fab = ExtendedFloatingActionButton(activity).apply {
            tag = ASSISTANT_FAB_TAG
            text = "Ajuda"
            contentDescription = activity.getString(R.string.chatbot_open_content_description)
            setIconResource(android.R.drawable.ic_menu_help)
            setTextColor(ContextCompat.getColor(activity, R.color.white))
            iconTint = ContextCompat.getColorStateList(activity, R.color.white)
            backgroundTintList = ContextCompat.getColorStateList(activity, R.color.wofertas_blue_primary)
            elevation = dp(activity, 8).toFloat()
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            setOnClickListener {
                activity.startActivity(
                    Intent(activity, ChatbotActivity::class.java)
                        .putExtra(ChatbotActivity.EXTRA_PAGE, activity.javaClass.simpleName)
                )
            }
        }

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            val margin = dp(activity, 16)
            setMargins(margin, margin, margin, dp(activity, 88))
        }

        content.addView(fab, params)
    }

    private fun dp(activity: AppCompatActivity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
