package com.example.proyectodepspot

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import java.io.File
import java.io.IOException
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class AudioManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var exoPlayer: ExoPlayer? = null
    private var isRecording = false
    private var startTime: Long = 0

    fun startRecording(onError: (String) -> Unit) {
        if (!hasPermission()) {
            onError("Se requiere permiso de micrófono")
            return
        }

        if (isRecording) {
            stopRecording()
        }

        try {
            audioFile = createAudioFile()
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            startTime = System.currentTimeMillis()
        } catch (e: IOException) {
            Log.e("AudioManager", "Error starting recording", e)
            onError("Error al iniciar la grabación: ${e.message}")
            releaseRecorder()
        } catch (e: Exception) {
            Log.e("AudioManager", "Error inesperado al iniciar la grabación", e)
            onError("Error inesperado: ${e.message}")
            releaseRecorder()
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) {
            return null
        }

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            isRecording = false
            return audioFile
        } catch (e: Exception) {
            Log.e("AudioManager", "Error stopping recording", e)
            releaseRecorder()
            return null
        }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.apply {
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioManager", "Error releasing recorder", e)
        } finally {
            mediaRecorder = null
            isRecording = false
        }
    }

    fun playAudio(audioFile: File, onCompletion: () -> Unit) {
        exoPlayer?.release()
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(audioFile.toURI().toString())
            setMediaItem(mediaItem)
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        onCompletion()
                    }
                }
            })
            prepare()
            play()
        }
    }

    fun stopPlayback() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }

    fun release() {
        stopPlayback()
        if (isRecording) {
            stopRecording()
        }
        releaseRecorder()
    }

    private fun createAudioFile(): File {
        val fileName = "audio_${System.currentTimeMillis()}.mp3"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: throw IOException("No se pudo acceder al directorio de almacenamiento")
        
        // Asegurarse de que el directorio existe
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                throw IOException("No se pudo crear el directorio de almacenamiento")
            }
        }
        
        val file = File(storageDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        
        return file
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
} 