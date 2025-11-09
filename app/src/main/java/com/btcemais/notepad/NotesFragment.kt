package com.btcemais.notepad

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import java.io.File
import java.io.OutputStream

class NotesFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var btnAddNote: Button
    private val notesList = mutableListOf<String>()
    private lateinit var adapter: NoteAdapter

    // Variáveis para salvar externamente
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
            Toast.makeText(requireContext(), "Salvamento cancelado", Toast.LENGTH_SHORT).show()
        }
        // Limpa as variáveis temporárias
        currentNoteTitleForSave = null
        currentNoteContentForSave = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notes, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListView()
        loadNotes()
    }

    private fun initViews(view: View) {
        listView = view.findViewById(R.id.listViewNotes)
        btnAddNote = view.findViewById(R.id.btnAddNote)

        btnAddNote.setOnClickListener {
            val intent = Intent(requireContext(), NoteEditorActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupListView() {
        adapter = NoteAdapter(notesList)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val noteTitle = notesList[position]
            val intent = Intent(requireContext(), NoteEditorActivity::class.java).apply {
                putExtra("NOTE_TITLE", noteTitle)
            }
            startActivity(intent)
        }
    }

    private fun loadNotes() {
        notesList.clear()
        val files = requireContext().filesDir.listFiles()
        files?.filter { it.isFile && it.name.endsWith(".txt") }?.forEach { file ->
            val noteTitle = file.name.removeSuffix(".txt")
            notesList.add(noteTitle)
        }
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun showDeleteConfirmationDialog(noteTitle: String, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Nota")
            .setMessage("Tem certeza que deseja excluir \"$noteTitle\"?")
            .setPositiveButton("Excluir") { _, _ ->
                deleteNote(noteTitle, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSaveExternalConfirmationDialog(noteTitle: String) {
        AlertDialog.Builder(requireContext())
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
            val file = File(requireContext().filesDir, "$noteTitle.txt")
            if (file.exists()) {
                file.delete()
                notesList.removeAt(position)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Nota excluída com sucesso", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao excluir nota", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prepareNoteForExternalSave(noteTitle: String) {
        try {
            val internalFile = File(requireContext().filesDir, "$noteTitle.txt")
            if (!internalFile.exists()) {
                Toast.makeText(requireContext(), "Nota não encontrada", Toast.LENGTH_SHORT).show()
                return
            }

            val content = internalFile.readText()

            // Armazena os dados temporariamente
            currentNoteTitleForSave = noteTitle
            currentNoteContentForSave = content

            // Abre o seletor de arquivos do sistema
            openFilePicker()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao preparar nota para salvamento: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "${currentNoteTitleForSave}.txt")
        }
        createDocumentLauncher.launch(intent)
    }

    private fun saveNoteToUri(uri: Uri) {
        try {
            currentNoteContentForSave?.let { content ->
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    outputStream.write(content.toByteArray())
                    outputStream.flush()

                    Toast.makeText(
                        requireContext(),
                        "Nota \"${currentNoteTitleForSave}\" salva com sucesso!",
                        Toast.LENGTH_LONG
                    ).show()
                } ?: run {
                    Toast.makeText(requireContext(), "Erro: Não foi possível acessar o local selecionado", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), "Erro: Conteúdo da nota não encontrado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao salvar nota: ${e.message}", Toast.LENGTH_SHORT).show()
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