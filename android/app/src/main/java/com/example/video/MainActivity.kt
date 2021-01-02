package com.example.video

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

// Video code from:
// https://medium.com/@tylerwalker/integrating-camera2-api-on-android-feat-kotlin-4a4e65dc593f

/** Helper to ask camera permission.  */
object CameraPermissionHelper {
    private const val CAMERA_PERMISSION_CODE = 0
    private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    /** Check to see we have the necessary permissions for this app.  */
    fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
                activity, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE)
    }

    /** Check to see if we need to show the rationale for this permission.  */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION)
    }

    /** Launch Application Setting to grant permission.  */
    fun launchPermissionSettings(activity: Activity) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", activity.packageName, null)
        activity.startActivity(intent)
    }
}

class MainActivity : AppCompatActivity() {
    val tag = "dogs"
    var imageReader: ImageReader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
            return
        }


        val surfaceReadyCallback = object: SurfaceHolder.Callback {
            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) { }
            override fun surfaceDestroyed(p0: SurfaceHolder?) { }

            override fun surfaceCreated(p0: SurfaceHolder?) {
                startCameraSession()
            }
        }

        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)
        surfaceView.holder.addCallback(surfaceReadyCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }

        recreate()
    }

    private fun startCameraSession() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (cameraManager.cameraIdList.isEmpty()) {
            // no cameras
            return
        }
        val firstCamera = cameraManager.cameraIdList[0]
        try {
            cameraManager.openCamera(firstCamera, object: CameraDevice.StateCallback() {
                override fun onDisconnected(p0: CameraDevice) {
                    Log.e(tag, "camera disconnect")
                }
                override fun onError(p0: CameraDevice, p1: Int) {
                    Log.e(tag, "camera error")
                }

                override fun onOpened(cameraDevice: CameraDevice) {
                    // use the camera
                    Log.e(tag, "camera opened")
                    val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraDevice.id)

                    cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.let { streamConfigurationMap ->
                        streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)?.let { yuvSizes ->
                            val previewSize = yuvSizes.last()
                            val displayRotation = windowManager.defaultDisplay.rotation
                            val swappedDimensions = areDimensionsSwapped(displayRotation, cameraCharacteristics)
                            // swap width and height if needed
                            val rotatedPreviewWidth = if (swappedDimensions) previewSize.height else previewSize.width
                            val rotatedPreviewHeight = if (swappedDimensions) previewSize.width else previewSize.height
                            val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)
                            //surfaceView.holder.setFixedSize(rotatedPreviewWidth, rotatedPreviewHeight)

                            val previewSurface = surfaceView.holder.surface

                            var count = 0
                            val period = 5
                            val timestamp = System.currentTimeMillis() / 1000;
                            val sessionId = timestamp.toString()
                            var startTime = System.currentTimeMillis();

                            val folder = File(getExternalFilesDir(null), sessionId)
                            val watcher = Watcher(folder.absolutePath, sessionId)

                            val timerTask = object: TimerTask() {
                                override fun run() {
                                    watcher.checkForFiles()
                                    //watcher.enqueueBatches()
                                    //watcher.processQueue()
                                }
                            }

                            val timer = Timer().scheduleAtFixedRate(timerTask,0,5000);

                            val timerTaskQueue = object: TimerTask() {
                                override fun run() {
                                    watcher.processQueue()
                                }
                            }

                            val timer2 = Timer().scheduleAtFixedRate(timerTaskQueue,0,5000);

                            val listener = ImageReader.OnImageAvailableListener {
                                val image = it.acquireLatestImage()
                                // Log.e("dogs vid - " + count.toString(), image.toString())
                                if (count % period == 0) {
                                    val buffer: ByteBuffer = image.planes[0].getBuffer()
                                    val data = ByteArray(buffer.capacity())
                                    buffer.get(data)
                                    val fileName = fileIdToFileName((count/period).toString())
                                    writeToFile(fileName, watcher.imagesDir, data)
                                    val currentTime = System.currentTimeMillis()
                                    val dif = currentTime - startTime
                                    startTime = currentTime
                                    Log.e("image capture time - ", dif.toString())
                                }
                                image.close()
                                count++
                            }

                            if (imageReader == null) {
                                imageReader = ImageReader.newInstance(rotatedPreviewWidth, rotatedPreviewHeight, ImageFormat.JPEG , 2).apply {
                                    setOnImageAvailableListener(listener, Handler { true })
                                }
                            }

                            val imReaderSurface = imageReader?.surface as Surface
                            //val targets = listOf(previewSurface, imReaderSurface)
                            //val targets = listOf(previewSurface)
                            val targets = listOf(imReaderSurface)

                            val captureCallback = object : CameraCaptureSession.StateCallback()
                            {
                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    Log.e(tag, "onConfigureFailed")
                                }

                                override fun onConfigured(session: CameraCaptureSession) {
                                    Log.e(tag, "onConfigured")
                                    // session configured
//                                    val previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//                                            .apply {
//                                                addTarget(previewSurface)
//                                            }
                                    val recordRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                                            .apply {
                                                addTarget(imReaderSurface as Surface)
                                            }
//                                    session.setRepeatingRequest(
//                                            previewRequestBuilder.build(),
//                                            object: CameraCaptureSession.CaptureCallback() {},
//                                            Handler { true }
//                                    )

                                    session.setRepeatingRequest(
                                            recordRequestBuilder.build(),
                                            object: CameraCaptureSession.CaptureCallback() {},
                                            Handler { true }
                                    )
                                }
                            }


                            // Activate camera capture
                            cameraDevice.createCaptureSession(targets, captureCallback, Handler { true })
                        }
                    }
                }
            }, Handler { true })
        } catch (exception: SecurityException) {
            Log.v("Mainact", "");
        }
    }

    private fun areDimensionsSwapped(displayRotation: Int, cameraCharacteristics: CameraCharacteristics): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 90 || cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 0 || cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                // invalid display rotation
            }
        }
        return swappedDimensions
    }

    private fun writeToFile(fileName: String, folder: File, content: ByteArray) {
        var file: File = File(folder, fileName)
        try {
            val fileOutPutStream = FileOutputStream(file)
            fileOutPutStream.write(content)
            fileOutPutStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}