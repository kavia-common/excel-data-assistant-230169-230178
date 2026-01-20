package org.example.app.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.model.SheetModel

class SpreadsheetGridAdapter(
    private val context: Context,
    private var sheet: SheetModel,
    private val onCellUpdated: () -> Unit
) : RecyclerView.Adapter<SpreadsheetGridAdapter.CellVH>() {

    private val inflater = LayoutInflater.from(context)

    private var displayRows: Int = 0
    private var displayCols: Int = 0

    init {
        recalc()
    }

    fun setSheet(newSheet: SheetModel) {
        sheet = newSheet
        recalc()
        notifyDataSetChanged()
    }

    fun refresh() {
        recalc()
        notifyDataSetChanged()
    }

    private fun recalc() {
        // We display an extra header row/col:
        // - top-left is blank
        // - row 0 displays column labels (A, B, C...)
        // - col 0 displays row numbers (1, 2, 3...) with header row counted as 1 (Excel-ish)
        displayRows = sheet.rowCount() + 1
        displayCols = sheet.colCount() + 1
        if (displayRows < 2) displayRows = 2
        if (displayCols < 2) displayCols = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellVH {
        val view = inflater.inflate(R.layout.item_cell, parent, false)
        return CellVH(view)
    }

    override fun getItemCount(): Int = displayRows * displayCols

    override fun onBindViewHolder(holder: CellVH, position: Int) {
        val r = position / displayCols
        val c = position % displayCols

        val tv = holder.text
        tv.setOnClickListener(null)

        val isTopLeft = r == 0 && c == 0
        val isHeaderRow = r == 0 && c > 0
        val isHeaderCol = c == 0 && r > 0

        when {
            isTopLeft -> {
                tv.text = ""
                tv.setBackgroundResource(R.drawable.bg_cell_header)
            }
            isHeaderRow -> {
                tv.text = toColumnLabel(c)
                tv.setBackgroundResource(R.drawable.bg_cell_header)
            }
            isHeaderCol -> {
                tv.text = r.toString()
                tv.setBackgroundResource(R.drawable.bg_cell_header)
            }
            else -> {
                val value = sheet.getCell(r - 1, c - 1) ?: ""
                tv.text = value
                tv.setBackgroundResource(R.drawable.bg_cell)

                tv.setOnClickListener {
                    showEditDialog(row = r - 1, col = c - 1, currentValue = value)
                }
            }
        }
    }

    private fun showEditDialog(row: Int, col: Int, currentValue: String) {
        val dialogView = inflater.inflate(R.layout.dialog_edit_cell, null, false)
        val input = dialogView.findViewById<EditText>(R.id.editCellInput)
        input.setText(currentValue)

        val title = dialogView.findViewById<TextView>(R.id.editCellTitle)
        title.text = "Edit cell (${toColumnLabel(col + 1)}${row + 1})"

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                sheet.setCell(row, col, input.text?.toString())
                refresh()
                onCellUpdated()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toColumnLabel(colOneBased: Int): String {
        // 1 -> A, 26 -> Z, 27 -> AA
        var n = colOneBased
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    class CellVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.cellText)
    }
}
