package com.example.proyectodepspot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectodepspot.data.NotificationSettingsRepository
import com.example.proyectodepspot.utils.NotificationHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class NotificacionesActivity : AppCompatActivity() {
    private lateinit var soundSwitch: SwitchMaterial
    private lateinit var vibrationSwitch: SwitchMaterial
    private lateinit var pushNotificationSwitch: SwitchMaterial
    private lateinit var testButton: MaterialButton
    private lateinit var ringtoneGroup: RadioGroup
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var settingsRepository: NotificationSettingsRepository

    companion object {
        const val RINGTONE_CLASSIC = 1
        const val RINGTONE_SOFT = 2
        const val RINGTONE_MODERN = 3
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        // Inicializar NotificationHelper y repositorio
        notificationHelper = NotificationHelper(this)
        settingsRepository = NotificationSettingsRepository()

        // Configurar la barra superior
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Inicializar los switches y el grupo de radio
        soundSwitch = findViewById(R.id.soundSwitch)
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
        pushNotificationSwitch = findViewById(R.id.pushNotificationSwitch)
        testButton = findViewById(R.id.testButton)
        ringtoneGroup = findViewById(R.id.ringtoneGroup)
        
        // Cargar configuraciones
        loadSettings()

        // Configurar los listeners de los switches
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                try {
                    settingsRepository.updateSoundEnabled(isChecked)
                } catch (e: Exception) {
                    Toast.makeText(this@NotificacionesActivity, "Error al actualizar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
                    soundSwitch.isChecked = !isChecked
                }
            }
        }

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                try {
                    settingsRepository.updateVibrationEnabled(isChecked)
                } catch (e: Exception) {
                    Toast.makeText(this@NotificacionesActivity, "Error al actualizar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
                    vibrationSwitch.isChecked = !isChecked
                }
            }
        }

        pushNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
            lifecycleScope.launch {
                try {
                    settingsRepository.updatePushNotificationsEnabled(isChecked)
                } catch (e: Exception) {
                    Toast.makeText(this@NotificacionesActivity, "Error al actualizar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
                    pushNotificationSwitch.isChecked = !isChecked
                }
            }
        }

        // Configurar el listener del grupo de radio
        ringtoneGroup.setOnCheckedChangeListener { _, checkedId ->
            val ringtoneId = when (checkedId) {
                R.id.ringtone1 -> RINGTONE_CLASSIC
                R.id.ringtone2 -> RINGTONE_SOFT
                R.id.ringtone3 -> RINGTONE_MODERN
                else -> RINGTONE_CLASSIC
            }
            lifecycleScope.launch {
                try {
                    settingsRepository.updateSelectedRingtone(ringtoneId)
                } catch (e: Exception) {
                    Toast.makeText(this@NotificacionesActivity, "Error al actualizar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Configurar el botón de prueba
        testButton.setOnClickListener {
            lifecycleScope.launch {
                val settings = settingsRepository.getSettings()
                if (settings.soundEnabled) {
                    playTestSound(settings.selectedRingtone)
                }
                if (settings.vibrationEnabled) {
                    vibrate()
                }
                if (settings.pushNotificationsEnabled) {
                    showTestNotification()
                }
            }
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            try {
                val settings = settingsRepository.getSettings()
                soundSwitch.isChecked = settings.soundEnabled
                vibrationSwitch.isChecked = settings.vibrationEnabled
                pushNotificationSwitch.isChecked = settings.pushNotificationsEnabled
                
                when (settings.selectedRingtone) {
                    RINGTONE_CLASSIC -> findViewById<RadioButton>(R.id.ringtone1).isChecked = true
                    RINGTONE_SOFT -> findViewById<RadioButton>(R.id.ringtone2).isChecked = true
                    RINGTONE_MODERN -> findViewById<RadioButton>(R.id.ringtone3).isChecked = true
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificacionesActivity, "Error al cargar configuraciones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTestNotification() {
        notificationHelper.showMessageNotification("Este es un mensaje de prueba de Deepy")
    }

    private fun playTestSound(selectedRingtone: Int) {
        // Liberar el MediaPlayer anterior si existe
        mediaPlayer?.release()
        
        // Seleccionar el recurso de sonido según el tono elegido
        val soundResource = when (selectedRingtone) {
            RINGTONE_CLASSIC -> R.raw.message_sound
            RINGTONE_SOFT -> R.raw.message_sound_soft
            RINGTONE_MODERN -> R.raw.message_sound_modern
            else -> R.raw.message_sound
        }
        
        // Crear y reproducir el sonido
        mediaPlayer = MediaPlayer.create(this, soundResource)
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
        }
        mediaPlayer?.start()
    }

    private fun vibrate() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
        
        Toast.makeText(this, "Vibración activada", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Si el usuario rechaza el permiso, desactivar el switch
                pushNotificationSwitch.isChecked = false
                lifecycleScope.launch {
                    try {
                        settingsRepository.updatePushNotificationsEnabled(false)
                    } catch (e: Exception) {
                        Toast.makeText(this@NotificacionesActivity, "Error al actualizar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                Toast.makeText(this, "Se requiere permiso para mostrar notificaciones", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 