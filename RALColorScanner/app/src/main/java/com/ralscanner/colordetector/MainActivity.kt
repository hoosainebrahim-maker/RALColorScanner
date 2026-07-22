package com.ralscanner.colordetector

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.ralscanner.colordetector.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // When locked, sampling pauses and the last reading stays on screen.
    private var isLocked = false

    private val samplingHandler = Handler(Looper.getMainLooper())
    private val sampleIntervalMs = 250L

    // Side length (in preview-bitmap pixels) of the square region averaged for each reading.
    private val sampleBoxSize = 24

    private val samplingRunnable = object : Runnable {
        override fun run() {
            if (!isLocked) {
                sampleCenterColor()
            }
            samplingHandler.postDelayed(this, sampleIntervalMs)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                binding.colorNameText.text = getString(R.string.camera_permission_required)
                binding.hintText.visibility = View.GONE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.lockButton.setOnClickListener { toggleLock() }
        binding.previewView.setOnClickListener { toggleLock() }

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun toggleLock() {
        isLocked = !isLocked
        binding.lockButton.text =
            if (isLocked) getString(R.string.resume_scanning) else getString(R.string.tap_to_lock)
        binding.lockIndicator.visibility = if (isLocked) View.VISIBLE else View.GONE
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
                samplingHandler.post(samplingRunnable)
            } catch (exc: Exception) {
                binding.colorNameText.text = getString(R.string.camera_start_failed)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /** Reads the current preview frame, averages a small box at its center, and updates the UI. */
    private fun sampleCenterColor() {
        val bitmap: Bitmap = binding.previewView.bitmap ?: return

        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val half = sampleBoxSize / 2

        val left = (centerX - half).coerceIn(0, bitmap.width - 1)
        val top = (centerY - half).coerceIn(0, bitmap.height - 1)
        val right = (centerX + half).coerceIn(left + 1, bitmap.width)
        val bottom = (centerY + half).coerceIn(top + 1, bitmap.height)

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0

        for (x in left until right) {
            for (y in top until bottom) {
                val pixel = bitmap.getPixel(x, y)
                sumR += Color.red(pixel)
                sumG += Color.green(pixel)
                sumB += Color.blue(pixel)
                count++
            }
        }

        if (count == 0) return

        val avgR = (sumR / count).toInt()
        val avgG = (sumG / count).toInt()
        val avgB = (sumB / count).toInt()

        val match = ColorMatcher.findNearestRal(avgR, avgG, avgB)

        binding.colorSwatch.background.mutate().setTint(Color.rgb(avgR, avgG, avgB))
        binding.colorNameText.text = match.name
        binding.ralCodeText.text = getString(R.string.ral_code_format, match.code)
        binding.hintText.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        samplingHandler.removeCallbacks(samplingRunnable)
    }
}
