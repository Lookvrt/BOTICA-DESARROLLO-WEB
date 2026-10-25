package com.example.boticadesarrolloweb.model;

import com.example.boticadesarrolloweb.entity.LoteEntity;
import com.example.boticadesarrolloweb.entity.ProductoEntity;
import lombok.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioItemDto {

    private ProductoEntity producto;
    private Integer stockTotal;
    private Integer cantidadLotes;
    private String estadoStock;        // NORMAL, CRITICO, AGOTADO
    private String estadoStockLabel;   // "En Stock", "Stock Crítico", "Agotado"
    private String badgeClass;
    private LocalDate fechaProximoVencimiento;
    private Long diasParaVencimiento;
    private boolean proximoAVencer;
    private boolean vencido;
    private List<LoteEntity> lotes;

    public static InventarioItemDto from(ProductoEntity producto, List<LoteEntity> lotes) {
        int stock = lotes.stream()
                .mapToInt(l -> l.getStockActual() != null ? l.getStockActual() : 0)
                .sum();

        int stockMinimo = producto.getStockMinimo() != null ? producto.getStockMinimo() : 5;

        String estadoStock;
        String estadoStockLabel;
        String badgeClass;

        if (stock == 0) {
            estadoStock = "AGOTADO";
            estadoStockLabel = "Agotado";
            badgeClass = "bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25";
        } else if (stock <= stockMinimo) {
            estadoStock = "CRITICO";
            estadoStockLabel = "Stock Bajo";
            badgeClass = "bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25";
        } else {
            estadoStock = "NORMAL";
            estadoStockLabel = "En Stock";
            badgeClass = "bg-success bg-opacity-10 text-success border border-success border-opacity-25";
        }

        LocalDate hoy = LocalDate.now();
        LocalDate proximoVenc = null;
        Long dias = null;
        boolean proximo = false;
        boolean venc = false;

        // Encontrar lote con stock activo y fecha de vencimiento más cercana
        var loteMasCercano = lotes.stream()
                .filter(l -> l.getStockActual() != null && l.getStockActual() > 0 && l.getFechaVencimiento() != null)
                .min((a, b) -> a.getFechaVencimiento().compareTo(b.getFechaVencimiento()));

        if (loteMasCercano.isPresent()) {
            proximoVenc = loteMasCercano.get().getFechaVencimiento();
            dias = ChronoUnit.DAYS.between(hoy, proximoVenc);
            if (dias < 0) {
                venc = true;
            } else if (dias <= 45) {
                proximo = true;
            }
        }

        return InventarioItemDto.builder()
                .producto(producto)
                .stockTotal(stock)
                .cantidadLotes(lotes.size())
                .estadoStock(estadoStock)
                .estadoStockLabel(estadoStockLabel)
                .badgeClass(badgeClass)
                .fechaProximoVencimiento(proximoVenc)
                .diasParaVencimiento(dias)
                .proximoAVencer(proximo)
                .vencido(venc)
                .lotes(lotes)
                .build();
    }
}
