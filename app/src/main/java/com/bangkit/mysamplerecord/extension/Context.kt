package com.bangkit.mysamplerecord.extension

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Environment
import com.bangkit.mysamplerecord.R
import com.bangkit.mysamplerecord.helper.Config
import com.simplemobiletools.commons.extensions.internalStoragePath
import com.simplemobiletools.commons.helpers.isQPlus

//fun Context.updateWidgets(isRecording: Boolean) {
//    val widgetIDs = AppWidgetManager.getInstance(applicationContext)
//        ?.getAppWidgetIds(ComponentName(applicationContext, MyWidgetRecordDisplayProvider::class.java)) ?: return
//    if (widgetIDs.isNotEmpty()) {
//        Intent(applicationContext, MyWidgetRecordDisplayProvider::class.java).apply {
//            action = TOGGLE_WIDGET_UI
//            putExtra(IS_RECORDING, isRecording)
//            sendBroadcast(this)
//        }
//    }
//}

val Context.config: Config get() = Config.newInstance(applicationContext)

fun Context.getDefaultRecordingFolder() : String{
    val defaultPath = getDefaultRecordingsRelativePath()
    return "$internalStoragePath/$defaultPath"
}

fun Context.getDefaultRecordingsRelativePath() : String{
    return if(isQPlus()){
        "${Environment.DIRECTORY_MUSIC}/Recordings"
    }else{
        getString(R.string.app_name)
    }
}