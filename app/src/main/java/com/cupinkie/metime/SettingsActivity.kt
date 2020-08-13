package com.cupinkie.metime

import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat


class SettingsActivity : AppCompatActivity() {
    lateinit var orientationListener:OrientationEventListener

    // 默认横屏
    var screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

    var lastBackKeyTime = 0L;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // man传过来的参数
        lastBackKeyTime = intent.getLongExtra("lastBackKeyTime",lastBackKeyTime)
        screenOrientation = intent.getIntExtra("screenOrientation", screenOrientation)

        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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


//        // 读取设置 没设置就是这默认值
//        val scale = getResources().getDisplayMetrics().density;
//        var sp = getSharedPreferences("Sparkle", 0)
//        val tip = sp.getString("tip", "")
//        val photoPath = sp.getString("photoPath", "")
//        val marginTop = sp.getInt("marginTop", 15)
//        val marginBottom = sp.getInt("marginBottom", 15)
//        val marginLeft = sp.getInt("marginLeft", 40)
//        val marginRight = sp.getInt("marginRight", 40)
//        val timeFontSize = sp.getInt("timeFontSize", 70)
//        val dateFontSize = sp.getInt("dateFontSize", 30)
//        val tipFontSize = sp.getInt("tipFontSize", 30)
//        val use24hTime = sp.getBoolean("use24hTime", false)
//        val showSecond = sp.getBoolean("showSecond", true)



    }

    override fun onBackPressed() {
        // 连击退出 否则返回
        val l = System.currentTimeMillis() - lastBackKeyTime
        Log.v("lastBackKeyTime", l.toString())
        if (System.currentTimeMillis() - lastBackKeyTime < 5000) {
            System.exit(0)
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy();
        orientationListener.disable();
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            //        // 读取设置 没设置就是这默认值
//        val scale = getResources().getDisplayMetrics().density;
//        var sp = getSharedPreferences("Sparkle", 0)
//        val tip = sp.getString("tip", "")
//        val photoPath = sp.getString("photoPath", "")
//        val marginTop = sp.getInt("marginTop", 15)
//        val marginBottom = sp.getInt("marginBottom", 15)
//        val marginLeft = sp.getInt("marginLeft", 40)
//        val marginRight = sp.getInt("marginRight", 40)
//        val timeFontSize = sp.getInt("timeFontSize", 70)
//        val dateFontSize = sp.getInt("dateFontSize", 30)
//        val tipFontSize = sp.getInt("tipFontSize", 30)
//        val use24hTime = sp.getBoolean("use24hTime", false)
//        val showSecond = sp.getBoolean("showSecond", true)

        }
    }
}