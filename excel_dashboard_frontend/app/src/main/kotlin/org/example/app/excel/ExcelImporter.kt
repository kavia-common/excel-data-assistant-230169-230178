package org.example.app.excel

import android.content.ContentResolver
import android.net.Uri
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.example.app.model.InMemoryWorkbook
import org.example.app.model.SheetModel
import java.io.InputStream

object ExcelImporter {

    /**
     * PUBLIC_INTERFACE
     * Import an .xlsx file from a content Uri into a simplified in-memory workbook.
     *
     * @param contentResolver Android ContentResolver used to open the Uri.
     * @param uri The Uri returned by ACTION_OPEN_DOCUMENT.
     * @return InMemoryWorkbook containing all sheets and their string-rendered cell values.
     * @throws IllegalArgumentException if the Uri cannot be opened or parsed as .xlsx.
     */
    fun importXlsx(contentResolver: ContentResolver, uri: Uri): InMemoryWorkbook {
        val input: InputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Unable to open file")

        input.use { stream ->
            val wb = XSSFWorkbook(stream)
            wb.use { workbook ->
                val formatter = DataFormatter()

                val sheets = mutableListOf<SheetModel>()
                for (s in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(s)
                    val lastRow = sheet.lastRowNum
                    val rows = mutableListOf<MutableList<String?>>()

                    // We'll read [0..lastRow], but also handle empty sheets.
                    val maxRow = if (lastRow < 0) 0 else lastRow
                    for (r in 0..maxRow) {
                        val row = sheet.getRow(r)
                        val outRow = mutableListOf<String?>()

                        val lastCellNum = row?.lastCellNum?.toInt() ?: 0
                        val maxCell = if (lastCellNum < 0) 0 else lastCellNum
                        for (c in 0 until maxCell) {
                            val cell: Cell? = row?.getCell(c)
                            val value = if (cell == null) null else formatter.formatCellValue(cell)
                            outRow.add(if (value.isNullOrBlank()) null else value)
                        }
                        rows.add(outRow)
                    }

                    sheets.add(SheetModel(name = sheet.sheetName, rows = rows))
                }

                if (sheets.isEmpty()) {
                    sheets.add(SheetModel(name = "Sheet1", rows = mutableListOf(mutableListOf())))
                }

                return InMemoryWorkbook(sheets = sheets)
            }
        }
    }
}
