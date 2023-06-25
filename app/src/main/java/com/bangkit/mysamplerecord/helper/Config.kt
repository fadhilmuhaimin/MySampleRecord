package com.bangkit.mysamplerecord.helper

import android.content.Context
import android.media.MediaRecorder
import com.bangkit.mysamplerecord.R
import com.bangkit.mysamplerecord.extension.getDefaultRecordingFolder
import com.simplemobiletools.commons.helpers.BaseConfig

class Config(context : Context) : BaseConfig(context) {
    companion object{
        fun newInstance(context: Context) = Config(context)
    }

    var extension : Int
        get() = prefs.getInt(EXTENSION, EXTENSION_MP3)
        set(extension) = prefs.edit().putInt(EXTENSION,extension).apply()

    var hideNotification: Boolean
        get() = prefs.getBoolean(HIDE_NOTIFICATION, false)
        set(hideNotification) = prefs.edit().putBoolean(HIDE_NOTIFICATION, hideNotification).apply()

    var audioSource: Int
        get() = prefs.getInt(AUDIO_SOURCE, MediaRecorder.AudioSource.CAMCORDER)
        set(audioSource) = prefs.edit().putInt(AUDIO_SOURCE, audioSource).apply()

    var bitrate: Int
        get() = prefs.getInt(BITRATE, DEFAULT_BITRATE)
        set(bitrate) = prefs.edit().putInt(BITRATE, bitrate).apply()


    var saveRecordingsFolder : String
        get() = prefs.getString(SAVE_RECORDINGS,context.getDefaultRecordingFolder())!!
        set(saveRecordingFolder) = prefs.edit().putString(SAVE_RECORDINGS,saveRecordingFolder).apply()

    fun getExtension() = context.getString(
        when(extension){
            EXTENSION_MP3 -> R.string.mp3
            EXTENSION_M4A -> R.string.m4a

            else -> R.string.ogg
        }
    )
}