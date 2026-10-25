package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.VentaEntity;
import com.example.boticadesarrolloweb.repository.VentaRepository;

// Importaciones específicas de OpenPDF (Para PDF)
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

// Importaciones específicas de Apache POI (Para Excel)
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Importaciones específicas de Apache Commons CSV
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteExportService {

    private final VentaRepository ventaRepository;

    // --- 1. EXPORTAR A EXCEL (Apache POI) ---
    public ByteArrayInputStream generarExcelVentas() throws IOException {
        List<VentaEntity> ventas = ventaRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte de Ventas");

            // Estilo de Cabecera especificando org.apache.poi.ss.usermodel.Font para evitar conflictos
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Cabeceras de tabla Excel
            Row headerRow = sheet.createRow(0);
            String[] columnas = {"ID Venta", "Comprobante", "Número", "Método Pago", "Total", "Estado"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Filas de datos
            int rowIdx = 1;
            for (VentaEntity v : ventas) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(v.getIdVenta());
                row.createCell(1).setCellValue(v.getTipoComprobante() != null ? v.getTipoComprobante() : "-");
                row.createCell(2).setCellValue(v.getNumeroComprobante() != null ? v.getNumeroComprobante() : "-");
                row.createCell(3).setCellValue(v.getMetodoPago() != null ? v.getMetodoPago().name() : "-");
                row.createCell(4).setCellValue(v.getTotal() != null ? v.getTotal().doubleValue() : 0.0);
                row.createCell(5).setCellValue(v.getEstado() != null ? v.getEstado().name() : "-");
            }

            // Autoajustar ancho de columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // --- 2. EXPORTAR A PDF (OpenPDF) ---
    public ByteArrayInputStream generarPdfVentas() {
        List<VentaEntity> ventas = ventaRepository.findAll();
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fuente del Título especificando com.lowagie.text.Font para evitar conflictos
            com.lowagie.text.Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLUE);
            Paragraph title = new Paragraph("Reporte General de Ventas - Botica Ahorro y Salud", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // Espaciador

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            // Cabeceras de la Tabla PDF
            String[] headers = {"ID", "Comprobante", "Método Pago", "Total (S/)", "Estado"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Filas de datos
            for (VentaEntity v : ventas) {
                table.addCell(String.valueOf(v.getIdVenta()));
                table.addCell(v.getTipoComprobante() != null ? v.getTipoComprobante() : "-");
                table.addCell(v.getMetodoPago() != null ? v.getMetodoPago().name() : "-");
                table.addCell(v.getTotal() != null ? v.getTotal().toString() : "0.00");
                table.addCell(v.getEstado() != null ? v.getEstado().name() : "-");
            }

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // --- 3. EXPORTAR A CSV (Apache Commons CSV) ---
    public ByteArrayInputStream generarCsvVentas() throws IOException {
        List<VentaEntity> ventas = ventaRepository.findAll();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), CSVFormat.DEFAULT.builder().setHeader("ID", "Comprobante", "MetodoPago", "Total", "Estado").build())) {
            for (VentaEntity v : ventas) {
                csvPrinter.printRecord(
                        v.getIdVenta(),
                        v.getTipoComprobante(),
                        v.getMetodoPago() != null ? v.getMetodoPago().name() : "-",
                        v.getTotal(),
                        v.getEstado() != null ? v.getEstado().name() : "-"
                );
            }
            csvPrinter.flush();
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}