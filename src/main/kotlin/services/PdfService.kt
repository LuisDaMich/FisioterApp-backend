package com.logikamobile.fisioterapp.services

import com.lowagie.text.*
import com.lowagie.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PdfService {

    fun generateSessionPdf(patientName: String, date: String, treatmentPlan: String): ByteArray {
        val document = Document()
        val out = ByteArrayOutputStream()

        try {
            PdfWriter.getInstance(document, out)
            document.open()
            val titleFont = Font(Font.HELVETICA, 18f, Font.BOLD)
            val subTitleFont = Font(Font.HELVETICA, 12f, Font.BOLD)
            val bodyFont = Font(Font.HELVETICA, 12f, Font.NORMAL)
            // Si tuvieras un logo real: Image.getInstance("ruta/logo.png")
            val title = Paragraph("FisioterApp - Clínica de Fisioterapia", titleFont)
            title.alignment = Element.ALIGN_CENTER
            document.add(title)
            document.add(Paragraph(" "))
            document.add(Paragraph("Paciente: $patientName", subTitleFont))
            document.add(Paragraph("Fecha de Sesión: $date", subTitleFont))
            val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy")
            val printDate = LocalDate.now().format(formatter)
            document.add(Paragraph("Fecha de Impresión: $printDate", Font(Font.HELVETICA, 10f, Font.ITALIC)))
            document.add(Paragraph("-----------------------------------------------------------------------------"))
            document.add(Paragraph(" "))
            val instructionsTitle = Paragraph("PLAN DE TRATAMIENTO / EJERCICIOS EN CASA", subTitleFont)
            instructionsTitle.alignment = Element.ALIGN_CENTER
            document.add(instructionsTitle)
            document.add(Paragraph(" "))
            val instructions = Paragraph(treatmentPlan, bodyFont)
            document.add(instructions)
            document.add(Paragraph(" "))
            document.add(Paragraph(" "))
            document.add(Paragraph("__________________________"))
            document.add(Paragraph("Firma del Fisioterapeuta", Font(Font.HELVETICA, 10f, Font.ITALIC)))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }
        return out.toByteArray()
    }
}