package com.example.wofertas

import android.app.Activity
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

object ChatbotLauncher {

    private const val ASSISTANT_FAB_TAG = "wofertas_assistant_fab"

    fun install(activity: Activity) {
        if (!AuthManager.isLoggedIn(activity)) return

        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (content.findViewWithTag<View>(ASSISTANT_FAB_TAG) != null) return

        val fab = FloatingActionButton(activity).apply {
            tag = ASSISTANT_FAB_TAG
            contentDescription = activity.getString(R.string.chatbot_open_content_description)
            setImageResource(android.R.drawable.ic_dialog_info)
            useCompatPadding = true
            setOnClickListener {
                activity.startActivity(
                    Intent(activity, ChatbotActivity::class.java)
                        .putExtra(ChatbotActivity.EXTRA_PAGE, activity.javaClass.simpleName)
                )
            }
        }

        val margin = activity.resources.getDimensionPixelSize(R.dimen.fab_margin)
        val bottomNavigationOffset = (56 * activity.resources.displayMetrics.density).toInt()
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(margin, margin, margin, margin + bottomNavigationOffset)
        }

        content.addView(fab, params)
    }
}
