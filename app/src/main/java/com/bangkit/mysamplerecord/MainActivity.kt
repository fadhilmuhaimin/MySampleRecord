package com.bangkit.mysamplerecord

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.bangkit.mysamplerecord.databinding.ActivityMainBinding
import com.bangkit.mysamplerecord.helper.GET_RECORDER_INFO
import com.bangkit.mysamplerecord.helper.RECORDING_PAUSED
import com.bangkit.mysamplerecord.helper.RECORDING_RUNNING
import com.bangkit.mysamplerecord.helper.RECORDING_STOPPED
import com.bangkit.mysamplerecord.helper.TOGGLE_PAUSE
import com.bangkit.mysamplerecord.model.Events
import com.bangkit.mysamplerecord.services.RecorderService
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.PermissionRequiredDialog
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.getFormattedDuration
import com.simplemobiletools.commons.extensions.getPermissionString
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.hasPermission
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_POST_NOTIFICATIONS
import com.simplemobiletools.commons.helpers.PERMISSION_RECORD_AUDIO
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.isNougatPlus
import com.simplemobiletools.commons.helpers.isRPlus
import com.simplemobiletools.commons.helpers.isTiramisuPlus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var status = RECORDING_STOPPED
    private var pauseBlinkTimer = Timer()
    private var bus: EventBus? = null
    var actionOnPermission: ((granted: Boolean) -> Unit)? = null
    var isAskingPermissions = false
    private val GENERIC_PERM_HANDLER = 100
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handlePermission(PERMISSION_RECORD_AUDIO) {
            if (it) {
                tryInitVoiceRecorder()
            } else {
                this.toast(com.simplemobiletools.commons.R.string.no_audio_permissions)
                finish()
            }
        }


    }

    private fun tryInitVoiceRecorder() {
        if (isRPlus()) {
//            setupViewPager()
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) {
                if (it) {
//                    setupViewPager()
                } else {
                    finish()
                }
            }
        }
    }

    private fun setupColors() {
        val properPrimaryColor = getProperPrimaryColor()
        binding.toggleRecordingButton.apply {
            setImageDrawable(getToggleButtonIcon())
            background.applyColorFilter(properPrimaryColor)
        }

        binding.togglePauseButton.apply {
            setImageDrawable(resources.getColoredDrawableWithColor(R.drawable.ic_pause_vector, properPrimaryColor.getContrastColor()))
            background.applyColorFilter(properPrimaryColor)
        }

        binding.recorderVisualizer.chunkColor = properPrimaryColor
        binding.recordingDuration.setTextColor(getProperTextColor())
    }



    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
        pauseBlinkTimer.cancel()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupColors()
        binding.recorderVisualizer.recreate()
        bus = EventBus.getDefault()
        bus!!.register(this)

        updateRecordingDuration(0)
        binding.toggleRecordingButton.setOnClickListener {
            (this)?.handleNotificationPermission { granted ->
                if (granted) {
                    toggleRecording()
                } else {
                    PermissionRequiredDialog(this, com.simplemobiletools.commons.R.string.allow_notifications_voice_recorder)
                }
            }
        }

        binding.togglePauseButton.setOnClickListener {
            Intent(this, RecorderService::class.java).apply {
                action = TOGGLE_PAUSE
                startService(this)
            }
        }

        Intent(this, RecorderService::class.java).apply {
            action = GET_RECORDER_INFO
            try {
                startService(this)
            } catch (e: Exception) {
            }
        }

    }

    private fun toggleRecording() {
        status = if (status == RECORDING_RUNNING || status == RECORDING_PAUSED) {
            RECORDING_STOPPED
        } else {
            RECORDING_RUNNING
        }

        binding.toggleRecordingButton.setImageDrawable(getToggleButtonIcon())

        if (status == RECORDING_RUNNING) {
            startRecording()
        } else {
            binding.togglePauseButton.beGone()
            stopRecording()
        }
    }

    private fun startRecording() {
        Intent(this, RecorderService::class.java).apply {
            startService(this)
        }
        binding.recorderVisualizer.recreate()
    }

    private fun stopRecording() {
        Intent(this, RecorderService::class.java).apply {
            stopService(this)
        }
    }

    fun handlePermission(permissionId: Int, callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (hasPermission(permissionId)) {
            callback(true)
        } else {
            isAskingPermissions = true
            actionOnPermission = callback
            ActivityCompat.requestPermissions(this, arrayOf(getPermissionString(permissionId)), GENERIC_PERM_HANDLER)
        }
    }

    fun handleNotificationPermission(callback: (granted: Boolean) -> Unit) {
        if (!isTiramisuPlus()) {
            callback(true)
        } else {
            handlePermission(PERMISSION_POST_NOTIFICATIONS) { granted ->
                callback(granted)
            }
        }
    }

    private fun updateRecordingDuration(duration: Int) {
        binding.recordingDuration.text = duration.getFormattedDuration()
    }


    private fun refreshView() {
        binding.toggleRecordingButton.setImageDrawable(getToggleButtonIcon())
        binding.togglePauseButton.beVisibleIf(status != RECORDING_STOPPED && isNougatPlus())
        pauseBlinkTimer.cancel()

        if (status == RECORDING_PAUSED) {
            pauseBlinkTimer = Timer()
            pauseBlinkTimer.scheduleAtFixedRate(getPauseBlinkTask(), 500, 500)
        }

        if (status == RECORDING_RUNNING) {
            binding.togglePauseButton.alpha = 1f
        }
    }

    private fun getToggleButtonIcon(): Drawable {
        val drawable = if (status == RECORDING_RUNNING || status == RECORDING_PAUSED) R.drawable.ic_stop_vector else R.drawable.ic_microphone_vector
        return resources.getColoredDrawableWithColor(drawable, this.getProperPrimaryColor().getContrastColor())
    }

    private fun getPauseBlinkTask() = object : TimerTask() {
        override fun run() {
            if (status == RECORDING_PAUSED) {
                // update just the alpha so that it will always be clickable
                Handler(Looper.getMainLooper()).post {
                    binding.togglePauseButton.alpha =
                        if (binding.togglePauseButton.alpha == 0f) 1f else 0f
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (!RecorderService.isRunning) {
            status = RECORDING_STOPPED
        }

        refreshView()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun gotDurationEvent(event: Events.RecordingDuration) {
        updateRecordingDuration(event.duration)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun gotStatusEvent(event: Events.RecordingStatus) {
        status = event.status
        refreshView()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun gotAmplitudeEvent(event: Events.RecordingAmplitude) {
        val amplitude = event.amplitude
        if (status == RECORDING_RUNNING) {
            binding.recorderVisualizer.update(amplitude)
        }
    }
}