package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.VentaEntity;
import com.example.boticadesarrolloweb.enums.EstadoVenta;
import com.example.boticadesarrolloweb.repository.VentaRepository;
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
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteExportService {

    private final VentaRepository ventaRepository;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Catálogo ordenado de columnas disponibles
    private static final Map<String, String> CATALOGO_COLUMNAS = new LinkedHashMap<>() {{
        put("id", "ID Venta");
        put("fecha", "Fecha y Hora");
        put("comprobante", "Comprobante");
        put("metodoPago", "Método de Pago");
        put("vendedor", "Vendedor");
        put("total", "Total (S/)");
        put("estado", "Estado");
    }};

    private List<String> resolverColumnas(List<String> columnasSolicitadas) {
        if (columnasSolicitadas == null || columnasSolicitadas.isEmpty()) {
            return List.of("id", "fecha", "comprobante", "metodoPago", "total", "estado");
        }
        List<String> validas = CATALOGO_COLUMNAS.keySet().stream()
                .filter(columnasSolicitadas::contains)
                .toList();
        return validas.isEmpty() ? List.of("id", "fecha", "comprobante", "total") : validas;
    }

    private List<VentaEntity> consultarVentasFiltradas(LocalDate inicio, LocalDate fin, String estadoStr) {
        EstadoVenta estadoFiltro = null;
        if (estadoStr != null && !estadoStr.equalsIgnoreCase("TODAS")) {
            try {
                estadoFiltro = EstadoVenta.valueOf(estadoStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                estadoFiltro = EstadoVenta.REGISTRADA;
            }
        }
        LocalDateTime fechaInicio = (inicio != null) ? inicio.atStartOfDay() : null;
        LocalDateTime fechaFin = (fin != null) ? fin.atTime(LocalTime.MAX) : null;

        return ventaRepository.buscarVentasParaReporte(estadoFiltro, fechaInicio, fechaFin);
    }

    private String obtenerValorTexto(VentaEntity v, String claveColumna) {
        return switch (claveColumna) {
            case "id" -> String.valueOf(v.getIdVenta());
            case "fecha" -> v.getFechaVenta() != null ? v.getFechaVenta().format(FORMATO_FECHA) : "-";
            case "comprobante" -> {
                String tipo = v.getTipoComprobante() != null ? v.getTipoComprobante() : "";
                String num = v.getNumeroComprobante() != null ? v.getNumeroComprobante() : "";
                String combinado = (tipo + " " + num).trim();
                yield combinado.isEmpty() ? "-" : combinado;
            }
            case "metodoPago" -> v.getMetodoPago() != null ? v.getMetodoPago().name() : "-";
            case "vendedor" -> v.getUsuario() != null
                    ? (v.getUsuario().getNombres() + " " + v.getUsuario().getApellidos()).trim()
                    : "-";
            case "total" -> v.getTotal() != null ? String.format(Locale.US, "%.2f", v.getTotal()) : "0.00";
            case "estado" -> v.getEstado() != null ? v.getEstado().name() : "-";
            default -> "-";
        };
    }

    private BigDecimal calcularTotalRegistradas(List<VentaEntity> ventas) {
        return ventas.stream()
                .filter(v -> v.getEstado() == EstadoVenta.REGISTRADA && v.getTotal() != null)
                .map(VentaEntity::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // --- 1. EXPORTAR A EXCEL (Apache POI) ---
    @Transactional(readOnly = true)
    public ByteArrayInputStream generarExcelVentas(LocalDate inicio, LocalDate fin, String estado, List<String> cols) throws IOException {
        List<VentaEntity> ventas = consultarVentasFiltradas(inicio, fin, estado);
        List<String> columnasActivas = resolverColumnas(cols);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte de Ventas");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Cabeceras dinámicas
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnasActivas.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(CATALOGO_COLUMNAS.get(columnasActivas.get(i)));
                cell.setCellStyle(headerStyle);
            }

            // Filas de datos
            int rowIdx = 1;
            for (VentaEntity v : ventas) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < columnasActivas.size(); i++) {
                    String colKey = columnasActivas.get(i);
                    Cell cell = row.createCell(i);
                    if ("total".equals(colKey)) {
                        cell.setCellValue(v.getTotal() != null ? v.getTotal().doubleValue() : 0.0);
                    } else {
                        cell.setCellValue(obtenerValorTexto(v, colKey));
                    }
                }
            }

            // Fila de total general si se incluyó la columna "total"
            int idxTotal = columnasActivas.indexOf("total");
            if (idxTotal != -1) {
                Row totalRow = sheet.createRow(rowIdx);
                Cell labelCell = totalRow.createCell(Math.max(0, idxTotal - 1));
                labelCell.setCellValue("TOTAL REGISTRADAS:");
                labelCell.setCellStyle(headerStyle);

                Cell valCell = totalRow.createCell(idxTotal);
                valCell.setCellValue(calcularTotalRegistradas(ventas).doubleValue());
                valCell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < columnasActivas.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // --- 2. EXPORTAR A PDF (OpenPDF) ---
    @Transactional(readOnly = true)
    public ByteArrayInputStream generarPdfVentas(LocalDate inicio, LocalDate fin, String estado, List<String> cols) {
        List<VentaEntity> ventas = consultarVentasFiltradas(inicio, fin, estado);
        List<String> columnasActivas = resolverColumnas(cols);

        Document document = new Document(columnasActivas.size() > 5 ? PageSize.A4.rotate() : PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(25, 135, 84));
            Paragraph title = new Paragraph("Reporte de Ventas - Botica Ahorro y Salud", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            com.lowagie.text.Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            String rangoTexto = (inicio != null || fin != null)
                    ? "Periodo: " + (inicio != null ? inicio : "Inicio") + " al " + (fin != null ? fin : "Actualidad")
                    : "Periodo: Historial completo";
            Paragraph sub = new Paragraph(rangoTexto + " | Filtro estado: " + estado, fontSub);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(12f);
            document.add(sub);

            PdfPTable table = new PdfPTable(columnasActivas.size());
            table.setWidthPercentage(100);

            com.lowagie.text.Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            for (String colKey : columnasActivas) {
                PdfPCell cell = new PdfPCell(new Phrase(CATALOGO_COLUMNAS.get(colKey), fontHeader));
                cell.setBackgroundColor(new Color(33, 37, 41));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6f);
                table.addCell(cell);
            }

            com.lowagie.text.Font fontCell = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            for (VentaEntity v : ventas) {
                for (String colKey : columnasActivas) {
                    PdfPCell cell = new PdfPCell(new Phrase(obtenerValorTexto(v, colKey), fontCell));
                    cell.setPadding(5f);
                    if ("total".equals(colKey)) {
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    table.addCell(cell);
                }
            }

            document.add(table);

            if (columnasActivas.contains("total")) {
                com.lowagie.text.Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
                Paragraph totalPar = new Paragraph(
                        "Total Ventas Registradas: S/ " + String.format(Locale.US, "%.2f", calcularTotalRegistradas(ventas)),
                        fontTotal
                );
                totalPar.setAlignment(Element.ALIGN_RIGHT);
                totalPar.setSpacingBefore(10f);
                document.add(totalPar);
            }

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // --- 3. EXPORTAR A CSV (Apache Commons CSV) ---
    @Transactional(readOnly = true)
    public ByteArrayInputStream generarCsvVentas(LocalDate inicio, LocalDate fin, String estado, List<String> cols) throws IOException {
        List<VentaEntity> ventas = consultarVentasFiltradas(inicio, fin, estado);
        List<String> columnasActivas = resolverColumnas(cols);

        String[] headers = columnasActivas.stream()
                .map(CATALOGO_COLUMNAS::get)
                .toArray(String[]::new);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter csvPrinter = new CSVPrinter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

            for (VentaEntity v : ventas) {
                List<String> fila = columnasActivas.stream()
                        .map(colKey -> obtenerValorTexto(v, colKey))
                        .toList();
                csvPrinter.printRecord(fila);
            }
            csvPrinter.flush();
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}