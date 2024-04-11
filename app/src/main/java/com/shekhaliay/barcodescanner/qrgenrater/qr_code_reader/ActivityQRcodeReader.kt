package com.shekhaliay.barcodescanner.qrgenrater.qr_code_reader

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.shekhaliay.barcodescanner.qrgenrater.R
import com.shekhaliay.barcodescanner.qrgenrater.databinding.ActivityQrcodeReaderBinding

class ActivityQRcodeReader : AppCompatActivity(), ZXingScannerView.ResultHandler {
  private lateinit var binding: ActivityQrcodeReaderBinding
    var EXTRA_QUERY = "query"
    var TEXT_ENTRY = "text"
    var TAG = "QRReaderActivity"
    var barcode_result: String? = null
    protected var camera_id = -1
    private var selected_indices: ArrayList<Int>? = null
    var viewGroup: ViewGroup? = null
    var zXingScannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrcodeReaderBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        binding.toolbar.mToolBarThumb.setOnClickListener { finish() }
        binding.toolbar.mToolBarText.text = "QR Reader"
        init()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEnabled) {
                    isEnabled = false
                    finish()
                }
            }
        })
    }

    private fun init() {
        viewGroup = binding.flCamera
        zXingScannerView = ZXingScannerView(this)
        viewGroup!!.addView(zXingScannerView)
    }

    fun setupBarcodeFormats() {
        val arrayList = ArrayList<BarcodeFormat>()
        if (selected_indices == null || selected_indices!!.isEmpty()) {
            selected_indices = ArrayList()
            for (i in ZXingScannerView.ALL_FORMATS.indices) {
                selected_indices!!.add(i)
            }
        }
        for (selectedIndex in selected_indices!!) {
            arrayList.add(ZXingScannerView.ALL_FORMATS[selectedIndex])
        }
        if (zXingScannerView != null) {
            zXingScannerView!!.setFormats(arrayList)
        }
    }

    override fun handleResult(rawResult: Result?) {
        barcode_result = rawResult!!.text
        ToneGenerator(5, 100).startTone(24)
        val dialog = Dialog(this, R.style.ThemeWithRoundShape)
        dialog.requestWindowFeature(1)
        dialog.setContentView(R.layout.dialog_qr_out)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        dialog.window!!.setLayout(-1, -2)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        val tv_search = dialog.findViewById<TextView>(R.id.tv_search)
        val tv_result = dialog.findViewById<TextView>(R.id.tv_result)
        if (barcode_result!!.startsWith("tel")) {
            tv_search.text = "Call"
        }
        tv_result.text = barcode_result
        (dialog.findViewById<View>(R.id.tv_share) as TextView).setOnClickListener { view: View? ->
            val intent = Intent("android.intent.action.SEND")
            intent.setType("text/*")
            intent.putExtra("android.intent.extra.SUBJECT", "")
            intent.putExtra("android.intent.extra.TEXT", barcode_result)
            startActivity(Intent.createChooser(intent, "Share text using"))
            dialog.dismiss()
        }
        (dialog.findViewById<View>(R.id.tv_search) as TextView).setOnClickListener { view: View? ->
            val intent: Intent
            if (barcode_result!!.startsWith("tel")) {
                intent = Intent(Intent.ACTION_DIAL)
                intent.setData(Uri.parse(barcode_result))
            } else {
                intent = Intent("android.intent.action.WEB_SEARCH")
                intent.setClassName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.googlequicksearchbox.SearchActivity"
                )
                intent.putExtra(EXTRA_QUERY, barcode_result)
            }
            startActivity(intent)
            dialog.dismiss()
        }
        (dialog.findViewById<View>(R.id.iv_close) as ImageView).setOnClickListener { view: View? ->
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                ClipData.newPlainText(
                    TEXT_ENTRY,
                    barcode_result
                )
            )
            Toast.makeText(
                this@ActivityQRcodeReader,
                "Text copied to clipboard",
                Toast.LENGTH_SHORT
            ).show()
            if (zXingScannerView == null) {
                zXingScannerView = ZXingScannerView(this@ActivityQRcodeReader)
                viewGroup!!.addView(zXingScannerView)
            }
            zXingScannerView!!.setResultHandler(this@ActivityQRcodeReader)
            zXingScannerView!!.startCamera(camera_id)
            setupBarcodeFormats()
            dialog.dismiss()
        }
        dialog.show()
    }

    public override fun onResume() {
        if (zXingScannerView == null) {
            zXingScannerView = ZXingScannerView(this)
            viewGroup!!.addView(zXingScannerView)
        }
        zXingScannerView!!.setResultHandler(this)
        zXingScannerView!!.startCamera(camera_id)
        setupBarcodeFormats()
        super.onResume()
    }

    public override fun onDestroy() {
        zXingScannerView!!.stopCamera()
        super.onDestroy()
    }
}