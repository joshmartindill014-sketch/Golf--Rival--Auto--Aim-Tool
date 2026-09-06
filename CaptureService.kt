package com.golfrival.autoaim

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.*
import android.media.*
import android.media.projection.MediaProjection
import android.os.*
import android.view.*
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.abs

class CaptureService: Service() {
    private lateinit var projection: MediaProjection
    private var imageReader: ImageReader?=null
    private var vd: VirtualDisplay?=null
    private lateinit var wm: WindowManager
    private lateinit var overlay: TextView
    private val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var busy=false
    private var lastWind="--"

    override fun onCreate(){
        super.onCreate()
        val channel=NotificationChannel("aim","Golf Rival Auto Aim",NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        startForeground(7,Notification.Builder(this,"aim").setContentTitle("Golf Rival Auto Aim").setContentText("Reading wind…").setSmallIcon(android.R.drawable.ic_menu_compass).build())
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        overlay=TextView(this).apply{
            text="🌬️ --\nAIM: --"
            textSize=18f; setTextColor(Color.WHITE); setPadding(18,10,18,10)
            setBackgroundColor(0xCC111111.toInt())
        }
        val lp=WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT)
        lp.gravity=Gravity.TOP or Gravity.END; lp.x=12; lp.y=120
        wm.addView(overlay,lp)
    }
    override fun onStartCommand(i:Intent?,f:Int,s:Int):Int{
        val rc=i?.getIntExtra("resultCode",-1) ?: -1
        val data=i?.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY
        val pm=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection=pm.getMediaProjection(rc,data)
        val dm=resources.displayMetrics
        imageReader=ImageReader.newInstance(dm.widthPixels,dm.heightPixels,PixelFormat.RGBA_8888,2)
        vd=projection.createVirtualDisplay("GolfRivalAim",dm.widthPixels,dm.heightPixels,dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,imageReader!!.surface,null,null)
        imageReader!!.setOnImageAvailableListener({ readFrame() },Handler(Looper.getMainLooper()))
        return START_STICKY
    }
    private fun readFrame(){
        if(busy)return
        val im=imageReader?.acquireLatestImage() ?: return
        busy=true
        val plane=im.planes[0]
        val bmp=Bitmap.createBitmap(im.width,im.height,Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(plane.buffer); im.close()
        // Your supplied screenshot is 691x1536. Wind is in roughly the top-right 22% x 12%.
        val l=(bmp.width*0.78f).toInt().coerceAtLeast(0)
        val t=(bmp.height*0.15f).toInt().coerceAtLeast(0)
        val r=bmp.width
        val b=(bmp.height*0.28f).toInt().coerceAtMost(bmp.height)
        val crop=Bitmap.createBitmap(bmp,l,t,(r-l).coerceAtLeast(1),(b-t).coerceAtLeast(1))
        recognizer.process(InputImage.fromBitmap(crop,0))
            .addOnSuccessListener { res ->
                val txt=res.text.replace(","," .").trim()
                val m=Regex("""(?<!\d)(\d{1,2}(?:\.\d)?)(?!\d)""").find(txt)
                if(m!=null){
                    val w=m.groupValues[1].toFloatOrNull()
                    if(w!=null && w in 0.0f..30.0f){
                        lastWind=m.groupValues[1]
                        update("🌬️ $lastWind\nAIM: ←")
                    }
                }
            }.addOnCompleteListener { crop.recycle(); bmp.recycle(); busy=false }
    }
    private fun update(s:String){ overlay.post{overlay.text=s} }
    override fun onDestroy(){
        try{wm.removeView(overlay)}catch(_:Exception){}
        vd?.release(); imageReader?.close(); projection.stop(); recognizer.close()
        super.onDestroy()
    }
    override fun onBind(i:Intent?)=null
}
