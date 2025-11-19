package com.btcemais.notepad

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    // Variables for drag and drop
    private var draggedPosition = -1
    private var draggedView: View? = null

    // SharedPreferences for saving note order
    private lateinit var sharedPreferences: SharedPreferences
    private val NOTES_ORDER_KEY = "notes_order"

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

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("notepad_prefs", 0)

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

        // Setup drag and drop for list items
        setupDragAndDrop()
    }

    private fun setupDragAndDrop() {
        listView.setOnDragListener { view, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> {
                    // Drag started, we can change appearance if needed
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENTERED -> {
                    // Change appearance when drag enters the list
                    true
                }
                android.view.DragEvent.ACTION_DRAG_LOCATION -> {
                    // Handle drag location to show drop position
                    true
                }
                android.view.DragEvent.ACTION_DRAG_EXITED -> {
                    // Reset appearance when drag exits
                    true
                }
                android.view.DragEvent.ACTION_DROP -> {
                    // Handle the drop
                    val x = event.x
                    val y = event.y

                    // Find the position where the item was dropped
                    val dropPosition = listView.pointToPosition(x.toInt(), y.toInt())

                    if (dropPosition != AdapterView.INVALID_POSITION && draggedPosition != -1 && draggedPosition != dropPosition) {
                        // Rearrange the items
                        rearrangeNotes(draggedPosition, dropPosition)
                    }

                    // Reset dragged position
                    draggedPosition = -1
                    draggedView?.visibility = View.VISIBLE
                    draggedView = null

                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    // Reset any visual changes
                    draggedView?.visibility = View.VISIBLE
                    draggedView = null
                    draggedPosition = -1
                    true
                }
                else -> false
            }
        }
    }

    private fun rearrangeNotes(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || fromPosition >= notesList.size ||
            toPosition < 0 || toPosition >= notesList.size) {
            return
        }

        // Get the note that was moved
        val movedNote = notesList[fromPosition]

        // Remove from original position
        notesList.removeAt(fromPosition)

        // Insert at new position
        notesList.add(toPosition, movedNote)

        // Update the adapter
        adapter.notifyDataSetChanged()

        // Save the new order to SharedPreferences
        saveNotesOrder()
    }

    private fun saveNotesOrder() {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(notesList)
        editor.putString(NOTES_ORDER_KEY, json)
        editor.apply()
    }

    private fun loadSavedOrder(): List<String>? {
        val gson = Gson()
        val json = sharedPreferences.getString(NOTES_ORDER_KEY, null)
        if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            return gson.fromJson(json, type)
        }
        return null
    }

    private fun loadNotes() {
        notesList.clear()
        val files = requireContext().filesDir.listFiles()

        // Load saved order from SharedPreferences
        val savedOrder = loadSavedOrder()

        if (savedOrder != null) {
            // We have a saved order, use it
            val allNotes = files?.filter { it.isFile && it.name.endsWith(".txt") }?.map { it.name.removeSuffix(".txt") } ?: emptyList()

            // Create a map for quick lookup
            val noteSet = allNotes.toSet()

            // Filter saved order to only include existing notes
            val validOrder = savedOrder.filter { noteSet.contains(it) }

            // Add any new notes that aren't in the saved order
            val newNotes = allNotes.filter { !savedOrder.contains(it) }

            notesList.addAll(validOrder)
            notesList.addAll(newNotes)
        } else {
            // No saved order, load normally
            files?.filter { it.isFile && it.name.endsWith(".txt") }?.forEach { file ->
                val noteTitle = file.name.removeSuffix(".txt")
                notesList.add(noteTitle)
            }
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
                // Save the updated order
                saveNotesOrder()
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
            val btnMove = view.findViewById<ImageButton>(R.id.btnMove)

            tvNoteTitle.text = noteTitle

            btnDelete.setOnClickListener {
                showDeleteConfirmationDialog(noteTitle, position)
            }

            btnSaveExternal.setOnClickListener {
                showSaveExternalConfirmationDialog(noteTitle)
            }

            // Setup drag and drop for move button
            btnMove.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        // Start drag operation
                        draggedPosition = position
                        draggedView = view

                        val shadowBuilder = View.DragShadowBuilder(view)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            v.startDragAndDrop(null, shadowBuilder, null, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            v.startDrag(null, shadowBuilder, null, 0)
                        }

                        // Hide the original view during drag
                        view.visibility = View.INVISIBLE
                        true
                    }
                    else -> false
                }
            }

            return view
        }
    }
}