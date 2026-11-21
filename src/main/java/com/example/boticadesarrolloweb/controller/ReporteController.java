package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.service.ReporteExportService;
import com.example.boticadesarrolloweb.service.ReporteService;
import com.example.boticadesarrolloweb.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReporteController {

    private final VentaService ventaService;
    private final ReporteService reporteService;
    private final ReporteExportService reporteExportService;

    @GetMapping("/reporte")
    public String reporte(Model model) {
        model.addAttribute("totalVentas", ventaService.obtenerTotalVentas());
        model.addAttribute("nroVentas", ventaService.obtenerCantidadVentas());
        model.addAttribute("promedioVenta", reporteService.calcularPromedioVenta());
        model.addAttribute("topProducto", ventaService.obtenerTopProducto());
        model.addAttribute("ventasRecientes", reporteService.obtenerDetalleVentasRecientes());
        return "reporte";
    }

    @GetMapping("/api/reportes/ventas-diarias")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerVentasDiariasApi(
            @RequestParam(name = "periodo", defaultValue = "semana") String periodo) {
        return ResponseEntity.ok(ventaService.obtenerReporteVentasPorPeriodo(periodo));
    }

    @GetMapping("/reporte/exportar/excel")
    public ResponseEntity<InputStreamResource> exportarExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(defaultValue = "REGISTRADA") String estado,
            @RequestParam(required = false) List<String> cols) throws IOException {

        ByteArrayInputStream stream = reporteExportService.generarExcelVentas(inicio, fin, estado, cols);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=reporte_ventas.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/reporte/exportar/pdf")
    public ResponseEntity<InputStreamResource> exportarPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(defaultValue = "REGISTRADA") String estado,
            @RequestParam(required = false) List<String> cols) {

        ByteArrayInputStream stream = reporteExportService.generarPdfVentas(inicio, fin, estado, cols);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=reporte_ventas.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/reporte/exportar/csv")
    public ResponseEntity<InputStreamResource> exportarCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(defaultValue = "REGISTRADA") String estado,
            @RequestParam(required = false) List<String> cols) throws IOException {

        ByteArrayInputStream stream = reporteExportService.generarCsvVentas(inicio, fin, estado, cols);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=reporte_ventas.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(stream));
    }
}