package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.LoteEntity;
import com.example.boticadesarrolloweb.repository.LoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteRepository loteRepository;

    public List<LoteEntity> listarPorProducto(Integer idProducto) {
        return loteRepository.findByProductoIdProductoOrderByFechaVencimientoAsc(idProducto);
    }

    public List<LoteEntity> listarDisponiblesPorProducto(Integer idProducto) {
        return loteRepository.findLotesDisponiblesByProductoId(idProducto);
    }

    public Integer calcularStockTotal(Integer idProducto) {
        return loteRepository.sumStockByProductoId(idProducto);
    }

    public Optional<LoteEntity> buscarPorId(Integer idLote) {
        return loteRepository.findById(idLote);
    }

    public boolean existeLoteEnProducto(Integer idProducto, String numeroLote) {
        return loteRepository.existsByProductoIdProductoAndNumeroLoteIgnoreCase(idProducto, numeroLote);
    }

    public LoteEntity guardar(LoteEntity lote) {
        if (lote.getStockActual() == null) {
            lote.setStockActual(lote.getCantidadInicial() != null ? lote.getCantidadInicial() : 0);
        }
        return loteRepository.save(lote);
    }

    public void eliminar(Integer idLote) {
        loteRepository.deleteById(idLote);
    }

    public List<LoteEntity> listarProximosAVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);
        return loteRepository.findByFechaVencimientoBetweenOrderByFechaVencimientoAsc(hoy, limite);
    }

    public List<LoteEntity> listarVencidos() {
        return loteRepository.findByFechaVencimientoBeforeOrderByFechaVencimientoAsc(LocalDate.now());
    }
}
