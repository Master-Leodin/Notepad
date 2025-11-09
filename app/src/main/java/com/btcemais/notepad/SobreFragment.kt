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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class SobreFragment : Fragment() {

    // Referências para as views
    private lateinit var tvAppVersion: TextView
    private lateinit var tvAppDescription: TextView
    private lateinit var btnCopiarEmail: MaterialButton
    private lateinit var btnDoacaoWise: MaterialButton
    private lateinit var btnCopiarWise: MaterialButton
    private lateinit var btnDoacaoLightning: MaterialButton
    private lateinit var btnDoacaoBitcoin: MaterialButton
    private lateinit var btnCompartilhar: MaterialButton

    // Métodos de doação
    private val paypalPixEmail = "leonardo132@gmail.com"
    private val wiseId = "leonardot1427"
    private val wiseLink = "https://wise.com/pay/me/leonardot1427"
    private val lightningAddress = "mightynepal82@walletofsatoshi.com"
    private val bitcoinAddress = "bc1qjahmm3qtzpn9kc86j8uedejjuvtlp66z8w3wek"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla o layout diretamente sem View Binding
        return inflater.inflate(R.layout.fragment_sobre, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa as views manualmente
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
        tvAppVersion.text = "Versão 1.0"
        tvAppDescription.text = "Notepad - Seu aplicativo de bloco de notas simples"
    }

    private fun setupClickListeners() {
        // Botão para copiar email (PayPal e PIX)
        btnCopiarEmail.setOnClickListener {
            copiarParaAreaTransferencia(paypalPixEmail, "Email copiado para PayPal e PIX!")
        }

        // Botão de doação via Wise
        btnDoacaoWise.setOnClickListener {
            abrirLinkExterno(wiseLink)
        }

        // Botão para copiar ID Wise
        btnCopiarWise.setOnClickListener {
            copiarParaAreaTransferencia(wiseId, "ID Wise copiado!")
        }

        // Botão de doação via Lightning
        btnDoacaoLightning.setOnClickListener {
            copiarParaAreaTransferencia(lightningAddress, "Endereço Lightning copiado!")
        }

        // Botão de doação via Bitcoin (On Chain)
        btnDoacaoBitcoin.setOnClickListener {
            copiarParaAreaTransferencia(bitcoinAddress, "Endereço Bitcoin copiado!")
        }

        // Botão de compartilhar app
        btnCompartilhar.setOnClickListener {
            compartilharApp()
        }
    }

    private fun abrirLinkExterno(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Não foi possível abrir o link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copiarParaAreaTransferencia(texto: String, mensagemSucesso: String) {
        try {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Doação", texto)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(requireContext(), mensagemSucesso, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao copiar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartilharApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Notepad - App de Bloco de Notas")
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Baixe o Notepad - App de bloco de notas simples na página: https://leonportfolio.netlify.app/projects"
            )
            startActivity(Intent.createChooser(shareIntent, "Compartilhar app"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao compartilhar", Toast.LENGTH_SHORT).show()
        }
    }
}