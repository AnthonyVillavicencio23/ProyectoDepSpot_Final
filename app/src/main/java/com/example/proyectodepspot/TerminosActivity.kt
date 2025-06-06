package com.example.proyectodepspot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class TerminosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminos)

        findViewById<MaterialButton>(R.id.btnRegresar).setOnClickListener {
            onBackPressed()
        }
    }
} 