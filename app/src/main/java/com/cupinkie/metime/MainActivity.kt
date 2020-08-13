package com.cupinkie.metime

import android.annotation.SuppressLint
import android.app.ActionBar
import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var orientationListener:OrientationEventListener

    // 默认横屏
    var screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

    private var handler: Handler = Handler()
    private var runnable: Runnable? = null
    var sdfDate = SimpleDateFormat("yyyy年M月d日 E")
    var sdfTime = SimpleDateFormat("h:mm:ss")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //去除标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        //去除状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_main)

        // 屏幕旋转监听
        setRequestedOrientation(screenOrientation)
        orientationListener = object : OrientationEventListener(this,SensorManager.SENSOR_DELAY_UI) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation > 80 && orientation < 100) { //90度 横向翻转
                    if (screenOrientation != 90) {
                        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        setRequestedOrientation(screenOrientation)
                    }
                } else if (orientation > 260 && orientation < 280) { //270度 横向
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    setRequestedOrientation(screenOrientation)
                } else {
                    return;
                }
            }
        }
        if (orientationListener.canDetectOrientation()) {
            Log.v("Sparkle", "屏幕旋转 初始化成功");
            orientationListener.enable();
        } else {
            Log.v("Sparkle", "屏幕旋转 初始化失败");
            orientationListener.disable();
        }

        // 读取设置 没设置就是这默认值
        val scale = getResources().getDisplayMetrics().density;
//        var sp = getSharedPreferences("Sparkle", 0)
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val tip = sp.getString("tip", "")
        val photoPath = sp.getString("photoPath", "")
        val marginTop = (sp.getString("marginTop", "15")!!.toInt() * scale + 0.5).toInt()
        val marginBottom = (sp.getString("marginBottom", "15")!!.toInt() * scale + 0.5).toInt()
        val marginLeft = (sp.getString("marginLeft", "40")!!.toInt() * scale + 0.5).toInt()
        val marginRight = (sp.getString("marginRight", "40")!!.toInt() * scale + 0.5).toInt()
        val timeFontSize = sp.getString("timeFontSize", "70")!!.toInt()
        val dateFontSize = sp.getString("dateFontSize", "30")!!.toInt()
        val tipFontSize = sp.getString("tipFontSize", "30")!!.toInt()
        val use24hTime = sp.getBoolean("use24hTime", false)
        val showSecond = sp.getBoolean("showSecond", true)

        Log.v("margin", marginLeft.toString() + " " + marginTop + " " + marginRight + " " + marginBottom)

        // 准备界面
        timeText.setPadding(marginLeft,0,0,marginBottom)
        timeText.textSize = timeFontSize.toFloat()
        dateText.setPadding(0,0,marginRight,marginBottom)
        dateText.textSize = dateFontSize.toFloat()
        tipText.setPadding(0,marginTop,0,0)
        tipText.textSize = tipFontSize.toFloat()

        if (!photoPath.isNullOrEmpty()) {
            val f = File(photoPath)
            if (f.isFile) {
                bgImage.setImageURI(Uri.fromFile(f))
            }
        }

        if (!tip.isNullOrBlank()) {
            tipText.text = tip
        }

        if (use24hTime) {
            if (showSecond) {
                sdfTime = SimpleDateFormat("H:mm:ss")
            } else {
                sdfTime = SimpleDateFormat("H:mm")
            }
        } else {
            if (showSecond) {
                sdfTime = SimpleDateFormat("h:mm:ss")
            } else {
                sdfTime = SimpleDateFormat("h:mm")
            }
        }



        // 时间显示线程
        runnable = object : Runnable {
            override fun run() {
                val d = Date()
                timeText.text = sdfTime.format(d)
                dateText.text = sdfDate.format(d)

                handler.postDelayed(this, 500)
            }
        }
        handler.postDelayed(runnable, 1000)

    }

    override fun onBackPressed() {
        // 返回进入设置
        startActivity(
            Intent(this, SettingsActivity::class.java)
                .putExtra("lastBackKeyTime", System.currentTimeMillis())
                .putExtra("screenOrientation", screenOrientation)
        )
    }

    override fun onDestroy() {
        super.onDestroy();
        orientationListener.disable()
        if (runnable != null) {
            handler.removeCallbacks(runnable)
        }
    }






}
