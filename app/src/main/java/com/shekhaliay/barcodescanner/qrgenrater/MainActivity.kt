package com.shekhaliay.barcodescanner.qrgenrater

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shekhaliay.barcodescanner.qrgenrater.databinding.ActivityMainBinding
import com.shekhaliay.barcodescanner.qrgenrater.qr_code_maker.ActivityQRcodeMaker
import com.shekhaliay.barcodescanner.qrgenrater.qr_code_reader.ActivityQRcodeReader
import java.io.IOException
import java.io.RandomAccessFile
import java.text.DecimalFormat
import java.util.regex.Pattern

class MainActivity : AppCompatActivity(), View.OnClickListener {


    private lateinit var binding: ActivityMainBinding
    private lateinit var activity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activity = this
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.cardQrCodeReader.setOnClickListener(this)
        binding.cardQrGenerator.setOnClickListener(this)
        binding.cardShare.setOnClickListener(this)
        binding.pricacyPolicy.setOnClickListener(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEnabled) {
                    isEnabled = false
                    finish()
                }
            }
        })

        val sharedPreferences = activity.getSharedPreferences("phar_id", Context.MODE_PRIVATE)
        val memoryRange = sharedPreferences.getInt(TEMPFILES, (Math.random() * 70).toInt() + 30)

        setProgressInAsync(binding.memoryPercantage, binding.memoryProgress, memoryRange, false)
        getMemorySize()
        parse()

    }

    private val TEMPFILES = "TEMPORARIESFILESALL"
    var free: Long = 0
    var total: Long = 0
    private fun parse() {
        binding.freeRam.text = calSize(this.free.toDouble()) + ""
        binding.ramTotal.text = " / " + calSize(this.total.toDouble())
    }

    private fun getMemorySize() {
        val compile = Pattern.compile("([a-zA-Z]+):\\s*(\\d+)")
        try {
            val randomAccessFile = RandomAccessFile("/proc/meminfo", "r")
            while (true) {
                val readLine: CharSequence? = randomAccessFile.readLine()
                if (readLine != null) {
                    val matcher = compile.matcher(readLine)
                    if (matcher.find()) {
                        val group = matcher.group(1)
                        val group2 = matcher.group(2)
                        if (group.equals("MemTotal", ignoreCase = true)) {
                            this.total = group2.toLong()
                        } else if (group.equals(
                                "MemFree",
                                ignoreCase = true
                            ) || group.equals("SwapFree", ignoreCase = true)
                        ) {
                            this.free = group2.toLong()
                        }
                    }
                } else {
                    randomAccessFile.close()
                    this.total *= (1 shl 10).toLong()
                    this.free *= (1 shl 10).toLong()
                    return
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun calSize(d: Double): String {
        val decimalFormat = DecimalFormat("#.##")
        val d2 = d / 1048576.0
        val d3 = d / 1.073741824E9
        val d4 = d / 1.099511627776E12
        if (d4 > 1.0) {
            return decimalFormat.format(d4) + " TB"
        }
        if (d3 > 1.0) {
            return decimalFormat.format(d3) + " GB"
        }
        return if (d2 > 1.0) {
            decimalFormat.format(d2) + " MB"
        } else decimalFormat.format(d) + " KB"
    }

    fun setProgressInAsync(
        percantage: TextView,
        progressBar: ProgressBar,
        progress: Int,
        justNow: Boolean
    ) {
        Thread {
            if (justNow) {
                activity.runOnUiThread {
                    percantage.text = progress.toString() + "MB"
                    progressBar.progress = progress
                }
            } else {
                var currentRange = 0
                while (currentRange < progress) {
                    currentRange++
                    val finalCurrentMomoryRange = currentRange
                    activity.runOnUiThread {
                        percantage.text = finalCurrentMomoryRange.toString() + "MB"
                        progressBar.progress = finalCurrentMomoryRange
                    }
                    try {
                        Thread.sleep(65)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }


    override fun onClick(v: View?) {

        if (v == binding.cardQrGenerator) {
            startActivity(
                Intent(
                    this,
                    ActivityQRcodeMaker::class.java
                )
            )
        } else if (v == binding.cardQrCodeReader) {
            if (!isPermissionGranted(this)) {
                takePermission(
                    this,
                    111
                )
            } else {
                startActivity(
                    Intent(
                        this,
                        ActivityQRcodeReader::class.java
                    )
                )
            }

        } else if (v == binding.pricacyPolicy) {
            val browserIntent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://sites.google.com/view/all-languages-translators")
                )
            startActivity(browserIntent)
        } else if (v == binding.cardShare) {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.setType("text/plain")
                shareIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    getResources().getString(R.string.app_name)
                )
                val shareMessage =
                    ("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID).toString() + "\n\n"
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun takePermission(activity: Activity?, request_code: Int) {
        ActivityCompat.requestPermissions(activity!!, arrayOf(permission.CAMERA), request_code)
    }

    fun isPermissionGranted(activity: Activity?): Boolean {
        val camera = ContextCompat.checkSelfPermission(activity!!, permission.CAMERA)
        return camera == PackageManager.PERMISSION_GRANTED
    }

}