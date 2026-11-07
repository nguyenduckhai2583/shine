package com.example.shine.ui.shortcut

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.shine.MainActivity

class ShortcutTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetIntent = Intent(this, MainActivity::class.java).apply {
            action = intent?.action ?: Intent.ACTION_VIEW
            intent?.extras?.let { putExtras(it) }
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        startActivity(targetIntent)
        finish()
    }
}
