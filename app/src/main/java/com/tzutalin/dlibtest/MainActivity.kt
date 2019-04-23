package com.tzutalin.dlibtest

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat

import com.dexafree.materialList.card.Card
import com.dexafree.materialList.card.provider.BigImageCardProvider
import com.dexafree.materialList.view.MaterialListView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tzutalin.dlib.Constants
import com.tzutalin.dlib.FaceDet
import com.tzutalin.dlib.PedestrianDet
import com.tzutalin.dlib.VisionDetRet

import java.io.File
import java.util.ArrayList

import hugo.weaving.DebugLog
import timber.log.Timber


open class MainActivity : AppCompatActivity() {
    // UI
    private var mDialog: ProgressDialog? = null
    private var mListView: MaterialListView? = null
    private var mFabActionBt: FloatingActionButton? = null
    private var mFabCamActionBt: FloatingActionButton? = null
    private var mToolbar: Toolbar? = null

    private var mTestImgPath: String? = null
    private var mFaceDet: FaceDet? = null
    private var mPersonDet: PedestrianDet? = null
    private val mCard = ArrayList<Card>()

    /* Checks if external storage is available for read and write */
    private val isExternalStorageWritable: Boolean
        @DebugLog
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    /* Checks if external storage is available to at least read */
    private val isExternalStorageReadable: Boolean
        @DebugLog
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mListView = findViewById<View>(R.id.material_list_view) as MaterialListView
        setSupportActionBar(mToolbar)
        // Just use hugo to print log
        isExternalStorageWritable
        isExternalStorageReadable

        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        val currentApiVersion = Build.VERSION.SDK_INT

        if (currentApiVersion >= Build.VERSION_CODES.M) {
            verifyPermissions(this)
        }

