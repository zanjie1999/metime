package com.cupinkie.metime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager

class StartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getAction().toString();
//        if (type.equals(Intent.ACTION_BOOT_COMPLETED)) {}
        // 检查下设置是否需要自启
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val startAtBooted = sp.getBoolean("startAtBooted", false)
        Log.v("metime", "startAtBooted："+ startAtBooted)
        if (startAtBooted) {
            Toast.makeText(context, "咩时间启动啦~", Toast.LENGTH_LONG).show()
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
