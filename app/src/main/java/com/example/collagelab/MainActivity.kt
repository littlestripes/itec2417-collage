package com.example.collagelab

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Date

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    // access buttons here
    private lateinit var imageButtons: List<ImageButton>

    private lateinit var mainView: View

    // generated before picture is taken
    private var newPhotoPath: String? = null
    // access picture is one is taken
    private var visibleImagePath: String? = null

    // access image file paths here
    private var photoPaths: ArrayList<String?> = arrayListOf(null, null, null, null)

    private var whichImageIndex: Int? = null

    private var currentPhotoPath: String? = null

    // bundle keys
    private val NEW_PHOTO_PATH_KEY = "new photo path key"
    private val PHOTO_PATH_LIST_ARRAY_KEY = "photo path list array key"
    private val IMAGE_INDEX_KEY = "image index key"

    private val cameraActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result -> handleImage(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        whichImageIndex = savedInstanceState?.getInt(IMAGE_INDEX_KEY)
        newPhotoPath = savedInstanceState?.getString(NEW_PHOTO_PATH_KEY)
        photoPaths = savedInstanceState?.getStringArrayList(PHOTO_PATH_LIST_ARRAY_KEY) ?: arrayListOf(
            null,
            null,
            null,
            null
        )

        mainView = findViewById(R.id.content)

        imageButtons = listOf(
            findViewById(R.id.imageButton1),
            findViewById(R.id.imageButton2),
            findViewById(R.id.imageButton3),
            findViewById(R.id.imageButton4)
        )

        for (imageButton: ImageButton in imageButtons) {
            imageButton.setOnClickListener { ib ->
                takePictureFor(ib as ImageButton)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // ArrayList instead of List because it needs to be saved in instance state
        outState.putStringArrayList(PHOTO_PATH_LIST_ARRAY_KEY, photoPaths)
        outState.putString(NEW_PHOTO_PATH_KEY, newPhotoPath)
        whichImageIndex?.let { outState.putInt(IMAGE_INDEX_KEY, it) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // not using zip's return value here, just using the second arg (the transform) to
            // perform the necessary operation
            imageButtons.zip(photoPaths) { imageButton, photoPath ->
                photoPath?.let {  // ad-hoc null check; if path isn't null, load img
                    loadImage(imageButton, photoPath)
                }
            }
        }
    }

    private fun takePicture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val (photoFile, photoFilePath) = createImageFile()
        if (photoFile != null) {
            newPhotoPath = photoFilePath
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.example.collagelab.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraActivityLauncher.launch(takePictureIntent)
        }
    }

    // returns a File and the absolute path of the File
    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): Pair<File?, String?> {
        return try {
            val dateTime = SimpleDateFormat("yyyyMMdd__HHmmss").format(Date())
            val imageFilename = "COLLAGE_$dateTime"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(imageFilename, ".jpg", storageDir)
            // "to" keyword used for Pairs
            file to file.absolutePath
        } catch (ex: IOException) {
            null to null
        }
    }

    private fun handleImage(result: ActivityResult) {
        // when pic taken, photoPath in photoPaths should be overwritten
        when (result.resultCode) {
            RESULT_OK -> {
                Log.d(TAG, "Result ok, image at $newPhotoPath")
                // overwrite idx with current file to prepare to load new image
                whichImageIndex?.let { photoPaths[it] = newPhotoPath }
            }
            RESULT_CANCELED -> {
                Log.d(TAG, "Result cancelled, no picture taken")
            }
        }
    }

    private fun loadImage(imageButton: ImageButton, photoFilePath: String) {
        Picasso.get()
            .load(File(photoFilePath))
            .error(android.R.drawable.stat_notify_error)  // shows error icon when error
            .fit()  // attempt resize to fit within ImageButton
            .centerCrop()  // crop to fit
            .into(imageButton, object: Callback {
                override fun onSuccess() {
                    Log.d(TAG, "successfully loaded $photoFilePath")
                }

                override fun onError(e: Exception?) {
                    Log.e(TAG, "error loading $photoFilePath", e)
                }
            })
    }

    private fun takePictureFor(imageButton: ImageButton) {
        val index = imageButtons.indexOf(imageButton)
        whichImageIndex = index

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val (photoFile, photoFilePath) = createImageFile()

        if (photoFile != null) {
            currentPhotoPath = photoFilePath
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.example.collagelab.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraActivityLauncher.launch(takePictureIntent)
        }
    }
}