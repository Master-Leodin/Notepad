package com.btcemais.notepad

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var btnAddNote: Button
    private val notesList = mutableListOf<String>()
    private lateinit var adapter: NoteAdapter

    // Variável para armazenar temporariamente a nota que queremos salvar
    private var currentNoteTitleForSave: String? = null
    private var currentNoteContentForSave: String? = null

    // Launcher para criar documento usando Storage Access Framework
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                saveNoteToUri(uri)
            }
        } else {
            Toast.makeText(this, "Salvamento cancelado", Toast.LENGTH_SHORT).show()
        }
        // Limpa as variáveis temporárias
        currentNoteTitleForSave = null
        currentNoteContentForSave = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListView()
        loadNotes()
    }

    private fun initViews() {
        listView = findViewById(R.id.listViewNotes)
        btnAddNote = findViewById(R.id.btnAddNote)

        btnAddNote.setOnClickListener {
            println("DEBUG: Botão Novo Bloco de Notas clicado")
            val intent = Intent(this, NoteEditorActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupListView() {
        adapter = NoteAdapter(notesList)
        listView.adapter = adapter

        listView.setOnItemClickListener { parent, view, position, id ->
            val noteTitle = notesList[position]
            println("DEBUG: Clicou na nota: $noteTitle - Posição: $position")

            val intent = Intent(this, NoteEditorActivity::class.java).apply {
                putExtra("NOTE_TITLE", noteTitle)
                println("DEBUG: Intent criado com extra: NOTE_TITLE = $noteTitle")
            }
            startActivity(intent)
        }
    }

    private fun loadNotes() {
        notesList.clear()
        val files = filesDir.listFiles()
        println("DEBUG: Número de arquivos: ${files?.size}")

        files?.filter { it.isFile && it.name.endsWith(".txt") }?.forEach { file ->
            val noteTitle = file.name.removeSuffix(".txt")
            notesList.add(noteTitle)
            println("DEBUG: Adicionando nota à lista: $noteTitle")
        }

        if (notesList.isEmpty()) {
            println("DEBUG: Nenhuma nota encontrada")
        } else {
            println("DEBUG: Total de notas na lista: ${notesList.size}")
        }

        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        println("DEBUG: MainActivity onResume - Recarregando notas")
        loadNotes()
    }

    private fun showDeleteConfirmationDialog(noteTitle: String, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Nota")
            .setMessage("Tem certeza que deseja excluir \"$noteTitle\"?")
            .setPositiveButton("Excluir") { _, _ ->
                deleteNote(noteTitle, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSaveExternalConfirmationDialog(noteTitle: String) {
        AlertDialog.Builder(this)
            .setTitle("Salvar no Dispositivo")
            .setMessage("Deseja salvar \"$noteTitle\" na pasta de documentos do dispositivo?")
            .setPositiveButton("Sim") { _, _ ->
                prepareNoteForExternalSave(noteTitle)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteNote(noteTitle: String, position: Int) {
        try {
            val file = File(filesDir, "$noteTitle.txt")
            if (file.exists()) {
                file.delete()
                notesList.removeAt(position)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Nota excluída com sucesso", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao excluir nota", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prepareNoteForExternalSave(noteTitle: String) {
        try {
            val internalFile = File(filesDir, "$noteTitle.txt")
            if (!internalFile.exists()) {
                Toast.makeText(this, "Nota não encontrada", Toast.LENGTH_SHORT).show()
                return
            }

            val content = internalFile.readText()

            // Armazena os dados temporariamente
            currentNoteTitleForSave = noteTitle
            currentNoteContentForSave = content

            // Abre o seletor de arquivos do sistema
            openFilePicker()

        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao preparar nota para salvamento: ${e.message}", Toast.LENGTH_SHORT).show()
            println("DEBUG: Erro ao preparar nota: ${e.message}")
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "${currentNoteTitleForSave}.txt")
            // Removemos a linha problemática com EXTRA_INITIAL_URI
        }
        createDocumentLauncher.launch(intent)
    }

    private fun saveNoteToUri(uri: Uri) {
        try {
            currentNoteContentForSave?.let { content ->
                contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    outputStream.write(content.toByteArray())
                    outputStream.flush()

                    Toast.makeText(
                        this,
                        "Nota \"${currentNoteTitleForSave}\" salva com sucesso!",
                        Toast.LENGTH_LONG
                    ).show()

                    println("DEBUG: Nota salva externamente via SAF: $uri")
                } ?: run {
                    Toast.makeText(this, "Erro: Não foi possível acessar o local selecionado", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "Erro: Conteúdo da nota não encontrado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar nota: ${e.message}", Toast.LENGTH_SHORT).show()
            println("DEBUG: Erro ao salvar via SAF: ${e.message}")
            e.printStackTrace()
        }
    }

    inner class NoteAdapter(private val notes: List<String>) : BaseAdapter() {
        override fun getCount(): Int = notes.size
        override fun getItem(position: Int): String = notes[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_note, parent, false)

            val noteTitle = notes[position]

            val tvNoteTitle = view.findViewById<TextView>(R.id.tvNoteTitle)
            val btnDelete = view.findViewById<ImageButton>(R.id.btnDelete)
            val btnSaveExternal = view.findViewById<ImageButton>(R.id.btnSaveExternal)

            tvNoteTitle.text = noteTitle

            btnDelete.setOnClickListener {
                showDeleteConfirmationDialog(noteTitle, position)
            }

            btnSaveExternal.setOnClickListener {
                showSaveExternalConfirmationDialog(noteTitle)
            }

            return view
        }
    }
}