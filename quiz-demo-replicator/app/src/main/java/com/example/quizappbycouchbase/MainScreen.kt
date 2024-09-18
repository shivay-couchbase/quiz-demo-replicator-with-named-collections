package com.example.quizappbycouchbase

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainScreen : AppCompatActivity() {
    private lateinit var databaseManager: DatabaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_screen)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setupWindowInsetsListener()

        requestStoragePermission()

        val buttonStart = findViewById<Button>(R.id.button_start)
        val etName = findViewById<AppCompatEditText>(R.id.et_name)
        val buttonScore = findViewById<Button>(R.id.button_score)
        databaseManager = DatabaseManager(this)

        buttonScore.setOnClickListener {
            startActivity(Intent(this, ScoreActivity::class.java))
        }

        buttonStart.setOnClickListener {
            val userName = etName.text.toString().trim()
            if (userName.isEmpty()) {
                showToast("Please Enter Your Username")
            } else {
                val user = User(userName)
                databaseManager.insertUser(user)
                startUploadImageActivity(userName)
            }
        }
    }

    private fun setupWindowInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestPermissionLauncher.launch(permission)
    }

    private fun startUploadImageActivity(userName: String) {
        val intent = Intent(this, UploadImage::class.java).apply {
            putExtra(Constants.USER_NAME, userName)
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                showToast("Permission Granted")
            } else {
                showToast("Please grant permission")
            }
        }
}
