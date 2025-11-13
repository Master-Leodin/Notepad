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

        println("DEBUG: NoteEditorActivity started")
        println("DEBUG: Intent extras: ${intent.extras}")

        // Configure system back button behavior (new approach)
        setupBackPressedCallback()

        initViews()
        loadExistingNote()
        setupTextWatchers()
    }

    private fun setupBackPressedCallback() {
        // This is the new recommended way to handle the back button
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
            // We are editing an existing note
            println("DEBUG: Editing existing note: $existingNoteTitle")
            etNoteName.setText(existingNoteTitle)

            val file = File(filesDir, "$existingNoteTitle.txt")
            println("DEBUG: File path: ${file.absolutePath}")
            println("DEBUG: File exists: ${file.exists()}")

            if (file.exists()) {
                try {
                    val content = file.readText()
                    etNoteContent.setText(content)
                    // Store original content for later comparison
                    originalTitle = existingNoteTitle!!
                    originalContent = content
                    println("DEBUG: Content loaded successfully. Size: ${content.length} characters")
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.error_loading_note, Toast.LENGTH_SHORT).show()
                    println("DEBUG: Error loading content: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, R.string.note_file_not_found, Toast.LENGTH_SHORT).show()
                println("DEBUG: File doesn't exist")
            }
        } else {
            // We are creating a new note
            println("DEBUG: Creating new note - no title received")
            etNoteName.setText("")
            etNoteContent.setText("")
            // For new note, originalTitle and originalContent are empty
            originalTitle = ""
            originalContent = ""
            // New note starts with changes = true because it needs to be saved
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
            .setTitle(R.string.unsaved_note)
            .setMessage(R.string.unsaved_changes_message)
            .setPositiveButton(R.string.save_and_exit) { _, _ ->
                saveNoteAndExit()
            }
            .setNegativeButton(R.string.exit_without_saving) { _, _ ->
                finish()
            }
            .setNeutralButton(R.string.cancel, null)
            .show()
    }

    private fun saveNoteAndExit() {
        val noteTitle = etNoteName.text.toString().trim()
        val content = etNoteContent.text.toString()

        if (noteTitle.isEmpty()) {
            Toast.makeText(this, R.string.enter_note_name, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // If editing an existing note with different title, delete the old one
            existingNoteTitle?.let { oldTitle ->
                if (oldTitle != noteTitle) {
                    val oldFile = File(filesDir, "$oldTitle.txt")
                    if (oldFile.exists()) {
                        oldFile.delete()
                        println("DEBUG: Old file deleted: $oldTitle.txt")
                    }
                }
            }

            // Save file with .txt extension
            val file = File(filesDir, "$noteTitle.txt")
            file.writeText(content)

            Toast.makeText(this, R.string.note_saved_successfully, Toast.LENGTH_SHORT).show()
            println("DEBUG: Note saved: $noteTitle.txt")
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_saving, e.message), Toast.LENGTH_SHORT).show()
            println("DEBUG: Error saving: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun saveNote() {
        val noteTitle = etNoteName.text.toString().trim()
        val content = etNoteContent.text.toString()

        if (noteTitle.isEmpty()) {
            Toast.makeText(this, R.string.enter_note_name, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // If editing an existing note with different title, delete the old one
            existingNoteTitle?.let { oldTitle ->
                if (oldTitle != noteTitle) {
                    val oldFile = File(filesDir, "$oldTitle.txt")
                    if (oldFile.exists()) {
                        oldFile.delete()
                        println("DEBUG: Old file deleted: $oldTitle.txt")
                    }
                }
            }

            // Save file with .txt extension
            val file = File(filesDir, "$noteTitle.txt")
            file.writeText(content)

            // Update originals to reflect saved state
            originalTitle = noteTitle
            originalContent = content
            hasChanges = false

            Toast.makeText(this, R.string.note_saved_successfully, Toast.LENGTH_SHORT).show()
            println("DEBUG: Note saved: $noteTitle.txt")
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_saving, e.message), Toast.LENGTH_SHORT).show()
            println("DEBUG: Error saving: ${e.message}")
            e.printStackTrace()
        }
    }
}