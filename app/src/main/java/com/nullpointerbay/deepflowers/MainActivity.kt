package com.nullpointerbay.deepflowers

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Typeface
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Size
import android.util.TypedValue
import android.view.WindowManager
import android.widget.Toast


class MainActivity : AppCompatActivity(), ImageReader.OnImageAvailableListener {

    private val DESIRED_PREVIEW_SIZE = Size(640, 480)

    private val classifier: Classifier? = null

    private val sensorOrientation: Int? = null

    private val previewWidth = 0
    private val previewHeight = 0
    private val yuvBytes: Array<ByteArray>? = null
    private val rgbBytes: IntArray? = null
    private val rgbFrameBitmap: Bitmap? = null
    private val croppedBitmap: Bitmap? = null

    private val cropCopyBitmap: Bitmap? = null

    private val computing = false

    private val frameToCropTransform: Matrix? = null
    private val cropToFrameTransform: Matrix? = null

//    private val resultsView: ResultsView? = null

//    private val borderedText: BorderedText? = null

    private val lastProcessingTimeMs: Long = 0

    override fun onImageAvailable(reader: ImageReader?) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

    }

    private fun setFragment() {
        val fragment = CameraFragment.newInstance(
                object : CameraFragment.ConnectionCallback() {
                    fun onPreviewSizeChosen(size: Size, rotation: Int) {
                        this@MainActivity.onPreviewSizeChosen(size, rotation)
                    }
                },
                this,
                getLayoutId(),
                getDesiredPreviewFrameSize())

        fragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit()

    }

    private fun getDesiredPreviewFrameSize() = DESIRED_PREVIEW_SIZE

    private fun getLayoutId() = R.layout.camera_connection_fragment


    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }


    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) || shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(this@MainActivity, "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show()
            }
            requestPermissions(arrayOf(PERMISSION_CAMERA, PERMISSION_STORAGE), PERMISSIONS_REQUEST)
        }

    }

    fun onPreviewSizeChosen(size: Size, rotation: Int) {
        val textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics)
        borderedText = BorderedText(textSizePx)
        borderedText.setTypeface(Typeface.MONOSPACE)

        classifier = TensorFlowImageClassifier.create(
                assets,
                MODEL_FILE,
                LABEL_FILE,
                INPUT_SIZE,
                IMAGE_MEAN,
                IMAGE_STD,
                INPUT_NAME,
                OUTPUT_NAME)

        resultsView = findViewById(R.id.results) as ResultsView
        previewWidth = size.width
        previewHeight = size.height

        val display = windowManager.defaultDisplay
        val screenOrientation = display.rotation


        sensorOrientation = rotation + screenOrientation

        rgbBytes = IntArray(previewWidth * previewHeight)
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)

        frameToCropTransform = ImageUtils.getTransformationMatrix(
                previewWidth, previewHeight,
                INPUT_SIZE, INPUT_SIZE,
                sensorOrientation, MAINTAIN_ASPECT)

        cropToFrameTransform = Matrix()
        frameToCropTransform.invert(cropToFrameTransform)

        yuvBytes = arrayOfNulls<ByteArray>(3)

        addCallback(
                object : DrawCallback() {
                    fun drawCallback(canvas: Canvas) {
                        renderDebug(canvas)
                    }
                })
    }

    companion object {
        private val INPUT_NAME = "input"
        private val OUTPUT_NAME = "final_result"
        private val PERMISSIONS_REQUEST = 1
        private val PERMISSION_CAMERA = Manifest.permission.CAMERA
        private val PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
}
