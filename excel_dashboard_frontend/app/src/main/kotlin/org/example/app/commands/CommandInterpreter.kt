package org.example.app.commands

import org.example.app.model.SheetModel
import kotlin.math.roundToInt

/**
 * Lightweight command interpreter for the right-side prompt panel.
 *
 * Supported examples:
 * - "count rows"
 * - "stats column Age"
 * - "rename column Old to New"
 * - "merge columns First,Last into FullName"
 * - "filter where Status = Active"
 */
object CommandInterpreter {

    data class Result(
        val message: String,
        val didMutateSheet: Boolean
    )

    /**
     * PUBLIC_INTERFACE
     * Execute a user command against a sheet model.
     *
     * @param sheet Active sheet to query/modify.
     * @param command Raw user command.
     * @return Result including a human readable message and whether the sheet mutated.
     */
    fun execute(sheet: SheetModel, command: String): Result {
        val cmd = command.trim()
        if (cmd.isEmpty()) return Result("Empty command.", false)

        val lower = cmd.lowercase()

        return when {
            lower.startsWith("count rows") || lower == "count" || lower == "count row" ->
                Result("Rows (including header): ${sheet.rowCount()}", false)

            lower.startsWith("stats column ") -> {
                val colName = cmd.substringAfter("stats column ", "").trim()
                statsForColumn(sheet, colName)
            }

            lower.startsWith("rename column ") -> {
                // rename column Old to New
                val rest = cmd.substringAfter("rename column ", "")
                val parts = rest.split(" to ", ignoreCase = true, limit = 2)
                if (parts.size != 2) {
                    Result("Usage: rename column Old to New", false)
                } else {
                    renameColumn(sheet, parts[0].trim(), parts[1].trim())
                }
            }

            lower.startsWith("merge columns ") -> {
                // merge columns A,B into C
                val rest = cmd.substringAfter("merge columns ", "")
                val parts = rest.split(" into ", ignoreCase = true, limit = 2)
                if (parts.size != 2) {
                    Result("Usage: merge columns ColA,ColB into NewCol", false)
                } else {
                    val left = parts[0]
                    val newCol = parts[1].trim()
                    val cols = left.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (cols.size < 2) {
                        Result("Provide at least 2 columns: merge columns A,B into C", false)
                    } else {
                        mergeColumns(sheet, cols, newCol)
                    }
                }
            }

            lower.startsWith("filter where ") -> {
                // filter where Col = Value  (only "=" supported for now)
                val cond = cmd.substringAfter("filter where ", "").trim()
                filterWhere(sheet, cond)
            }

            else -> Result(
                "Unknown command. Try: count rows • stats column <Name> • rename column A to B • merge columns A,B into C • filter where A = X",
                false
            )
        }
    }

    private fun statsForColumn(sheet: SheetModel, colName: String): Result {
        val idx = sheet.findColumnIndexByName(colName)
            ?: return Result("Column '$colName' not found.", false)

        var countNonEmpty = 0
        val numericValues = mutableListOf<Double>()
        for (r in 1 until sheet.rowCount()) {
            val v = sheet.getCell(r, idx)?.trim()
            if (!v.isNullOrEmpty()) {
                countNonEmpty++
                v.toDoubleOrNull()?.let { numericValues.add(it) }
            }
        }

        val numericInfo = if (numericValues.isNotEmpty()) {
            val avg = numericValues.average()
            val min = numericValues.minOrNull()
            val max = numericValues.maxOrNull()
            " Numeric: n=${numericValues.size}, avg=${round2(avg)}, min=$min, max=$max"
        } else {
            ""
        }

        return Result("Stats for '$colName': non-empty=$countNonEmpty.$numericInfo", false)
    }

    private fun renameColumn(sheet: SheetModel, oldName: String, newName: String): Result {
        if (oldName.isBlank() || newName.isBlank()) return Result("Column names cannot be blank.", false)
        val idx = sheet.findColumnIndexByName(oldName) ?: return Result("Column '$oldName' not found.", false)
        sheet.setCell(0, idx, newName)
        return Result("Renamed column '$oldName' -> '$newName'.", true)
    }

    private fun mergeColumns(sheet: SheetModel, columnNames: List<String>, newColumnName: String): Result {
        val indices = columnNames.map { name ->
            sheet.findColumnIndexByName(name) ?: return Result("Column '$name' not found.", false)
        }

        val newIndex = sheet.colCount()
        sheet.setCell(0, newIndex, newColumnName)

        for (r in 1 until sheet.rowCount()) {
            val parts = indices.mapNotNull { idx -> sheet.getCell(r, idx)?.trim() }.filter { it.isNotEmpty() }
            sheet.setCell(r, newIndex, if (parts.isEmpty()) null else parts.joinToString(" "))
        }

        return Result("Merged ${columnNames.joinToString(", ")} into '$newColumnName'.", true)
    }

    private fun filterWhere(sheet: SheetModel, cond: String): Result {
        // Only supports "Col = Value" (value may contain spaces if quoted)
        val eqIdx = cond.indexOf("=")
        if (eqIdx <= 0) return Result("Usage: filter where Column = Value", false)

        val left = cond.substring(0, eqIdx).trim()
        var right = cond.substring(eqIdx + 1).trim()

        if (right.startsWith("\"") && right.endsWith("\"") && right.length >= 2) {
            right = right.substring(1, right.length - 1)
        }

        val colIdx = sheet.findColumnIndexByName(left) ?: return Result("Column '$left' not found.", false)

        val newRows = mutableListOf<MutableList<String?>>()
        // Keep header
        if (sheet.rowCount() > 0) newRows.add(sheet.rows[0].toMutableList())

        var kept = 0
        for (r in 1 until sheet.rowCount()) {
            val v = sheet.getCell(r, colIdx)?.trim() ?: ""
            if (v.equals(right, ignoreCase = true)) {
                newRows.add(sheet.rows[r].toMutableList())
                kept++
            }
        }

        sheet.rows.clear()
        sheet.rows.addAll(newRows)
        return Result("Filtered where '$left' = '$right'. Kept $kept rows.", true)
    }

    private fun round2(v: Double): Double = ((v * 100.0).roundToInt() / 100.0)
}
