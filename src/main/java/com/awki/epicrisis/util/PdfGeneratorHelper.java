package com.awki.epicrisis.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.awt.Color;

public class PdfGeneratorHelper {

    public static byte[] generarPdfEpicrisis(
            String pacienteNombre,
            String semanasGestacion,
            String motivoDerivacion,
            String observaciones,
            String resumenIa,
            String sintesisClinica,
            String conclusiones
    ) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Font styles
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.DARK_GRAY);
            Font sectionFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
            Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
            Font valueFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);

            // Title
            Paragraph title = new Paragraph("INFORME DE DERIVACIÓN - EPICRISIS DE GESTACIÓN", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Paciente Info Table
            Table table = new Table(2);
            table.setWidth(100f);
            table.setBorderWidth(1);
            table.setBorderColor(Color.LIGHT_GRAY);
            table.setPadding(5);

            table.addCell(new Cell(new Paragraph("Paciente:", labelFont)));
            table.addCell(new Cell(new Paragraph(pacienteNombre, valueFont)));

            table.addCell(new Cell(new Paragraph("Semanas de Gestación:", labelFont)));
            table.addCell(new Cell(new Paragraph(semanasGestacion, valueFont)));

            table.addCell(new Cell(new Paragraph("Motivo de Derivación:", labelFont)));
            table.addCell(new Cell(new Paragraph(motivoDerivacion, valueFont)));

            if (observaciones != null && !observaciones.isBlank()) {
                table.addCell(new Cell(new Paragraph("Observaciones Adicionales:", labelFont)));
                table.addCell(new Cell(new Paragraph(observaciones, valueFont)));
            }

            document.add(table);

            // Spacing
            Paragraph spacing = new Paragraph(" ");
            spacing.setSpacingAfter(15);
            document.add(spacing);

            // Resumen de IA Section
            document.add(new Paragraph("Resumen Clínico Inteligente (IA)", sectionFont));
            Paragraph pResumen = new Paragraph(resumenIa != null ? resumenIa : "No disponible.", valueFont);
            pResumen.setSpacingAfter(15);
            document.add(pResumen);

            // Sintesis Section
            document.add(new Paragraph("Síntesis de Controles y Síntomas", sectionFont));
            Paragraph pSintesis = new Paragraph(sintesisClinica != null ? sintesisClinica : "No disponible.", valueFont);
            pSintesis.setSpacingAfter(15);
            document.add(pSintesis);

            // Conclusiones Section
            document.add(new Paragraph("Conclusiones y Recomendaciones de Derivación", sectionFont));
            Paragraph pConclusiones = new Paragraph(conclusiones != null ? conclusiones : "No disponible.", valueFont);
            pConclusiones.setSpacingAfter(40);
            document.add(pConclusiones);

            // Signatures space
            Paragraph signatures = new Paragraph("___________________________\nFirma y Sello del Médico Obstetra", labelFont);
            signatures.setAlignment(Element.ALIGN_RIGHT);
            document.add(signatures);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de Epicrisis", e);
        }
    }
}