        setupUI()
    }

    private fun setupUI() {
        mListView = findViewById<View>(R.id.material_list_view) as MaterialListView
        mFabActionBt = findViewById<View>(R.id.fab) as FloatingActionButton
        mFabCamActionBt = findViewById<View>(R.id.fab_cam) as FloatingActionButton
        mToolbar = findViewById<View>(R.id.toolbar) as Toolbar

        mFabActionBt!!.setOnClickListener {
            // launch Gallery
            Toast.makeText(this@MainActivity, "Pick one image", Toast.LENGTH_SHORT).show()
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, RESULT_LOAD_IMG)
        }

        mFabCamActionBt!!.setOnClickListener { startActivity(Intent(this@MainActivity, CameraActivity::class.java)) }

        mToolbar!!.title = getString(R.string.app_name)
        Toast.makeText(this@MainActivity, getString(R.string.description_info), Toast.LENGTH_LONG).show()
    }

    @DebugLog
    protected fun demoStaticImage() {
        if (mTestImgPath != null) {
            Timber.tag(TAG).d("demoStaticImage() launch a task to det")
            runDemosAsync(mTestImgPath!!)
        } else {
            Timber.tag(TAG).d("demoStaticImage() mTestImgPath is null, go to gallery")
            Toast.makeText(this@MainActivity, "Pick an image to run algorithms", Toast.LENGTH_SHORT).show()
            // Create intent to Open Image applications like Gallery, Google Photos
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, RESULT_LOAD_IMG)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            Toast.makeText(this@MainActivity, "Demo using static images", Toast.LENGTH_SHORT).show()
            demoStaticImage()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == Activity.RESULT_OK && null != data) {
                // Get the Image from data
                val selectedImage = data.data
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                // Get the cursor
                val cursor = contentResolver.query(selectedImage!!, filePathColumn, null, null, null)
                cursor!!.moveToFirst()
                val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                mTestImgPath = cursor.getString(columnIndex)
                cursor.close()
                if (mTestImgPath != null) {
                    runDemosAsync(mTestImgPath!!)
                    Toast.makeText(this, "Img Path:" + mTestImgPath!!, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
        }

    }

    // ==========================================================
    // Tasks inner class
    // ==========================================================

    private fun runDemosAsync(imgPath: String) {
        demoPersonDet(imgPath)
        demoFaceDet(imgPath)
    }

    @SuppressLint("StaticFieldLeak")
    private fun demoPersonDet(imgPath: String) {
        object : AsyncTask<Void, Void, List<VisionDetRet>>() {
            override fun onPostExecute(personList: List<VisionDetRet>) {
                super.onPostExecute(personList)
                if (personList.isNotEmpty()) {
                    val card = Card.Builder(this@MainActivity)
                            .withProvider(BigImageCardProvider::class.java)
                            .setDrawable(drawRect(imgPath, personList, Color.BLUE))
                            .setTitle("Person det")
                            .endConfig()
                            .build()
                    mCard.add(card)
                } else {
                    Toast.makeText(applicationContext, "No person", Toast.LENGTH_LONG).show()
                }
                updateCardListView()
            }

            override fun doInBackground(vararg voids: Void): List<VisionDetRet>? {
                // Init
                if (mPersonDet == null) {
                    mPersonDet = PedestrianDet()
                }

                Timber.tag(TAG).d("Image path: $imgPath")

                return mPersonDet!!.detect(imgPath)
            }
        }.execute()
    }

    @SuppressLint("StaticFieldLeak")
    private fun demoFaceDet(imgPath: String) {
        object : AsyncTask<Void, Void, List<VisionDetRet>>() {
            override fun onPreExecute() {
                super.onPreExecute()
                showDialog("Detecting faces")
            }

            override fun onPostExecute(faceList: List<VisionDetRet>) {
                super.onPostExecute(faceList)
                if (faceList.isNotEmpty()) {
                    val card = Card.Builder(this@MainActivity)
                            .withProvider(BigImageCardProvider::class.java)
                            .setDrawable(drawRect(imgPath, faceList, Color.GREEN))
                            .setTitle("Face det")
                            .endConfig()
                            .build()
                    mCard.add(card)
                } else {
                    Toast.makeText(applicationContext, "No face", Toast.LENGTH_LONG).show()
                }
                updateCardListView()
                dismissDialog()
            }

            override fun doInBackground(vararg voids: Void): List<VisionDetRet>? {
                // Init
                if (mFaceDet == null) {
                    mFaceDet = FaceDet(Constants.faceShapeModelPath)
                }

                val targetPath = Constants.faceShapeModelPath
                if (!File(targetPath).exists()) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Copy landmark model to $targetPath", Toast.LENGTH_SHORT).show() }
                    FileUtils.copyFileFromRawToOthers(applicationContext, R.raw.shape_predictor_68_face_landmarks, targetPath)
                }

                return mFaceDet!!.detect(imgPath)
            }
        }.execute()
    }

    private fun updateCardListView() {
        mListView!!.clearAll()
        for (each in mCard) {
            mListView!!.add(each)
        }
    }

    private fun showDialog(title: String) {
        dismissDialog()
        mDialog = ProgressDialog.show(this@MainActivity, title, "process..", true)
    }

    private fun dismissDialog() {
        if (mDialog != null) {
            mDialog!!.dismiss()
            mDialog = null
        }
    }

    @DebugLog
    private fun drawRect(path: String, results: List<VisionDetRet>, color: Int): BitmapDrawable {
        val options = BitmapFactory.Options()
        options.inSampleSize = 1
        var bm = BitmapFactory.decodeFile(path, options)
        var bitmapConfig: Bitmap.Config? = bm.config
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true)
        val width = bm.width
        // By ratio scale
        val aspectRatio = bm.width / bm.height.toFloat()

        val maxSize = 512
        val newHeight: Int
        var resizeRatio = 1f
        newHeight = Math.round(maxSize / aspectRatio)

        if (bm.width > maxSize && bm.height > maxSize) {
            Timber.tag(TAG).d("Resize Bitmap")
            bm = getResizedBitmap(bm, maxSize, newHeight)
            resizeRatio = bm.width.toFloat() / width.toFloat()
            Timber.tag(TAG).d("resizeRatio $resizeRatio")
        }

        // Create canvas to draw
        val canvas = Canvas(bm)
        val paint = Paint()
        paint.color = color
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        // Loop result list
        for (ret in results) {
            val bounds = Rect()
            bounds.left = (ret.mLeft * resizeRatio).toInt()
            bounds.top = (ret.mTop * resizeRatio).toInt()
            bounds.right = (ret.mRight * resizeRatio).toInt()
            bounds.bottom = (ret.mBottom * resizeRatio).toInt()
            canvas.drawRect(bounds, paint)
            // Get landmark
            val landmarks = ret.mLandmarkPoints
            for (point in landmarks) {
                val pointX = (point.x * resizeRatio).toInt()
                val pointY = (point.y * resizeRatio).toInt()
                canvas.drawCircle(pointX.toFloat(), pointY.toFloat(), 2f, paint)
            }
        }

        return BitmapDrawable(resources, bm)
    }

    @DebugLog
    private fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, true)
    }

    companion object {
        private const val TAG = "MainActivity"

        private const val RESULT_LOAD_IMG = 1
        private const val REQUEST_CODE_PERMISSION = 2

        // Storage Permissions
        private val PERMISSIONS_REQ = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)

        /**
         * Checks if the app has permission to write to device storage or open camera
         * If the app does not has permission then the user will be prompted to grant permissions
         *
         * @param activity
         */
        @DebugLog
        private fun verifyPermissions(activity: AppCompatActivity): Boolean {
            // Check if we have write permission
            val writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val cameraPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)

            return if (writePermission != PackageManager.PERMISSION_GRANTED ||
                    readPermission != PackageManager.PERMISSION_GRANTED ||
                    cameraPermission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_REQ,
                        REQUEST_CODE_PERMISSION
                )
                false
            } else {
                true
            }
        }
    }
}