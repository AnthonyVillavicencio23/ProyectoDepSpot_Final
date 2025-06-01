package com.example.proyectodepspot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class ResultadoTestFragment : Fragment() {
    private var score: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            score = it.getInt(ARG_SCORE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_resultado_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val resultText = when {
            score <= 4 -> "Depresión mínima"
            score <= 9 -> "Depresión leve"
            score <= 14 -> "Depresión moderada"
            score <= 19 -> "Depresión moderadamente severa"
            else -> "Depresión severa"
        }

        view.findViewById<TextView>(R.id.resultText).text = "Tu puntuación es: $score\n$resultText"

        view.findViewById<Button>(R.id.btnFinalizar).setOnClickListener {
            activity?.finish()
        }
    }

    companion object {
        private const val ARG_SCORE = "score"

        fun newInstance(score: Int) = ResultadoTestFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_SCORE, score)
            }
        }
    }
} 