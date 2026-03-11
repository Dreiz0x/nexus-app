package com.nexus.intelligence.data.parser

import android.content.Context
import android.graphics.BitmapFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.googlecode.tesseract.android.TessBaseAPI
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class ParseResult(
    val text: String,
    val pageCount: Int = 0,
    val success: Boolean = true,
    val errorMessage: String? = null
)

@Singleton
class DocumentParser @Inject constructor(
    private val context: Context
) {
    private var tessBaseAPI: TessBaseAPI? = null
    private var pdfBoxInitialized = false

    fun initialize() {
        if (!pdfBoxInitialized) {
            PDFBoxResourceLoader.init(context)
            pdfBoxInitialized = true
        }
    }

    suspend fun parseFile(file: File): ParseResult {
        return try {
            when (getFileType(file)) {
                FileType.PDF -> parsePdf(file)
                FileType.DOC -> parseDoc(file)
                FileType.DOCX -> parseDocx(file)
                FileType.XLS -> parseXls(file)
                FileType.XLSX -> parseXlsx(file)
                FileType.PPT -> parsePpt(file)
                FileType.PPTX -> parsePptx(file)
                FileType.TXT -> parseTxt(file)
                FileType.CSV -> parseCsv(file)
                FileType.IMAGE -> parseImage(file)
                FileType.UNKNOWN -> ParseResult("", 0, false, "Unsupported file type")
            }
        } catch (e: Exception) {
            ParseResult("", 0, false, e.message ?: "Unknown parsing error")
        }
    }

    private fun parsePdf(file: File): ParseResult {
        initialize()
        return try {
            val document = PDDocument.load(file)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            val pages = document.numberOfPages
            document.close()
            ParseResult(text.trim(), pages)
        } catch (e: Exception) {
            ParseResult("", 0, false, "PDF parse error: ${e.message}")
        }
    }

    private fun parseDoc(file: File): ParseResult {
        // Formato .doc (Word 97-2003) no soportado en Android — usar .docx
        return ParseResult("", 0, false, "Formato .doc no soportado, convierte a .docx")
    }

    private fun parseDocx(file: File): ParseResult {
        return try {
            val fis = FileInputStream(file)
            val doc = XWPFDocument(fis)
            val sb = StringBuilder()
            for (paragraph in doc.paragraphs) sb.appendLine(paragraph.text)
            for (table in doc.tables) {
                for (row in table.rows) {
                    for (cell in row.tableCells) sb.append(cell.text).append("\t")
                    sb.appendLine()
                }
            }
            fis.close()
            ParseResult(sb.toString().trim(), 1)
        } catch (e: Exception) {
            ParseResult("", 0, false, "DOCX parse error: ${e.message}")
        }
    }

    private fun parseXls(file: File): ParseResult {
        return try {
            val fis = FileInputStream(file)
            val workbook = HSSFWorkbook(fis)
            val sb = StringBuilder()
            for (i in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(i)
                sb.appendLine("--- Sheet: ${sheet.sheetName} ---")
                for (row in sheet) {
                    for (cell in row) sb.append(cell.toString()).append("\t")
                    sb.appendLine()
                }
            }
            fis.close()
            ParseResult(sb.toString().trim(), workbook.numberOfSheets)
        } catch (e: Exception) {
            ParseResult("", 0, false, "XLS parse error: ${e.message}")
        }
    }

    private fun parseXlsx(file: File): ParseResult {
        return try {
            val fis = FileInputStream(file)
            val workbook = XSSFWorkbook(fis)
            val sb = StringBuilder()
            for (i in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(i)
                sb.appendLine("--- Sheet: ${sheet.sheetName} ---")
                for (row in sheet) {
                    for (cell in row) sb.append(cell.toString()).append("\t")
                    sb.appendLine()
                }
            }
            fis.close()
            ParseResult(sb.toString().trim(), workbook.numberOfSheets)
        } catch (e: Exception) {
            ParseResult("", 0, false, "XLSX parse error: ${e.message}")
        }
    }

    private fun parsePpt(file: File): ParseResult {
        // Formato .ppt (PowerPoint 97-2003) no soportado en Android — usar .pptx
        return ParseResult("", 0, false, "Formato .ppt no soportado, convierte a .pptx")
    }

    private fun parsePptx(file: File): ParseResult {
        return try {
            val fis = FileInputStream(file)
            val pptx = XMLSlideShow(fis)
            val sb = StringBuilder()
            var slideNum = 0
            for (slide in pptx.slides) {
                slideNum++
                sb.appendLine("--- Slide $slideNum ---")
                for (shape in slide.shapes) {
                    if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                        sb.appendLine(shape.text)
                    }
                }
            }
            fis.close()
            ParseResult(sb.toString().trim(), slideNum)
        } catch (e: Exception) {
            ParseResult("", 0, false, "PPTX parse error: ${e.message}")
        }
    }

    private fun parseTxt(file: File): ParseResult {
        return try {
            ParseResult(file.readText(Charsets.UTF_8).trim(), 1)
        } catch (e: Exception) {
            ParseResult("", 0, false, "TXT parse error: ${e.message}")
        }
    }

    private fun parseCsv(file: File): ParseResult {
        return try {
            val sb = StringBuilder()
            val reader = BufferedReader(InputStreamReader(FileInputStream(file)))
            var line: String?
            while (reader.readLine().also { line = it } != null) sb.appendLine(line)
            reader.close()
            ParseResult(sb.toString().trim(), 1)
        } catch (e: Exception) {
            ParseResult("", 0, false, "CSV parse error: ${e.message}")
        }
    }

    private fun parseImage(file: File): ParseResult {
        return try {
            if (tessBaseAPI == null) initTesseract()
            val api = tessBaseAPI ?: return ParseResult("", 0, false, "Tesseract not initialized")
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: return ParseResult("", 0, false, "Could not decode image")
            api.setImage(bitmap)
            val text = api.getUTF8Text() ?: ""
            bitmap.recycle()
            ParseResult(text.trim(), 1)
        } catch (e: Exception) {
            ParseResult("", 0, false, "OCR error: ${e.message}")
        }
    }

    private fun initTesseract() {
        val tessDataPath = File(context.filesDir, "tesseract")
        val tessDataDir = File(tessDataPath, "tessdata")
        if (!tessDataDir.exists()) tessDataDir.mkdirs()

        val engFile = File(tessDataDir, "eng.traineddata")
        if (!engFile.exists()) {
            try {
                context.assets.open("tessdata/eng.traineddata").use { input ->
                    engFile.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                return
            }
        }

        tessBaseAPI = TessBaseAPI()
        tessBaseAPI?.init(tessDataPath.absolutePath, "eng")
    }

    fun release() {
        tessBaseAPI?.recycle()
        tessBaseAPI = null
    }

    companion object {
        fun getFileType(file: File): FileType {
            return when (file.extension.lowercase()) {
                "pdf" -> FileType.PDF
                "doc" -> FileType.DOC
                "docx" -> FileType.DOCX
                "xls" -> FileType.XLS
                "xlsx" -> FileType.XLSX
                "ppt" -> FileType.PPT
                "pptx" -> FileType.PPTX
                "txt", "md", "log", "json", "xml", "html", "htm" -> FileType.TXT
                "csv" -> FileType.CSV
                "jpg", "jpeg", "png", "bmp", "tiff", "webp" -> FileType.IMAGE
                else -> FileType.UNKNOWN
            }
        }

        fun getDocumentTypeLabel(file: File): String {
            return when (file.extension.lowercase()) {
                "pdf" -> "PDF"
                "doc", "docx" -> "WORD"
                "xls", "xlsx" -> "EXCEL"
                "ppt", "pptx" -> "POWERPOINT"
                "jpg", "jpeg", "png", "bmp", "tiff", "webp" -> "IMAGE"
                "txt", "md", "log", "json", "xml", "html", "htm" -> "TEXT"
                "csv" -> "CSV"
                else -> "OTHER"
            }
        }

        val SUPPORTED_EXTENSIONS = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "log", "json", "xml", "html", "htm",
            "csv", "jpg", "jpeg", "png", "bmp", "tiff", "webp"
        )
    }
}

enum class FileType {
    PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT, CSV, IMAGE, UNKNOWN
}
