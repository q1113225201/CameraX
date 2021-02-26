package com.sjl.camerax

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraView
import java.io.File
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private val executorService by lazy { Executors.newSingleThreadExecutor() }
    val cameraView by lazy { findViewById<CameraView>(R.id.camera_view) }
    val cameraSwitch by lazy { findViewById<AppCompatImageButton>(R.id.camera_switch) }
    val takePicture by lazy { findViewById<AppCompatImageButton>(R.id.take_picture) }

    @RequiresPermission(Manifest.permission.CAMERA)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 10
            )
        } else {
            cameraView.bindToLifecycle(this)
            cameraView.cameraLensFacing = CameraSelector.LENS_FACING_FRONT
        }
        takePicture.setOnClickListener {
            takePicture()
        }
        cameraSwitch.setOnClickListener {
            switchLensFacing()
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraView.bindToLifecycle(this)
        cameraView.cameraLensFacing = CameraSelector.LENS_FACING_FRONT
    }

    /**
     * 切换镜头
     */
    private fun switchLensFacing() {
        cameraView.toggleCamera()
        val isFacingBack = cameraView.cameraLensFacing == CameraSelector.LENS_FACING_FRONT
        cameraSwitch.animate().rotationY(if (isFacingBack) 180f else 0f).start()
    }

    /**
     * 拍照
     */
    private fun takePicture() {
        val fileOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            getContentValues()
        ).build()
        cameraView.takePicture(
            fileOptions,
            executorService,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.e("onImageSaved", "Take Picture Uri：${outputFileResults.savedUri}")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("onError", "Take Picture Error：${exception.message}")
                }
            })
    }

    /**
     * 使用ContentValues存储图片输出信息
     */
    fun getContentValues(): ContentValues {
        // 创建拍照后输出的图片文件名
        val fileName = "${System.currentTimeMillis()}.jpg"
        return ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            // 适配Android Q版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            } else {
                val fileDir = File(
                    Environment.getExternalStorageDirectory(),
                    Environment.DIRECTORY_PICTURES
                ).also { if (!it.exists()) it.mkdir() }
                val filePath = fileDir.absolutePath + File.separator + fileName
                put(MediaStore.Images.Media.DATA, filePath)
            }
        }
    }
}