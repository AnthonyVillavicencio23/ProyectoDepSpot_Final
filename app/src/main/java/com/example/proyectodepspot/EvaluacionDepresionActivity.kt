package com.example.proyectodepspot

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class EvaluacionDepresionActivity : AppCompatActivity() {
    private var currentScore = 0
    private var currentQuestionIndex = 0
    private val questions = listOf(
        "¿Te has sentido triste o deprimido la mayor parte del día?",
        "¿Has perdido interés o placer en las actividades que antes disfrutabas?",
        "¿Has tenido problemas para dormir o has dormido demasiado?",
        "¿Te has sentido cansado o con poca energía?",
        "¿Has tenido cambios en tu apetito o peso?",
        "¿Te has sentido inútil o culpable?",
        "¿Has tenido dificultad para concentrarte o tomar decisiones?",
        "¿Has tenido pensamientos de muerte o suicidio?"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evaluacion_depresion)
        
        // Configurar el manejo del botón de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        })

        // Iniciar con la primera pregunta
        showQuestion(currentQuestionIndex)
    }

    private fun showQuestion(index: Int) {
        if (index < questions.size) {
            val fragment = TestDepresionFragment.newInstance(questions[index], index)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        } else {
            // Mostrar resultados
            showResults()
        }
    }

    fun onAnswerSelected(score: Int) {
        currentScore += score
        currentQuestionIndex++
        showQuestion(currentQuestionIndex)
    }

    private fun showResults() {
        val resultFragment = ResultadoTestFragment.newInstance(currentScore)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, resultFragment)
            .commit()
    }
} 