package pl.sportfamily.app.data.xlsx

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/** Arkusz jako prostokątna tabela tekstów (puste komórki to ""). */
data class Sheet(val name: String, val rows: List<List<String>>)

/** Arkusz do zapisu: wiersze, numery wierszy pogrubionych (nagłówki) i szerokości kolumn. */
data class SheetOut(
    val name: String,
    val rows: List<List<String>>,
    val headerRows: Set<Int> = emptySet(),
    val titleRows: Set<Int> = emptySet(),
    val columnWidths: List<Double> = emptyList(),
)

/**
 * Minimalny czytnik i zapis plików .xlsx (Office Open XML) bez zewnętrznych bibliotek.
 * Obsługuje teksty współdzielone, teksty wpisane w komórkę, liczby oraz godziny zapisane jako ułamek doby.
 */
object Xlsx {

    fun read(input: InputStream): List<Sheet> {
        val files = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (!e.isDirectory && e.name.endsWith(".xml") || e.name.endsWith(".rels")) files[e.name.trimStart('/')] = zip.readBytes()
            }
        }
        val shared = files["xl/sharedStrings.xml"]?.let { parseShared(it) } ?: emptyList()
        val workbook = files["xl/workbook.xml"] ?: error("To nie jest plik Excel (.xlsx)")
        val rels = files["xl/_rels/workbook.xml.rels"]?.let { parseRels(it) } ?: emptyMap()
        val wb = dom(workbook)
        val sheets = wb.getElementsByTagNameNS("*", "sheet")
        val out = ArrayList<Sheet>()
        for (i in 0 until sheets.length) {
            val s = sheets.item(i) as Element
            val name = s.getAttribute("name")
            val rid = s.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                .ifEmpty { s.getAttribute("r:id") }
            val target = rels[rid]?.let { if (it.startsWith("/")) it.trimStart('/') else "xl/$it" } ?: "xl/worksheets/sheet${i + 1}.xml"
            val bytes = files[target] ?: continue
            out += Sheet(name, parseSheet(bytes, shared))
        }
        return out
    }

    private fun dom(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isExpandEntityReferences = false
        // Bez zewnętrznych encji: plik pochodzi od użytkownika. Parser Androida nie zna tej opcji
        // (i sam nie wczytuje zewnętrznych encji), więc ustawiamy ją tylko tam, gdzie jest obsługiwana.
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
    }.newDocumentBuilder().parse(ByteArrayInputStream(bytes)).documentElement

    private fun parseRels(bytes: ByteArray): Map<String, String> {
        val list = dom(bytes).getElementsByTagNameNS("*", "Relationship")
        return (0 until list.length).associate { (list.item(it) as Element).let { e -> e.getAttribute("Id") to e.getAttribute("Target") } }
    }

    private fun textOf(e: Element): String {
        val ts = e.getElementsByTagNameNS("*", "t")
        return (0 until ts.length).joinToString("") { ts.item(it).textContent }
    }

    private fun parseShared(bytes: ByteArray): List<String> {
        val si = dom(bytes).getElementsByTagNameNS("*", "si")
        return (0 until si.length).map { textOf(si.item(it) as Element) }
    }

    private fun colIndex(ref: String): Int {
        var n = 0
        for (ch in ref) {
            if (!ch.isLetter()) break
            n = n * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return n - 1
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val root = dom(bytes)
        val cells = root.getElementsByTagNameNS("*", "c")
        val map = HashMap<Pair<Int, Int>, String>()
        var maxRow = -1
        var maxCol = -1
        for (i in 0 until cells.length) {
            val c = cells.item(i) as Element
            val ref = c.getAttribute("r")
            val row = ref.dropWhile { it.isLetter() }.toIntOrNull()?.minus(1) ?: continue
            val col = colIndex(ref)
            val type = c.getAttribute("t")
            val v = c.getElementsByTagNameNS("*", "v").let { if (it.length > 0) it.item(0).textContent else null }
            val value = when (type) {
                "s" -> v?.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                "inlineStr" -> textOf(c)
                "str", "e" -> v ?: ""
                "b" -> if (v == "1") "PRAWDA" else "FAŁSZ"
                else -> v?.let { numberToText(it) } ?: ""
            }
            if (value.isBlank()) continue
            map[row to col] = value
            maxRow = maxOf(maxRow, row)
            maxCol = maxOf(maxCol, col)
        }
        return (0..maxRow).map { r -> (0..maxCol).map { c -> map[r to c] ?: "" } }
    }

    /** Godzina wpisana w Excelu jako czas to ułamek doby (0.34375 = 8:15). Liczby całkowite zostają liczbami. */
    private fun numberToText(v: String): String {
        val d = v.toDoubleOrNull() ?: return v
        if (d > 0 && d < 1) {
            val minutes = Math.round(d * 24 * 60).toInt()
            return "%d:%02d".format(minutes / 60, minutes % 60)
        }
        return if (d == Math.floor(d)) d.toLong().toString() else v
    }

    // ---------- Zapis ----------

    fun write(output: OutputStream, sheets: List<SheetOut>) {
        ZipOutputStream(output).use { zip ->
            fun put(name: String, text: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
                append("""<Default Extension="xml" ContentType="application/xml"/>""")
                append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
                append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
                sheets.indices.forEach {
                    append("""<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
                }
                append("</Types>")
            })
            put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            put("xl/workbook.xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
                val used = HashSet<String>()
                sheets.forEachIndexed { i, s ->
                    var n = safeSheetName(s.name).ifBlank { "Arkusz${i + 1}" }
                    var k = 2
                    while (!used.add(n.lowercase())) n = safeSheetName(s.name).take(28) + " " + k++
                    append("""<sheet name="${esc(n)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
                }
                append("</sheets></workbook>")
            })
            put("xl/_rels/workbook.xml.rels", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                sheets.indices.forEach {
                    append("""<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""")
                }
                append("""<Relationship Id="rId${sheets.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
                append("</Relationships>")
            })
            // Style: 0 zwykły (zawijanie, do góry), 1 nagłówek (pogrubiony, tło), 2 tytuł (pogrubiony, większy).
            put("xl/styles.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="3"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FF141B3D"/><name val="Calibri"/></font><font><b/><sz val="14"/><color rgb="FF141B3D"/><name val="Calibri"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFFFE3D1"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"><color rgb="FFD9D4CA"/></left><right style="thin"><color rgb="FFD9D4CA"/></right><top style="thin"><color rgb="FFD9D4CA"/></top><bottom style="thin"><color rgb="FFD9D4CA"/></bottom><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="4"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf><xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf></cellXfs></styleSheet>""")
            sheets.forEachIndexed { i, s -> put("xl/worksheets/sheet${i + 1}.xml", sheetXml(s)) }
        }
    }

    private fun sheetXml(s: SheetOut): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        if (s.columnWidths.isNotEmpty()) {
            append("<cols>")
            s.columnWidths.forEachIndexed { i, w -> append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
            append("</cols>")
        }
        append("<sheetData>")
        s.rows.forEachIndexed { r, row ->
            append("""<row r="${r + 1}">""")
            row.forEachIndexed { c, v ->
                val inTable = row.any { it.isNotBlank() } && r !in s.titleRows
                val style = when {
                    r in s.titleRows -> 2
                    r in s.headerRows -> 1
                    inTable && s.rows.size > 1 && hasTableAround(s, r) -> 3
                    else -> 0
                }
                if (v.isEmpty() && style == 0) return@forEachIndexed
                append("""<c r="${colName(c)}${r + 1}" t="inlineStr" s="$style"><is><t xml:space="preserve">${esc(v)}</t></is></c>""")
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    /** Ramki tylko w blokach tabeli, czyli w wierszach pod nagłówkiem aż do pustego wiersza. */
    private fun hasTableAround(s: SheetOut, r: Int): Boolean {
        var i = r
        while (i >= 0) {
            if (i in s.headerRows) return true
            if (s.rows[i].all { it.isBlank() } || i in s.titleRows) return false
            i--
        }
        return false
    }

    fun colName(i: Int): String {
        var n = i + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.insert(0, 'A' + rem)
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun safeSheetName(n: String) = n.replace(Regex("""[\[\]:*?/\\]"""), " ").trim().take(31)

    private fun esc(s: String) = buildString {
        for (ch in s) when (ch) {
            '&' -> append("&amp;"); '<' -> append("&lt;"); '>' -> append("&gt;"); '"' -> append("&quot;")
            else -> if (ch == '\n' || ch == '\t' || ch >= ' ') append(ch)
        }
    }
}
