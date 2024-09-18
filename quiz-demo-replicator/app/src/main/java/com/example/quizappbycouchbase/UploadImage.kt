package com.example.quizappbycouchbase

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File


class UploadImage : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private lateinit var imageView: ImageView
    private lateinit var databaseManager: DatabaseManager
    private lateinit var category: String
    private lateinit var mUsername: String
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_image)

        // Initialize views and variables
        initializeViews()

        // Set up Retrofit service
        setupRetrofitService()

        // Set up click listeners
        setupClickListeners()

        // Initialize DatabaseManager
        databaseManager = DatabaseManager(this)
    }

    private fun initializeViews() {
        imageView = findViewById(R.id.image_view)
        mUsername = intent.getStringExtra(Constants.USER_NAME).toString()
    }

    private fun setupRetrofitService() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    private fun setupClickListeners() {
        val selectImageButton: Button = findViewById(R.id.upload_image)
        selectImageButton.setOnClickListener {
            openGallery()
        }

        val button: Button = findViewById(R.id.button_startquiz)
        button.setOnClickListener {
            if (imageView.drawable == null) {
                showToast("No image uploaded")
            } else {
                startQuiz()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            imageView.setImageURI(imageUri)
            uploadImage(imageUri)
        }
    }

    private fun uploadImage(imageUri: Uri?) {
        imageUri?.let {
            val imageFile = File(getRealPathFromURI(imageUri))
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

            // Make the request using Retrofit
            apiService.getImageEmbedding(body).enqueue(object : Callback<EmbeddingResponse> {
                override fun onResponse(call: Call<EmbeddingResponse>, response: Response<EmbeddingResponse>) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        val embeddingData = responseBody?.embedding
                        processEmbeddingData(embeddingData)
                    } else {
                        handleErrorResponse(response.code())
                    }
                }

                override fun onFailure(call: Call<EmbeddingResponse>, t: Throwable) {
                    handleFailure(t)
                }
            })
        }
    }

    private fun processEmbeddingData(embeddingData: List<Double>?) {
        embeddingData?.let {
            // Search for similar images in the database
            val imageObjects = databaseManager.search(embeddingData)
            category = findMostFrequentCategory(imageObjects) ?: ""
            Log.i("CATEGORY", category)
        }
    }

    private fun startQuiz() {
        val intent = Intent(this, QuizQuestionActivity::class.java)
        intent.putExtra(Constants.USER_NAME, mUsername)
        intent.putExtra(Constants.CATEGORY, category)
        startActivity(intent)
        finish()
    }

    private fun handleErrorResponse(code: Int) {
        Log.i("RESP", "FAIL: $code")
        showToast("Failed to upload image")
    }

    private fun handleFailure(t: Throwable) {
        Log.e("RESP", "Upload failed", t)
        showToast("Failed to upload image")
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    fun findMostFrequentCategory(imageObjects: List<ImageObject>): String? {
        // Create a map to store the count of each category
        val categoryCountMap = mutableMapOf<String, Int>()

        // Iterate over the list of ImageObject and count the occurrences of each category
        for (imageObject in imageObjects) {
            val category = imageObject.category
            val count = categoryCountMap.getOrDefault(category, 0)
            categoryCountMap[category] = count + 1
        }

        // Find the category with the highest count
        var maxCategory: String? = null
        var maxCount = 0
        for ((category, count) in categoryCountMap) {
            if (count > maxCount) {
                maxCategory = category
                maxCount = count
            }
        }

        return maxCategory
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
        cursor?.moveToFirst()
        val columnIndex: Int = cursor?.getColumnIndex(filePathColumn[0]) ?: -1
        val imgDecodableString: String? = if (columnIndex != -1) cursor?.getString(columnIndex) else null
        cursor?.close()
        Log.i("URI",imgDecodableString!!)
        return imgDecodableString
    }
}