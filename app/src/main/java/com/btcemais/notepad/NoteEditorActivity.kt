package com.btcemais.notepad

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class NoteEditorActivity : AppCompatActivity() {
    private lateinit var etNoteName: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button
    private var existingNoteTitle: String? = null
    private var originalContent: String = ""
    private var originalTitle: String = ""
    private var hasChanges: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)

        println("DEBUG: NoteEditorActivity iniciada")
        println("DEBUG: Intent extras: ${intent.extras}")

        // Configura o comportamento do botão de voltar do sistema (nova abordagem)
        setupBackPressedCallback()

        initViews()
        loadExistingNote()
        setupTextWatchers()
    }

    private fun setupBackPressedCallback() {
        // Esta é a nova forma recomendada de lidar com o botão voltar
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                checkForUnsavedChanges()
            }
        })
    }

    private fun initViews() {
        etNoteName = findViewById(R.id.etNoteName)
        etNoteContent = findViewById(R.id.etNoteContent)
        btnSave = findViewById(R.id.btnSaveNote)
        btnBack = findViewById(R.id.btnBack)

        btnSave.setOnClickListener {
            saveNote()
        }

        btnBack.setOnClickListener {
            checkForUnsavedChanges()
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkIfContentChanged()
            }
        }

        etNoteName.addTextChangedListener(textWatcher)
        etNoteContent.addTextChangedListener(textWatcher)
    }

    private fun checkIfContentChanged() {
        val currentTitle = etNoteName.text.toString().trim()
        val currentContent = etNoteContent.text.toString()

        hasChanges = currentTitle != originalTitle || currentContent != originalContent
    }

    private fun loadExistingNote() {
        existingNoteTitle = intent.getStringExtra("NOTE_TITLE")
        println("DEBUG: existingNoteTitle = $existingNoteTitle")

        if (existingNoteTitle != null) {
            // Estamos editando uma nota existente
            println("DEBUG: Editando nota existente: $existingNoteTitle")
            etNoteName.setText(existingNoteTitle)

            val file = File(filesDir, "$existingNoteTitle.txt")
            println("DEBUG: Caminho do arquivo: ${file.absolutePath}")
            println("DEBUG: Arquivo existe: ${file.exists()}")

            if (file.exists()) {
                try {
                    val content = file.readText()
                    etNoteContent.setText(content)
                    // Guarda o conteúdo original para comparar depois
                    originalTitle = existingNoteTitle!!
                    originalContent = content
                    println("DEBUG: Conteúdo carregado com sucesso. Tamanho: ${content.length} caracteres")
                } catch (e: Exception) {
                    Toast.makeText(this, "Erro ao carregar a nota", Toast.LENGTH_SHORT).show()
                    println("DEBUG: Erro ao carregar conteúdo: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Arquivo da nota não encontrado", Toast.LENGTH_SHORT).show()
                println("DEBUG: Arquivo não existe")
            }
        } else {
            // Estamos criando uma nova nota
            println("DEBUG: Criando nova nota - nenhum título recebido")
            etNoteName.setText("")
            etNoteContent.setText("")
            // Para nova nota, originalTitle e originalContent são vazios
            originalTitle = ""
            originalContent = ""
            // Nova nota começa com changes = true pois precisa ser salva
            hasChanges = true
        }
    }

    private fun checkForUnsavedChanges() {
        if (hasChanges) {
            showUnsavedChangesDialog()
        } else {
            finish()
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Nota não salva")
            .setMessage("Você tem alterações não salvas. O que deseja fazer?")
            .setPositiveButton("Salvar e sair") { _, _ ->
                saveNoteAndExit()
            }
            .setNegativeButton("Sair sem salvar") { _, _ ->
                finish()
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun saveNoteAndExit() {
        val noteTitle = etNoteName.text.toString().trim()
        val content = etNoteContent.text.toString()

        if (noteTitle.isEmpty()) {
            Toast.makeText(this, "Digite um nome para a nota antes de salvar", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Se estiver editando uma nota existente com título diferente, deleta a antiga
            existingNoteTitle?.let { oldTitle ->
                if (oldTitle != noteTitle) {
                    val oldFile = File(filesDir, "$oldTitle.txt")
                    if (oldFile.exists()) {
                        oldFile.delete()
                        println("DEBUG: Arquivo antigo deletado: $oldTitle.txt")
                    }
                }
            }

            // Salva o arquivo com extensão .txt
            val file = File(filesDir, "$noteTitle.txt")
            file.writeText(content)

            Toast.makeText(this, "Nota salva com sucesso!", Toast.LENGTH_SHORT).show()
            println("DEBUG: Nota salva: $noteTitle.txt")
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
            println("DEBUG: Erro ao salvar: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun saveNote() {
        val noteTitle = etNoteName.text.toString().trim()
        val content = etNoteContent.text.toString()

        if (noteTitle.isEmpty()) {
            Toast.makeText(this, "Digite um nome para a nota", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Se estiver editando uma nota existente com título diferente, deleta a antiga
            existingNoteTitle?.let { oldTitle ->
                if (oldTitle != noteTitle) {
                    val oldFile = File(filesDir, "$oldTitle.txt")
                    if (oldFile.exists()) {
                        oldFile.delete()
                        println("DEBUG: Arquivo antigo deletado: $oldTitle.txt")
                    }
                }
            }

            // Salva o arquivo com extensão .txt
            val file = File(filesDir, "$noteTitle.txt")
            file.writeText(content)

            // Atualiza os originais para refletir o estado salvo
            originalTitle = noteTitle
            originalContent = content
            hasChanges = false

            Toast.makeText(this, "Nota salva com sucesso!", Toast.LENGTH_SHORT).show()
            println("DEBUG: Nota salva: $noteTitle.txt")
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
            println("DEBUG: Erro ao salvar: ${e.message}")
            e.printStackTrace()
        }
    }
}