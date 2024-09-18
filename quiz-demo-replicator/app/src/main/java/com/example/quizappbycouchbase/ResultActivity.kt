package com.example.quizappbycouchbase

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    private lateinit var databaseManager: DatabaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_result)
        initializeViews()
    }

    private fun initializeViews() {
        databaseManager = DatabaseManager(this)
        val tvName: TextView = findViewById(R.id.tv_name)
        val tvScore: TextView = findViewById(R.id.tv_score)
        val btnFinish: Button = findViewById(R.id.btn_finish)

        val username = intent.getStringExtra(Constants.USER_NAME).toString()
        val category = intent.getStringExtra(Constants.CATEGORY).toString()
        val totalQuestions = intent.getStringExtra(Constants.TOTAL_QUESTIONS).toString()
        val correctAnswers = intent.getStringExtra(Constants.CORRECT_ANSWERS).toString()

        tvName.text = username
        tvScore.text = "Your Score is $correctAnswers out of $totalQuestions"

        saveUserScore(username, correctAnswers.toInt(), category)

        btnFinish.setOnClickListener {
            startActivity(Intent(this, MainScreen::class.java))
        }
    }

    private fun saveUserScore(username: String, score: Int, category: String) {
        databaseManager.insertUserScore(Scores(username, score, category))
    }
}
