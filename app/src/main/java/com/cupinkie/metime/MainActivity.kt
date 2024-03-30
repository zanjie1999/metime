package com.cupinkie.metime

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.OrientationEventListener
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var orientationListener: OrientationEventListener

    // 默认横屏
    var screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

    private var timeHandler: Handler = Handler()
    private var runnable: Runnable? = null
    var sdfDate = SimpleDateFormat("yyyy年M月d日 E")
    var sdfTime = SimpleDateFormat("h:mm")
    var sdfTimeSecond = SimpleDateFormat(":ss")
    var smallSecond = false
    var clearMsgSecond = 60

    // socket相关
    var clearTime = -1L
    var serverSocket: ServerSocket? = null
    var socket: Socket? = null



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
            val marginTopMsg = (sp.getString("marginTopMsg", "55")!!.toInt() * scale + 0.5f).toInt()
            val marginBottomTime = (sp.getString("marginBottomTime", "10")!!.toInt() * scale + 0.5f).toInt()
            val marginBottomDate = (sp.getString("marginBottomDate", "15")!!.toInt() * scale + 0.5f).toInt()
            val marginLeftTime = (sp.getString("marginLeftTime", "40")!!.toInt() * scale + 0.5f).toInt()
            val marginRightDate = (sp.getString("marginRightDate", "40")!!.toInt() * scale + 0.5f).toInt()
            val timeFontSize = sp.getString("timeFontSize", "70")!!.toInt()
            val dateFontSize = sp.getString("dateFontSize", "30")!!.toInt()
            val tipFontSize = sp.getString("tipFontSize", "30")!!.toInt()
            val msgFontSize = sp.getString("msgFontSize", "40")!!.toInt()
            val use24hTime = sp.getBoolean("use24hTime", false)
            val showSecond = sp.getString("showSecond", "2")
            val showYear = sp.getBoolean("showYear", false)
            val useWhiteText = sp.getBoolean("useWhiteText", false)
            val keepScreenOn = sp.getBoolean("keepScreenOn", false)
            val startMsgSocket = sp.getBoolean("startMsgSocket", false)
            clearMsgSecond = sp.getString("clearMsgSecond", "60")!!.toInt()

            if (keepScreenOn) {
                // 保持亮屏
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            // 准备界面  字体自带边缘所以小秒底部x2
            val timeSecondText = findViewById<TextView>(R.id.timeSecondText)
            val timeText = findViewById<TextView>(R.id.timeText)
            val dateText = findViewById<TextView>(R.id.dateText)
            val tipText = findViewById<TextView>(R.id.tipText)
            val msgText = findViewById<TextView>(R.id.msgText)
            val bgImage = findViewById<ImageView>(R.id.bgImage)

            timeSecondText.setPadding(0, 0, 0, (timeFontSize * 0.6 + marginBottomTime).toInt())
            timeSecondText.textSize = timeFontSize.toFloat() / 2
            timeText.setPadding(marginLeftTime, 0, 0, marginBottomTime)
            timeText.textSize = timeFontSize.toFloat()
            dateText.setPadding(0, 0, marginRightDate, marginBottomDate)
            dateText.textSize = dateFontSize.toFloat()
            tipText.setPadding(0, marginTopTip, 0, 0)
            tipText.textSize = tipFontSize.toFloat()
            msgText.setPadding(0, marginTopMsg, 0, 0)
            msgText.textSize = msgFontSize.toFloat()
            if (useWhiteText) {
                timeSecondText.setTextColor(ContextCompat.getColor(this, R.color.white_overlay_text))
                timeText.setTextColor(ContextCompat.getColor(this, R.color.white_overlay_text))
                dateText.setTextColor(ContextCompat.getColor(this, R.color.white_overlay_text))
                tipText.setTextColor(ContextCompat.getColor(this, R.color.white_overlay_text))
                msgText.setTextColor(ContextCompat.getColor(this, R.color.white_overlay_text))
            } else {
                timeSecondText.setTextColor(ContextCompat.getColor(this, R.color.black_overlay_text))
                timeText.setTextColor(ContextCompat.getColor(this, R.color.black_overlay_text))
                dateText.setTextColor(ContextCompat.getColor(this, R.color.black_overlay_text))
                tipText.setTextColor(ContextCompat.getColor(this, R.color.black_overlay_text))
                msgText.setTextColor(ContextCompat.getColor(this, R.color.black_overlay_text))
            }

            if (!tip.isNullOrBlank()) {
                tipText.text = tip
            }

            if (use24hTime) {
                if (showSecond == "1") {
                    sdfTime = SimpleDateFormat("H:mm:ss")
                } else {
                    sdfTime = SimpleDateFormat("H:mm")
                }
            } else {
                if (showSecond == "1") {
                    sdfTime = SimpleDateFormat("h:mm:ss")
                } else {
                    sdfTime = SimpleDateFormat("h:mm")
                }
            }

            // 换成bool大概能提升性能吧XD
            smallSecond = showSecond == "2"

            if (showYear) {
                sdfDate = SimpleDateFormat("yyyy年M月d日 E")
            } else {
                sdfDate = SimpleDateFormat("M月d日 E")
            }

            if (!photoPath.isNullOrEmpty()) {
                val bitmap = BitmapFactory.decodeFile(photoPath)
                bgImage.setImageBitmap(bitmap)
            }

            // socket服务
            if (startMsgSocket) {
                 Thread(Runnable {
                    try {
                        serverSocket = ServerSocket(50803)
                        Log.v("Socket", "监听启动成功")
                        while (serverSocket != null) {
                            // accept阻塞 Socket close了会直接抛异常出来
                            val newSocket = serverSocket!!.accept()
                            // 连新的就关闭旧的 节省cpu资源
                            socket?.close()
                            socket = newSocket
                            // 起新线程以便接下一个连接 连多了cpu会炸
                            Thread(Runnable {
                                try {
                                    if (socket != null) {
                                        val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                                        val writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                                        updateMsgText("已连接：" + socket!!.inetAddress.hostAddress)
                                        while (socket!!.isConnected) {
                                            // readLine阻塞 Socket close了会直接抛异常出来
                                            val line = reader.readLine()
                                            if (line != null) {
                                                updateMsgText(line)
                                                writer.write("ok\n")
                                                writer.flush()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }).start()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }).start()


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
                val timeText = findViewById<TextView>(R.id.timeText)
                val dateText = findViewById<TextView>(R.id.dateText)
                val timeSecondText = findViewById<TextView>(R.id.timeSecondText)
                val msgText = findViewById<TextView>(R.id.msgText)

                val d = Date()
                timeText.text = sdfTime.format(d)
                dateText.text = sdfDate.format(d)
                if (smallSecond) {
                    timeSecondText.text = sdfTimeSecond.format(d)
                }

                // 顺便处理下 清空msg
                if(clearTime != -1L && clearTime < d.time) {
                    msgText.text = ""
                    clearTime = -1L
                }

                timeHandler.postDelayed(this, 500)
            }
        }
        timeHandler.postDelayed(runnable as Runnable, 1000)

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
        serverSocket?.close()
        orientationListener.disable()
        if (runnable != null) {
            timeHandler.removeCallbacks(runnable!!)
        }
        super.onDestroy();
    }

    // msg Text Handler
    private val msgHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg != null) {
                super.handleMessage(msg)
            }
            val text = msg?.obj.toString()
            val msgText = findViewById<TextView>(R.id.msgText)
            msgText.text = text
            Log.v("msg", text)
            // 定时清空
            clearTime = if (clearMsgSecond == -1) -1L else clearMsgSecond * 1000 + System.currentTimeMillis()
        }
    }

    fun updateMsgText(text: String) {
        val msg = msgHandler.obtainMessage();
        msg.obj = text
        msgHandler.sendMessage(msg)
    }


}
