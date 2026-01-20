package org.example.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.example.app.commands.CommandInterpreter
import org.example.app.excel.ExcelImporter
import org.example.app.model.InMemoryWorkbook
import org.example.app.model.SheetModel
import org.example.app.ui.SpreadsheetGridAdapter

class MainActivity : Activity() {

    private lateinit var btnPickFiles: Button
    private lateinit var uploadStatus: TextView
    private lateinit var sheetSpinner: Spinner
    private lateinit var recyclerGrid: RecyclerView
    private lateinit var promptLog: TextView
    private lateinit var commandInput: EditText
    private lateinit var btnRunCommand: Button

    private var workbook: InMemoryWorkbook? = null
    private var activeSheet: SheetModel? = null

    private var gridAdapter: SpreadsheetGridAdapter? = null

    private val REQUEST_OPEN_DOCUMENT = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupGrid()
        setupSheetSpinner()
        setupActions()

        setStatus("No files selected.")
    }

    private fun bindViews() {
        btnPickFiles = findViewById(R.id.btnPickFiles)
        uploadStatus = findViewById(R.id.uploadStatus)
        sheetSpinner = findViewById(R.id.sheetSpinner)
        recyclerGrid = findViewById(R.id.recyclerGrid)
        promptLog = findViewById(R.id.promptLog)
        commandInput = findViewById(R.id.commandInput)
        btnRunCommand = findViewById(R.id.btnRunCommand)
    }

    private fun setupGrid() {
        // A reasonable default column count; HorizontalScrollView allows more columns.
        val columnCount = 8
        recyclerGrid.layoutManager = GridLayoutManager(this, columnCount)

        val placeholderSheet = SheetModel(
            name = "Sheet1",
            rows = mutableListOf(mutableListOf("Import an .xlsx file to begin"))
        )

        gridAdapter = SpreadsheetGridAdapter(
            context = this,
            sheet = placeholderSheet,
            onCellUpdated = { /* no-op for now */ }
        )
        recyclerGrid.adapter = gridAdapter
    }

    private fun setupSheetSpinner() {
        // Start empty; will be populated after import.
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf<String>("—"))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sheetSpinner.adapter = adapter
        sheetSpinner.isEnabled = false
    }

    private fun setupActions() {
        btnPickFiles.setOnClickListener { openDocumentPicker() }

        btnRunCommand.setOnClickListener {
            val sheet = activeSheet
            if (sheet == null) {
                appendLog("No active sheet. Import a file first.")
                return@setOnClickListener
            }

            val cmd = commandInput.text?.toString() ?: ""
            commandInput.setText("")

            appendLog("> $cmd")
            val result = CommandInterpreter.execute(sheet, cmd)
            appendLog(result.message)

            if (result.didMutateSheet) {
                gridAdapter?.refresh()
            }
        }
    }

    private fun openDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            // Only .xlsx (OOXML spreadsheets)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, REQUEST_OPEN_DOCUMENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_OPEN_DOCUMENT || resultCode != RESULT_OK || data == null) return

        val uris = extractUris(data)
        if (uris.isEmpty()) {
            setStatus("No files selected.")
            return
        }

        // For this iteration, import the first selected workbook, but display count.
        setStatus("Selected ${uris.size} file(s). Importing…")
        appendLog("Selected ${uris.size} file(s).")

        try {
            val wb = ExcelImporter.importXlsx(contentResolver, uris[0])
            workbook = wb
            setStatus("Imported: ${uris[0].lastPathSegment ?: "workbook"}")

            populateSheets(wb)
            appendLog("Imported workbook with ${wb.sheets.size} sheet(s).")

        } catch (e: Exception) {
            setStatus("Import failed: ${e.message}")
            appendLog("Import failed: ${e.message}")
        }
    }

    private fun extractUris(data: Intent): List<Uri> {
        val result = mutableListOf<Uri>()
        val clip = data.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i).uri?.let { result.add(it) }
            }
        } else {
            data.data?.let { result.add(it) }
        }
        return result
    }

    private fun populateSheets(wb: InMemoryWorkbook) {
        val names = wb.sheetNames()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sheetSpinner.adapter = adapter
        sheetSpinner.isEnabled = true

        // Default to first sheet.
        setActiveSheet(wb.sheets.first())

        sheetSpinner.setSelection(0)
        sheetSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val selected = wb.sheets.getOrNull(position) ?: return
                setActiveSheet(selected)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // no-op
            }
        })
    }

    private fun setActiveSheet(sheet: SheetModel) {
        activeSheet = sheet
        gridAdapter?.setSheet(sheet)
        appendLog("Active sheet: ${sheet.name} (${sheet.rowCount()} rows, ${sheet.colCount()} cols)")
    }

    private fun setStatus(text: String) {
        uploadStatus.text = text
    }

    private fun appendLog(line: String) {
        val current = promptLog.text?.toString() ?: ""
        val next = if (current.isBlank()) line else "$current\n$line"
        promptLog.text = next
    }
}
