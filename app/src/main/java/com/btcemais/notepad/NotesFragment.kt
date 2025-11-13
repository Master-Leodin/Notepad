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
import androidx.fragment.app.Fragment
import java.io.File
import java.io.OutputStream

class NotesFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var btnAddNote: Button
    private val notesList = mutableListOf<String>()
    private lateinit var adapter: NoteAdapter

    // Variables for external save
    private var currentNoteTitleForSave: String? = null
    private var currentNoteContentForSave: String? = null

    // Launcher for creating document using Storage Access Framework
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                saveNoteToUri(uri)
            }
        } else {
            Toast.makeText(requireContext(), R.string.save_cancelled, Toast.LENGTH_SHORT).show()
        }
        // Clear temporary variables
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
            .setTitle(R.string.delete_note)
            .setMessage(getString(R.string.delete_confirmation, noteTitle))
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteNote(noteTitle, position)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSaveExternalConfirmationDialog(noteTitle: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.save_to_device)
            .setMessage(getString(R.string.save_to_device_confirmation, noteTitle))
            .setPositiveButton(R.string.yes) { _, _ ->
                prepareNoteForExternalSave(noteTitle)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteNote(noteTitle: String, position: Int) {
        try {
            val file = File(requireContext().filesDir, "$noteTitle.txt")
            if (file.exists()) {
                file.delete()
                notesList.removeAt(position)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), R.string.note_deleted_successfully, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.error_deleting_note, Toast.LENGTH_SHORT).show()
        }
    }

    private fun prepareNoteForExternalSave(noteTitle: String) {
        try {
            val internalFile = File(requireContext().filesDir, "$noteTitle.txt")
            if (!internalFile.exists()) {
                Toast.makeText(requireContext(), R.string.note_not_found, Toast.LENGTH_SHORT).show()
                return
            }

            val content = internalFile.readText()

            // Temporarily store data
            currentNoteTitleForSave = noteTitle
            currentNoteContentForSave = content

            // Open system file picker
            openFilePicker()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.error_preparing_note, e.message), Toast.LENGTH_SHORT).show()
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
                        getString(R.string.note_saved_successfully),
                        Toast.LENGTH_LONG
                    ).show()
                } ?: run {
                    Toast.makeText(requireContext(), R.string.error_accessing_location, Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), R.string.error_note_content_not_found, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.error_saving_note, e.message), Toast.LENGTH_SHORT).show()
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