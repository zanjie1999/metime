package com.cupinkie.metime

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.OrientationEventListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


class SettingsActivity : AppCompatActivity() {
    lateinit var orientationListener: OrientationEventListener

    // 默认横屏
    var screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

    var lastBackKeyTime = 0L;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // man传过来的参数
        lastBackKeyTime = intent.getLongExtra("lastBackKeyTime", lastBackKeyTime)
        screenOrientation = intent.getIntExtra("screenOrientation", screenOrientation)

        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val e = sp.edit();
        // 文件选择器回调
        if (resultCode == Activity.RESULT_OK) {
            val uri = data!!.data
            var filePath: String?
            if ("file".equals(uri?.scheme, ignoreCase = false)) { //使用第三方应用打开
                filePath = uri?.path
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) { //4.4以后
                filePath = FileUtil.getPath(this, uri)
            } else { //4.4以下下系统调用方法
            filePath = FileUtil.getRealPathFromURI(this, uri)
            }
            Log.v("Sparkle", "文件选择：" + filePath + "")
            e.putString("photoPath", filePath)
            e.apply()
        } else {
            e.remove("photoPath")
            e.apply()
        }
        super.onActivityResult(requestCode, resultCode, data)
        // 此处不知道怎么findPreferences所以干脆直接让activity重新加载一次
        recreate()
    }

    override fun onBackPressed() {
        // 连击退出 否则返回
//        val l = System.currentTimeMillis() - lastBackKeyTime
//        Log.v("lastBackKeyTime", l.toString())
//        if (System.currentTimeMillis() - lastBackKeyTime < 5000) {
        System.exit(0)
//        } else {
//            super.onBackPressed()
//        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        orientationListener.disable();
        super.onDestroy();
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
//            findPreference<Preference>("")

            // 设置为只能输入数字
            for (key in listOf(
                "marginTopTip",
                "marginBottomTime",
                "marginLeftTime",
                "marginBottomDate",
                "marginRightDate",
                "timeFontSize",
                "dateFontSize",
                "tipFontSize"
            )) {
                val numberPreference = findPreference<EditTextPreference>(key)
                Log.v("onCreatePreferences", key + ": " + numberPreference)
                numberPreference?.setOnBindEditTextListener { editText ->
                    editText.inputType = InputType.TYPE_CLASS_NUMBER
                }
            }

            // 选择背景图片目录
            val photoPath = findPreference<Preference>("photoPath")
            photoPath?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(intent, 0)

                true
            }
            photoPath?.summary = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("photoPath", "未选择")


        }
    }
}