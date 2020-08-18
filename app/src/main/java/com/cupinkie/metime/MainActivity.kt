package com.cupinkie.metime

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.OrientationEventListener
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var orientationListener: OrientationEventListener

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
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_main)

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("landscapeScreen", true)) {
            // 屏幕旋转监听
            setRequestedOrientation(screenOrientation)
            orientationListener = object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
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
        }

        // 存储空间权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
        }

        try {
            // 读取设置 没设置就是这默认值
            val scale = this.resources.displayMetrics.scaledDensity;
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val tip = sp.getString("tip", "")
            val photoPath = sp.getString("photoPath", "")
            val marginTopTip = (sp.getString("marginTopTip", "15")!!.toInt() * scale + 0.5f).toInt()
            val marginBottomTime = (sp.getString("marginBottomTime", "15")!!.toInt() * scale + 0.5f).toInt()
            val marginBottomDate = (sp.getString("marginBottomDate", "15")!!.toInt() * scale + 0.5f).toInt()
            val marginLeftTime = (sp.getString("marginLeftTime", "40")!!.toInt() * scale + 0.5f).toInt()
            val marginRightDate = (sp.getString("marginRightDate", "40")!!.toInt() * scale + 0.5f).toInt()
            val timeFontSize = sp.getString("timeFontSize", "70")!!.toInt()
            val dateFontSize = sp.getString("dateFontSize", "30")!!.toInt()
            val tipFontSize = sp.getString("tipFontSize", "30")!!.toInt()
            val use24hTime = sp.getBoolean("use24hTime", false)
            val showSecond = sp.getBoolean("showSecond", true)
            val showYear = sp.getBoolean("showYear", true)
            val useWhiteText = sp.getBoolean("useWhiteText", false)
            val keepScreenOn = sp.getBoolean("keepScreenOn", false)

            if (keepScreenOn) {
                // 保持亮屏
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            // 准备界面
            timeText.setPadding(marginLeftTime, 0, 0, marginBottomTime)
            timeText.textSize = timeFontSize.toFloat()
            dateText.setPadding(0, 0, marginRightDate, marginBottomDate)
            dateText.textSize = dateFontSize.toFloat()
            tipText.setPadding(0, marginTopTip, 0, 0)
            tipText.textSize = tipFontSize.toFloat()
            if (useWhiteText) {
//                timeText.setTextColor(this.resources.getColor(R.color.white_overlay))
                timeText.setTextColor(ContextCompat.getColor(this, R.color.white_overlay_text))
                dateText.setTextColor(ContextCompat.getColor(this, R.color.white_overlay_text))
                tipText.setTextColor(ContextCompat.getColor(this, R.color.white_overlay_text))
            } else {
                timeText.setTextColor(ContextCompat.getColor(this, R.color.black_overlay_text))
                dateText.setTextColor(ContextCompat.getColor(this, R.color.black_overlay_text))
                tipText.setTextColor(ContextCompat.getColor(this, R.color.black_overlay_text))
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

            if (showYear) {
                sdfDate = SimpleDateFormat("yyyy年M月d日 E")
            } else {
                sdfDate = SimpleDateFormat("M月d日 E")
            }

            if (!photoPath.isNullOrEmpty()) {
                val bitmap = BitmapFactory.decodeFile(photoPath)
                bgImage.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "请检查设置：" + e.message, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(this, SettingsActivity::class.java)
                    .putExtra("lastBackKeyTime", System.currentTimeMillis())
                    .putExtra("screenOrientation", screenOrientation)
            )
            e.printStackTrace()
            return
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
