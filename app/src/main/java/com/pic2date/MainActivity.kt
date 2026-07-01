package com.pic2date

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pic2date.ui.ScanViewModel
import com.pic2date.ui.navigation.AppRoot
import com.pic2date.ui.theme.Pic2DateTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    private var cameraImageUri: Uri? = null

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.scan(it, contentResolver.getType(it)) }
    }

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) cameraImageUri?.let { viewModel.scan(it, "image/jpeg") }
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launchCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            Pic2DateTheme {
                AppRoot(
                    viewModel = viewModel,
                    onPickImage = {
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onTakePhoto = ::onTakePhoto,
                )
            }
        }

        // Only handle the launch intent on first creation to avoid re-scanning
        // after a configuration change / process recreation.
        if (savedInstanceState == null) {
            handleShareIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /** Handles images/PDFs shared (ACTION_SEND) or opened (ACTION_VIEW). */
    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
        uri?.let { viewModel.scan(it, intent.type) }
    }

    private fun onTakePhoto() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val uri = createCameraUri()
        cameraImageUri = uri
        takePicture.launch(uri)
    }

    private fun createCameraUri(): Uri {
        val dir = File(cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }
}
