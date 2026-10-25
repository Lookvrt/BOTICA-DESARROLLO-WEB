package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.enums.EstadoCompra;
import com.example.boticadesarrolloweb.model.InventarioItemDto;
import com.example.boticadesarrolloweb.repository.CompraRepository;
import com.example.boticadesarrolloweb.service.ProductoService;
import com.example.boticadesarrolloweb.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final CompraRepository compraRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<InventarioItemDto> inventario = productoService.listarInventario(null, null, null);

        List<InventarioItemDto> stockCriticoList = inventario.stream()
                .filter(i -> "CRITICO".equals(i.getEstadoStock()) || "AGOTADO".equals(i.getEstadoStock()))
                .toList();

        BigDecimal ventasHoy = ventaService.obtenerVentasHoy();

        YearMonth mesActual = YearMonth.now();
        LocalDateTime inicioMes = mesActual.atDay(1).atStartOfDay();
        LocalDateTime finMes = mesActual.atEndOfMonth().atTime(LocalTime.MAX);
        BigDecimal comprasMes = compraRepository.sumarTotalPorRangoFechas(EstadoCompra.REGISTRADA, inicioMes, finMes);

        // Obtener datos de los 3 gráficos
        Map<String, Object> graficoSemana = ventaService.obtenerVentasSemanalesGrafico();
        Map<String, Object> graficoHoy = ventaService.obtenerVentasHoyGrafico();
        Map<String, Object> graficoMes = ventaService.obtenerVentasMensualesGrafico();

        model.addAttribute("ventasHoy", ventasHoy);
        model.addAttribute("totalProductos", inventario.size());
        model.addAttribute("totalStockCritico", stockCriticoList.size());
        model.addAttribute("comprasMes", comprasMes);
        model.addAttribute("productosCriticos", stockCriticoList);

        // Inyectar datos de gráficos directamente al modelo HTML
        model.addAttribute("graficoSemanaLabels", graficoSemana.get("labels"));
        model.addAttribute("graficoSemanaData", graficoSemana.get("data"));

        model.addAttribute("graficoHoyLabels", graficoHoy.get("labels"));
        model.addAttribute("graficoHoyData", graficoHoy.get("data"));

        model.addAttribute("graficoMesLabels", graficoMes.get("labels"));
        model.addAttribute("graficoMesData", graficoMes.get("data"));

        return "dashboard";
    }
}