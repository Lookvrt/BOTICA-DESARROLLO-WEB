package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.ProveedorEntity;
import com.example.boticadesarrolloweb.model.Proveedor;
import com.example.boticadesarrolloweb.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProveedorService {
    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional(readOnly = true)
    public List<ProveedorEntity> listarProveedores() {
        return listarProveedores(null, "asc");
    }

    @Transactional(readOnly = true)
    public List<ProveedorEntity> listarProveedores(String query, String orden) {
        String queryLimpia = (query != null) ? query.trim() : null;
        return proveedorRepository.buscarYOrdenarPorRazonSocial(queryLimpia, orden);
    }

    @Transactional
    public ProveedorEntity guardarProveedor(Proveedor proveedor) {
        String rucLimpio = (proveedor.getRuc() != null && !proveedor.getRuc().trim().isEmpty())
                ? proveedor.getRuc().trim()
                : null;

        if (rucLimpio != null) {
            boolean rucDuplicado = (proveedor.getIdProveedor() == null)
                    ? proveedorRepository.existsByRuc(rucLimpio)
                    : proveedorRepository.existsByRucAndIdProveedorNot(rucLimpio, proveedor.getIdProveedor());

            if (rucDuplicado) {
                throw new IllegalArgumentException("El RUC " + rucLimpio + " ya se encuentra registrado en otro proveedor.");
            }
        }

        ProveedorEntity entidad;

        if (proveedor.getIdProveedor() != null) {
            entidad = proveedorRepository.findById(proveedor.getIdProveedor())
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        } else {
            entidad = new ProveedorEntity();
            entidad.setEstado(1);
        }

        entidad.setRazonSocial(proveedor.getRazonSocial().trim());
        entidad.setRuc(rucLimpio);
        entidad.setDireccion(proveedor.getDireccion());
        entidad.setTelefono(proveedor.getTelefono());
        entidad.setCorreo(proveedor.getCorreo());
        entidad.setEstado(proveedor.getEstado() == null ? 1 : proveedor.getEstado());

        return proveedorRepository.save(entidad);
    }

    @Transactional
    public void eliminarProveedor(Integer id) {
        if (!proveedorRepository.existsById(id)) {
            throw new RuntimeException("Proveedor no encontrado");
        }

        proveedorRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ProveedorEntity buscarPorId(Integer id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
    }
}