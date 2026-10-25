package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.VentaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final VentaService ventaService;

    public BigDecimal calcularPromedioVenta() {
        long nroVentas = ventaService.obtenerCantidadVentas();
        if (nroVentas == 0) return BigDecimal.ZERO;

        BigDecimal total = ventaService.obtenerTotalVentas();
        return total.divide(BigDecimal.valueOf(nroVentas), 2, RoundingMode.HALF_UP);
    }

    public List<VentaEntity> obtenerDetalleVentasRecientes() {
        return ventaService.obtenerUltimasVentas();
    }
}