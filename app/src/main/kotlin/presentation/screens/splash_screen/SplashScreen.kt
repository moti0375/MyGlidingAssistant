package com.bartovapps.gpstriprec.presentation.screens.splash_screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bartovapps.gpstriprec.R
import com.bartovapps.gpstriprec.presentation.screens.main_screen.MainScreen
import com.bartovapps.gpstriprec.utils.Utils
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


@AndroidEntryPoint
class SplashScreen : AppCompatActivity() {

    private val permissionsViewModel: PermissionsViewModel by viewModels()
    private var version: String? = null
    private lateinit var iv: ImageView
    var attachmentFile: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionsViewModel.onStepResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.splash_screen)
        setupUi()
        setupObservers()

        permissionsViewModel.initPermissionChain(
            hasLocation = isGranted(Manifest.permission.ACCESS_FINE_LOCATION),
            hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                isGranted(Manifest.permission.POST_NOTIFICATIONS) else true,
            hasMediaImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                isGranted(Manifest.permission.READ_MEDIA_IMAGES) else true,
            hasStorage = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) else true
        )
    }


    private fun setupUi() {
        attachmentFile = getExternalFilesDir(null).toString() + "/" + "attachment.kml"
        version = Utils.getApplicationVersion(this@SplashScreen)
        iv = findViewById(R.id.splashImage)


        Picasso.with(this)
            .load(R.drawable.splash)
            .fit()
            .centerInside()
            .into(iv)
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupObservers() {
        permissionsViewModel.currentStep.observe(this) { step ->
            if (step != null) {
                showRationaleDialog(step)
            } else if (permissionsViewModel.navigateToMain.value != true) {
                // If step is null and we aren't moving to main, a mandatory permission was denied
                finish()
            }
        }

        permissionsViewModel.navigateToMain.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                scheduleMainActivity()
            }
        }
    }


    private fun showRationaleDialog(step: PermissionsViewModel.PermissionStep) {
        AlertDialog.Builder(this)
            .setTitle(step.title)
            .setMessage(step.message)
            .setCancelable(false)
            .setPositiveButton("Allow") { _, _ ->
                requestPermissionLauncher.launch(step.permission)
            }
            .setNegativeButton("Refuse") { dialog, _ ->
                dialog.dismiss()
                permissionsViewModel.onStepResult(false)
            }
            .show()
    }
    private fun scheduleMainActivity() {
        Handler(Looper.getMainLooper()).postDelayed(
            Runnable { this.startMainActivity() },
            SPLASH_TIMEOUT
        )
    }

    private fun startMainActivity() {
        var path: String? = null

        if (intent.scheme == null) {
            startActivity(Intent(this@SplashScreen, MainScreen::class.java))
            finish()
            return
        }

        if (intent.scheme == "file") {
            path = intent.data?.path
            //            Log.i(LOG_TAG, "Started from a local file..");
//            Log.i(LOG_TAG, "type is: " + launcherIntent.getType());
//            Log.i(LOG_TAG, "Host is: " + getIntent().getData().getHost());
            val i = Intent(this@SplashScreen, MainScreen::class.java)
            path?.let {
                i.putExtra("kml_path", it)
            }
            startActivity(i)
            finish()
            return
        }


        if (intent.scheme == "content") {
//            Log.i(LOG_TAG, "Started from a email attachment..");
//            Log.i(LOG_TAG, "Type is: " + getIntent().getType());
//            Log.i(LOG_TAG, "Host is: " + getIntent().getData().getHost());
            val uri = intent.data
            uri?.let {
                try {
                    val attachment = contentResolver.openInputStream(uri)
                    if (attachment == null) {
//                        Log.i(LOG_TAG, "not from mail attachment.. getting path from data..");
                        val i = Intent(this@SplashScreen, MainScreen::class.java)
                        startActivity(i)
                    } else {
//                        Log.i(LOG_TAG, "Received file from attachment");
                        val uploadAttachmentTask = UploadAttachmentTask()
                        val container = Container()
                        container.fileName = attachmentFile
                        container.uri = uri
                        uploadAttachmentTask.execute(container)
                        //path = getFileFromAttachment(uri);
                    }
                    //                    Log.i(LOG_TAG, "Got path: " + path);
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    //                    Log.i(LOG_TAG, "File not found exception..");
                } catch (e: SecurityException) {
                    Toast.makeText(
                        this@SplashScreen,
                        getString(R.string.error_access_attachment),
                        Toast.LENGTH_SHORT
                    ).show()
                    val i = Intent(this@SplashScreen, MainScreen::class.java)
                    startActivity(i)
                    finish()
                }

            }
        }
    }


    private inner class UploadAttachmentTask : AsyncTask<Container, Void?, Container>() {
        override fun doInBackground(vararg params: Container): Container {
            val container: Container = params[0]

            try {

                val attachment = container.uri?.let {
                    contentResolver.openInputStream(it)
                }
                val tmp = FileOutputStream(container.fileName)

                val bis = BufferedInputStream(attachment)
                val b = StringBuffer()

                while (bis.available() != 0) {
                    val c = bis.read().toChar()
                    b.append(c)
                }

                bis.close()
                attachment?.close()
                tmp.write(b.toString().toByteArray())
                tmp.close()
                container.status = true
            } catch (e: FileNotFoundException) {
                container.status = false
                //                Log.i(LOG_TAG, "File not found exception..");
                e.printStackTrace()
            } catch (e: IOException) {
                container.status = false
                //                Log.i(LOG_TAG, "IOException..");
                e.printStackTrace()
            }
            return container
        }


        override fun onPostExecute(container: Container?) {
            if (container?.status == true) {
                if (container.fileName != null) {
                    val i = Intent(this@SplashScreen, MainScreen::class.java).also {
                        it.putExtra("kml_path", container.fileName)
                    }
                    startActivity(i)
                    finish()
                }
            }
        }

    }

    internal class Container {
        var uri: Uri? = null
        var fileName: String? = null
        var status: Boolean = false
    }

    companion object {
        private val LOG_TAG: String = SplashScreen::class.java.getSimpleName()
        private const val SPLASH_TIMEOUT: Long = 3500
    }
}