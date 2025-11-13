package com.btcemais.notepad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class SobreFragment : Fragment() {

    // View references
    private lateinit var tvAppVersion: TextView
    private lateinit var tvAppDescription: TextView
    private lateinit var btnCopiarEmail: MaterialButton
    private lateinit var btnDoacaoWise: MaterialButton
    private lateinit var btnCopiarWise: MaterialButton
    private lateinit var btnDoacaoLightning: MaterialButton
    private lateinit var btnDoacaoBitcoin: MaterialButton
    private lateinit var btnCompartilhar: MaterialButton

    // Donation methods
    private val paypalPixEmail = "leonardo132@gmail.com"
    private val wiseId = "leonardot1427"
    private val wiseLink = "https://wise.com/pay/me/leonardot1427"
    private val lightningAddress = "mightynepal82@walletofsatoshi.com"
    private val bitcoinAddress = "bc1qjahmm3qtzpn9kc86j8uedejjuvtlp66z8w3wek"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout directly without View Binding
        return inflater.inflate(R.layout.fragment_sobre, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views manually
        initViews(view)
        setupClickListeners()
        setupAppInfo()
    }

    private fun initViews(view: View) {
        tvAppVersion = view.findViewById(R.id.tvAppVersion)
        tvAppDescription = view.findViewById(R.id.tvAppDescription)
        btnCopiarEmail = view.findViewById(R.id.btnCopiarEmail)
        btnDoacaoWise = view.findViewById(R.id.btnDoacaoWise)
        btnCopiarWise = view.findViewById(R.id.btnCopiarWise)
        btnDoacaoLightning = view.findViewById(R.id.btnDoacaoLightning)
        btnDoacaoBitcoin = view.findViewById(R.id.btnDoacaoBitcoin)
        btnCompartilhar = view.findViewById(R.id.btnCompartilhar)
    }

    private fun setupAppInfo() {
        tvAppVersion.text = getString(R.string.app_version)
        tvAppDescription.text = getString(R.string.app_description)
    }

    private fun setupClickListeners() {
        // Button to copy email (PayPal and PIX)
        btnCopiarEmail.setOnClickListener {
            copyToClipboard(paypalPixEmail, getString(R.string.email_copied))
        }

        // Wise donation button
        btnDoacaoWise.setOnClickListener {
            openExternalLink(wiseLink)
        }

        // Button to copy Wise ID
        btnCopiarWise.setOnClickListener {
            copyToClipboard(wiseId, getString(R.string.wise_id_copied))
        }

        // Lightning donation button
        btnDoacaoLightning.setOnClickListener {
            copyToClipboard(lightningAddress, getString(R.string.lightning_address_copied))
        }

        // Bitcoin donation button (On Chain)
        btnDoacaoBitcoin.setOnClickListener {
            copyToClipboard(bitcoinAddress, getString(R.string.bitcoin_address_copied))
        }

        // Share app button
        btnCompartilhar.setOnClickListener {
            shareApp()
        }
    }

    private fun openExternalLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String, successMessage: String) {
        try {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Donation", text)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.copy_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text))
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.share_error, Toast.LENGTH_SHORT).show()
        }
    }
}