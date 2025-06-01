package com.example.proyectodepspot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class TestDepresionFragment : Fragment() {
    private var question: String? = null
    private var questionIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            question = it.getString(ARG_QUESTION)
            questionIndex = it.getInt(ARG_INDEX)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_test_depresion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.questionText).text = question

        val buttons = listOf(
            view.findViewById<Button>(R.id.btnNunca),
            view.findViewById<Button>(R.id.btnAlgunasVeces),
            view.findViewById<Button>(R.id.btnFrecuentemente),
            view.findViewById<Button>(R.id.btnSiempre)
        )

        val scores = listOf(0, 1, 2, 3)

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                (activity as? EvaluacionDepresionActivity)?.onAnswerSelected(scores[index])
            }
        }
    }

    companion object {
        private const val ARG_QUESTION = "question"
        private const val ARG_INDEX = "index"

        fun newInstance(question: String, index: Int) = TestDepresionFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_QUESTION, question)
                putInt(ARG_INDEX, index)
            }
        }
    }
} 