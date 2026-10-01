package com.atlas.agent

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var goal: EditText
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Atlas"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
            setBackgroundColor(Color.rgb(7,16,24))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        fun text(s:String, size:Float, color:Int=Color.WHITE) = TextView(this).apply {
            text=s; textSize=size; setTextColor(color)
        }
        fun button(label:String, action:()->Unit)=Button(this).apply {
            text=label
            setOnClickListener { action() }
        }

        root.addView(text("ATLAS", 30f).apply { gravity=Gravity.CENTER })
        root.addView(text("Personal AI Operating Agent", 16f, Color.rgb(159,183,196)).apply { gravity=Gravity.CENTER })

        status = text("Accessibility: checking…", 16f, Color.rgb(85,230,255)).also {
            it.setPadding(0,30,0,12); root.addView(it)
        }

        root.addView(button("Enable Atlas Control") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        goal = EditText(this).apply {
            hint="مثلاً: open settings / home / back / read screen"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.rgb(13,25,34))
            setPadding(24,24,24,24)
        }
        root.addView(goal, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin=24
        })

        root.addView(button("Run") { runGoal(goal.text.toString()) },
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        root.addView(button("EMERGENCY STOP") {
            goal.setText("")
            output.text="Stopped. Atlas will not perform another action until you press Run."
        }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        output = text("Ready.", 14f, Color.LTGRAY).apply {
            setPadding(0,24,0,0)
            setTextIsSelectable(true)
        }
        root.addView(output)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        val enabled = AtlasAccessibilityService.instance != null
        status.text = if (enabled) "Accessibility: CONNECTED" else "Accessibility: NOT ENABLED"
        status.setTextColor(if (enabled) Color.rgb(99,230,190) else Color.rgb(255,107,107))
    }

    private fun runGoal(raw:String) {
        val service = AtlasAccessibilityService.instance
        if (service == null) {
            output.text = "First enable Atlas under Android Accessibility settings."
            return
        }
        val cmd = raw.trim().lowercase()
        val ok = when {
            cmd.contains("open settings") || cmd.contains("تنظیمات") -> service.openSettings()
            cmd == "home" || cmd.contains("خانه") -> service.home()
            cmd == "back" || cmd.contains("برگرد") -> service.back()
            cmd.contains("read screen") || cmd.contains("صفحه") -> {
                output.text = service.visibleText().ifBlank { "No readable text found." }
                return
            }
            cmd.startsWith("tap ") -> service.tapText(raw.substringAfter(" "))
            cmd.startsWith("type ") -> service.typeIntoFocused(raw.substringAfter(" "))
            else -> {
                output.text = "Local MVP command not recognized. AI backend routing will be connected next."
                return
            }
        }
        output.text = if (ok) "Action executed." else "Action could not be executed on the current screen."
    }
}
