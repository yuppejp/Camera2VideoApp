package com.android.example.camera2videoapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Build
import android.view.Surface
import android.view.TextureView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
//import kotlinx.android.synthetic.main.activity_main.*
import android.media.MediaRecorder
import android.widget.Button
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var cameraManager: CameraManager
    private lateinit var frontCameraId: String
    private lateinit var backCameraId: String

    // recording
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var cameraDevice: CameraDevice
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var recButton: Button
    private lateinit var stopButton: Button

    // add myselft
    private lateinit var front_preview: TextureView
    private lateinit var back_preview: TextureView
    private lateinit var context: Context

    private val frontCameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            startCameraPreview(camera, front_preview)
        }

        override fun onDisconnected(camera: CameraDevice) {}
        override fun onError(camera: CameraDevice, error: Int) {}
    }

    private val backCameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            startCameraPreview(camera, back_preview)
        }

        override fun onDisconnected(camera: CameraDevice) {}
        override fun onError(camera: CameraDevice, error: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // add myself
        front_preview = findViewById<TextureView>(R.id.front_preview)
        back_preview = findViewById<TextureView>(R.id.back_preview)
        context = this
        recButton = findViewById<Button>(R.id.recButton)
        stopButton = findViewById<Button>(R.id.stopButton)

        recButton.setOnClickListener {
            startRecordingVideo()
        }
        stopButton.setOnClickListener {
            stopRecordingVideo()
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

//        val permissionRequest =
//            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//                if (granted) {
//                    openCameraDevices()
//                } else {
//                    finish()
//                }
//            }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.CAMERA
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                openCameraDevices()
//            } else {
//                permissionRequest.launch(Manifest.permission.CAMERA)
//            }
//        } else {
//            openCameraDevices()
//        }

         val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[Manifest.permission.CAMERA] == true &&
                    permissions[Manifest.permission.RECORD_AUDIO] == true
                ) {
                    // Both permissions granted, proceed with your operation
                    openCameraDevices()
                } else {
                    // One or both permissions denied, show a message or close the app
                    finish()
                }
            }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                // Both permissions are already granted, proceed with your operation
                openCameraDevices()
            }
            else -> {
                // Request the permissions
                requestPermissionLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            }
        }

    }

    private fun openCameraDevices() {
        for (cameraId in cameraManager.cameraIdList) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val cameraLensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

            if (cameraLensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                frontCameraId = cameraId
            } else if (cameraLensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                backCameraId = cameraId
            }
        }

        front_preview.surfaceTextureListener = textureListener
        back_preview.surfaceTextureListener = backCameratextureListener
    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { // add myself

                if (::frontCameraId.isInitialized && front_preview.isAvailable) {
                    cameraManager.openCamera(frontCameraId, frontCameraStateCallback, null)
                }

//                if (::backCameraId.isInitialized && back_preview.isAvailable) {
//                    cameraManager.openCamera(backCameraId, backCameraStateCallback, null)
//                }
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return true
        }
    }

    private val backCameratextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { // add myself

                if (::backCameraId.isInitialized && back_preview.isAvailable) {
                    cameraManager.openCamera(backCameraId, backCameraStateCallback, null)
                }
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return true
        }
    }

    private fun startCameraPreview(cameraDevice: CameraDevice, textureView: TextureView) {
        val surfaceTexture= textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(640, 480)
        val previewSurface = Surface(surfaceTexture)

        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(previewSurface)

        cameraDevice.createCaptureSession(
            listOf(previewSurface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    session.setRepeatingRequest(
                        captureRequestBuilder.build(),
                        null,
                        null
                    )
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            null
        )
    }


    private fun setupMediaRecorder() {
        mediaRecorder = MediaRecorder()

        // Configure the MediaRecorder settings
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setVideoSize(1920, 1080)
        mediaRecorder.setVideoFrameRate(30)
        mediaRecorder.setOutputFile("path/to/your/output/file.mp4")

        try {
            mediaRecorder.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun startRecordingVideo() {
        setupMediaRecorder()

        val targets = arrayListOf<Surface>()

        val previewSurface = Surface(front_preview.surfaceTexture) // Use surfaceTexture here
        targets.add(previewSurface)

        val recorderSurface = mediaRecorder.surface
        targets.add(recorderSurface)

        previewRequestBuilder.addTarget(recorderSurface)

        cameraDevice.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null)

                // Start the MediaRecorder
                mediaRecorder.start()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                // Handle the error
            }
        }, null)
    }

    private fun stopRecordingVideo() {
        mediaRecorder.stop()
        mediaRecorder.reset()

        // Remove the recorder surface from the capture session
        previewRequestBuilder.removeTarget(mediaRecorder.surface)

        // Stop the repeating request and update the preview
        captureSession.stopRepeating()
        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null)
    }
}
