package org.example.app.model

/**
 * Simple in-memory representation of a workbook that supports:
 * - Multiple sheets
 * - Basic cell access/edit
 * - Header-aware column operations (rename/merge/filter/stats)
 *
 * This intentionally does not attempt to fully model Excel features (styles, formulas, etc.).
 */
class InMemoryWorkbook(
    val sheets: MutableList<SheetModel>
) {
    fun sheetNames(): List<String> = sheets.map { it.name }

    fun getSheetByName(name: String): SheetModel? = sheets.firstOrNull { it.name == name }
}

class SheetModel(
    val name: String,
    val rows: MutableList<MutableList<String?>> // row-major
) {
    fun rowCount(): Int = rows.size
    fun colCount(): Int = rows.maxOfOrNull { it.size } ?: 0

    fun ensureSize(targetRows: Int, targetCols: Int) {
        while (rows.size < targetRows) {
            rows.add(mutableListOf())
        }
        for (r in 0 until rows.size) {
            val row = rows[r]
            while (row.size < targetCols) {
                row.add(null)
            }
        }
    }

    fun getCell(row: Int, col: Int): String? {
        if (row < 0 || col < 0) return null
        if (row >= rows.size) return null
        val r = rows[row]
        if (col >= r.size) return null
        return r[col]
    }

    fun setCell(row: Int, col: Int, value: String?) {
        if (row < 0 || col < 0) return
        ensureSize(row + 1, col + 1)
        rows[row][col] = value
    }

    /**
     * Returns header row (row 0) as list of strings (nulls become empty strings).
     */
    fun header(): List<String> {
        if (rows.isEmpty()) return emptyList()
        val h = rows[0]
        return h.map { it ?: "" }
    }

    fun findColumnIndexByName(columnName: String): Int? {
        val trimmed = columnName.trim()
        if (trimmed.isEmpty()) return null
        val headers = header()
        val idx = headers.indexOfFirst { it.equals(trimmed, ignoreCase = true) }
        return if (idx >= 0) idx else null
    }
}
