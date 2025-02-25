package com.blank.kotlinproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var filterRecyclerView: RecyclerView
    private var selectedBitmap: Bitmap? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { loadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        selectImageButton = findViewById(R.id.selectImageButton)
        filterRecyclerView = findViewById(R.id.filterRecyclerView)

        setupRecyclerView()
        setupImagePicker()
        checkPermissions()
    }

    private fun loadLutFiles(): List<String> {
        return try {
            // List all files from the "luts" folder in assets
            assets.list("luts")?.filter { it.endsWith(".png") }?.toList() ?: emptyList()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun applyFilter(sourceBitmap: Bitmap, lutFileName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load the LUT file from assets
                val lutBitmap = assets.open("luts/$lutFileName").use { input ->
                    BitmapFactory.decodeStream(input)
                }

                println(lutBitmap)

                // Create filter instance and apply
                val filter = HaldLutFilter()
                val filteredBitmap = filter.applyLut(sourceBitmap, lutBitmap)

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(filteredBitmap)
                }

                // Cleanup
                lutBitmap.recycle()

            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Show error message to user
                    Toast.makeText(this@MainActivity, "Error applying filter", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        val lutFiles = loadLutFiles() // Load LUT files from assets
        filterRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        filterRecyclerView.adapter = FilterAdapter(this, lutFiles) { lutFile ->
            selectedBitmap?.let { bitmap ->
                applyFilter(bitmap, lutFile)
            }
        }
    }

    private fun setupImagePicker() {
        selectImageButton.setOnClickListener {
            getContent.launch("image/*")
        }
    }

    private fun loadImage(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            withContext(Dispatchers.Main) {
                selectedBitmap = bitmap
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }


}