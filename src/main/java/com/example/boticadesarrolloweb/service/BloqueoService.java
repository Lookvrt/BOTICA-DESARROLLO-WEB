package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.LoteEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Se usa refresh() y no una consulta con lock porque la entidad ya puede estar cargada en el contexto de
 * persistencia con datos viejos (open-in-view está activo): refresh bloquea la fila Y recarga su estado real.
 * Debe llamarse DENTRO de la transacción; el bloqueo se mantiene hasta el commit o rollback.
 */
@Service
public class BloqueoService {

    @PersistenceContext
    private EntityManager entityManager;

    /** Bloquea una fila (venta, compra...) y recarga su estado. Evita, p. ej., anular la misma venta dos veces a la vez. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void bloquear(Object entidad) {
        entityManager.flush(); // que refresh() no descarte cambios pendientes de la misma transacción
        entityManager.refresh(entidad, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Bloquea y recarga los lotes siempre en orden de id, para reducir el riesgo de deadlock
     * entre operaciones simultáneas.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void bloquearLotes(Collection<LoteEntity> lotes) {
        Map<Integer, LoteEntity> porId = new TreeMap<>();
        for (LoteEntity lote : lotes) {
            if (lote != null) {
                porId.putIfAbsent(lote.getIdLote(), lote);
            }
        }
        entityManager.flush();
        porId.values().forEach(lote -> entityManager.refresh(lote, LockModeType.PESSIMISTIC_WRITE));
    }
}