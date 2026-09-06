package com.golfrival.autoaim

import android.app.*
import android.content.*
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val req = 44
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28,28,28,28)
        }
        val title = TextView(this).apply {
            text = "🎯 Golf Rival Auto Aim"
            textSize = 26f
            setTextColor(Color.WHITE)
        }
        val info = TextView(this).apply {
            text = "\nThis prototype reads the wind from the Golf Rival screen and places a small answer over the game.\n\n1. Allow “Display over other apps”.\n2. Tap START.\n3. Accept the Android screen-capture prompt.\n4. Open Golf Rival.\n\nThe first version uses the screen layout in your supplied screenshot as its detection starting point."
            textSize = 16f
            setTextColor(Color.LTGRAY)
        }
        val start = Button(this).apply { text = "START AUTO AIM" }
        box.addView(title); box.addView(info); box.addView(start)
        setContentView(box)
        window.decorView.setBackgroundColor(Color.rgb(18,18,18))

        start.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                Toast.makeText(this,"Allow the permission, then press START again.",Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mgr.createScreenCaptureIntent(), req)
        }
    }
    override fun onActivityResult(r:Int,c:Int,d:Intent?) {
        super.onActivityResult(r,c,d)
        if (r==req && c==RESULT_OK && d!=null) {
            val i=Intent(this,CaptureService::class.java).apply {
                putExtra("resultCode",c); putExtra("data",d)
            }
            if (Build.VERSION.SDK_INT>=26) startForegroundService(i) else startService(i)
            Toast.makeText(this,"Auto Aim running — open Golf Rival.",Toast.LENGTH_LONG).show()
        }
    }
}
