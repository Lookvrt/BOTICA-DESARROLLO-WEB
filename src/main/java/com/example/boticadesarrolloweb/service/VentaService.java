package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.VentaEntity;
import com.example.boticadesarrolloweb.enums.EstadoVenta;
import com.example.boticadesarrolloweb.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;

    @Transactional(readOnly = true)
    public BigDecimal obtenerVentasHoy() {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);
        return ventaRepository.sumarTotalPorRangoFechas(EstadoVenta.REGISTRADA, inicioHoy, finHoy);
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerTotalVentas() {
        return ventaRepository.sumarTotalVentas(EstadoVenta.REGISTRADA);
    }

    @Transactional(readOnly = true)
    public long obtenerCantidadVentas() {
        return ventaRepository.countByEstado(EstadoVenta.REGISTRADA);
    }

    @Transactional(readOnly = true)
    public List<VentaEntity> obtenerUltimasVentas() {
        return ventaRepository.findTop10ByEstadoOrderByFechaVentaDesc(EstadoVenta.REGISTRADA);
    }

    @Transactional(readOnly = true)
    public String obtenerTopProducto() {
        String top = ventaRepository.obtenerProductoMasVendido();
        return top != null ? top : "Sin ventas";
    }

    // --- Método genérico para el ReporteController (Filtros: hoy, semana, mes) ---
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerReporteVentasPorPeriodo(String periodo) {
        if ("hoy".equalsIgnoreCase(periodo)) {
            return obtenerVentasHoyGrafico();
        } else if ("mes".equalsIgnoreCase(periodo)) {
            return obtenerVentasMensualesGrafico();
        } else {
            return obtenerVentasSemanalesGrafico();
        }
    }

    // --- Método de compatibilidad previo ---
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerReporteVentasSemanales() {
        return obtenerVentasSemanalesGrafico();
    }

    // --- Gráfico 1: Ventas de Hoy por Hora (08:00 a 22:00) ---
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerVentasHoyGrafico() {
        Map<String, BigDecimal> mapaHoras = new LinkedHashMap<>();
        for (int h = 8; h <= 22; h += 2) {
            mapaHoras.put(String.format("%02d:00", h), BigDecimal.ZERO);
        }

        List<Object[]> resultados = ventaRepository.obtenerVentasHoyPorHora();
        for (Object[] fila : resultados) {
            if (fila[0] != null && fila[1] != null) {
                int hora = Integer.parseInt(fila[0].toString());
                int rangoHora = (hora / 2) * 2;
                if (rangoHora < 8) rangoHora = 8;
                if (rangoHora > 22) rangoHora = 22;

                String clave = String.format("%02d:00", rangoHora);
                BigDecimal actual = mapaHoras.getOrDefault(clave, BigDecimal.ZERO);
                mapaHoras.put(clave, actual.add(new BigDecimal(fila[1].toString())));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", new ArrayList<>(mapaHoras.keySet()));
        response.put("data", new ArrayList<>(mapaHoras.values()));
        return response;
    }

    // --- Gráfico 2: Ventas Semanales (Últimos 7 días) ---
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerVentasSemanalesGrafico() {
        Map<String, BigDecimal> mapaDias = new LinkedHashMap<>();
        LocalDateTime desde = LocalDate.now().minusDays(6).atStartOfDay();

        for (int i = 6; i >= 0; i--) {
            LocalDate fecha = LocalDate.now().minusDays(i);
            mapaDias.put(fecha.toString(), BigDecimal.ZERO);
        }

        List<Object[]> resultados = ventaRepository.obtenerVentasDiariasDesde(desde);
        for (Object[] fila : resultados) {
            if (fila[0] != null && fila[1] != null) {
                mapaDias.put(fila[0].toString(), new BigDecimal(fila[1].toString()));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", new ArrayList<>(mapaDias.keySet()));
        response.put("data", new ArrayList<>(mapaDias.values()));
        return response;
    }

    // --- Gráfico 3: Ventas Mensuales (Últimos 30 días) ---
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerVentasMensualesGrafico() {
        Map<String, BigDecimal> mapaMes = new LinkedHashMap<>();
        LocalDateTime desde = LocalDate.now().minusDays(29).atStartOfDay();

        for (int i = 29; i >= 0; i--) {
            LocalDate fecha = LocalDate.now().minusDays(i);
            mapaMes.put(fecha.toString().substring(5), BigDecimal.ZERO); // Formato MM-DD
        }

        List<Object[]> resultados = ventaRepository.obtenerVentasDiariasDesde(desde);
        for (Object[] fila : resultados) {
            if (fila[0] != null && fila[1] != null) {
                String clave = fila[0].toString().substring(5);
                mapaMes.put(clave, new BigDecimal(fila[1].toString()));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labels", new ArrayList<>(mapaMes.keySet()));
        response.put("data", new ArrayList<>(mapaMes.values()));
        return response;
    }
}