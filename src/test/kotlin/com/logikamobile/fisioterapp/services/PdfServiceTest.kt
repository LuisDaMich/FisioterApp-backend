package com.logikamobile.fisioterapp.services

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PdfServiceTest {

    private val pdfService = PdfService()

    @Test
    fun `should generate a valid PDF byte array`() {
        // 1. ARRANGE (Preparamos datos de prueba)
        val name = "Don Pepe Grillo"
        val date = "2026-02-04"
        val plan = "1. Compresas calientes 15 min.\n2. Estiramientos de cuello.\n3. Ultrasonido."

        // 2. ACT (Ejecutamos el generador)
        val pdfBytes = pdfService.generateSessionPdf(name, date, plan)

        // 3. ASSERT (Verificamos que funcionó)

        // Verificación A: Que no esté vacío
        assertTrue(pdfBytes.isNotEmpty(), "El array de bytes del PDF no debería estar vacío")

        // Verificación B: Validar la cabecera del archivo (Magic Bytes)
        // Todo archivo PDF válido comienza con los caracteres "%PDF"
        val header = String(pdfBytes.copyOfRange(0, 4))
        assertTrue(header.startsWith("%PDF"), "El archivo generado no es un PDF válido (Falta cabecera %PDF)")


        File("test_output.pdf").writeBytes(pdfBytes)
    }
}