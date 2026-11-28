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
    private Integer stockVendible;
    private Integer unidadesVencidas;
    private List<LoteEntity> lotes;

    public static InventarioItemDto from(ProductoEntity producto, List<LoteEntity> lotes) {
        int stock = lotes.stream()
                .mapToInt(l -> l.getStockActual() != null ? l.getStockActual() : 0)
                .sum();
        LocalDate hoy = LocalDate.now();
        int stockVendible = lotes.stream()
                .filter(l -> l.getFechaVencimiento() != null && !l.getFechaVencimiento().isBefore(hoy))
                .mapToInt(l -> l.getStockActual() != null ? l.getStockActual() : 0)
                .sum();

        int unidadesVencidas = lotes.stream()
                .filter(l -> l.getFechaVencimiento() != null && l.getFechaVencimiento().isBefore(hoy))
                .mapToInt(l -> l.getStockActual() != null ? l.getStockActual() : 0)
                .sum();

        int stockMinimo = producto.getStockMinimo() != null ? producto.getStockMinimo() : 5;

        String estadoStock;
        String estadoStockLabel;
        String badgeClass;

        // El estado se calcula con lo que se puede vender: el stock vencido no cuenta
        if (stockVendible == 0) {
            estadoStock = "AGOTADO";
            estadoStockLabel = "Agotado";
            badgeClass = "bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25";
        } else if (stockVendible <= stockMinimo) {
            estadoStock = "CRITICO";
            estadoStockLabel = "Stock Bajo";
            badgeClass = "bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25";
        } else {
            estadoStock = "NORMAL";
            estadoStockLabel = "En Stock";
            badgeClass = "bg-success bg-opacity-10 text-success border border-success border-opacity-25";
        }

        LocalDate proximoVenc = null;
        Long dias = null;
        boolean proximo = false;
        boolean venc = false;

        // Próximo vencimiento: solo entre lotes vendibles (con stock y NO vencidos). Un lote ya vencido
        // no hace que el producto figure como vencido si todavía tiene otros lotes vigentes.
        var loteMasCercano = lotes.stream()
                .filter(l -> l.getStockActual() != null && l.getStockActual() > 0
                        && l.getFechaVencimiento() != null && !l.getFechaVencimiento().isBefore(hoy))
                .min((a, b) -> a.getFechaVencimiento().compareTo(b.getFechaVencimiento()));

        if (loteMasCercano.isPresent()) {
            proximoVenc = loteMasCercano.get().getFechaVencimiento();
            dias = ChronoUnit.DAYS.between(hoy, proximoVenc);
            proximo = dias <= 45;
        }
        venc = stockVendible == 0 && unidadesVencidas > 0;

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
                .stockVendible(stockVendible)
                .unidadesVencidas(unidadesVencidas)
                .vencido(venc)
                .lotes(lotes)
                .build();
    }
}
