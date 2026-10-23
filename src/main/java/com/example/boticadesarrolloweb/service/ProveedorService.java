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
        return proveedorRepository.findAll();
    }

    @Transactional
    public ProveedorEntity guardarProveedor(Proveedor proveedor) {
        ProveedorEntity entidad;

        if (proveedor.getIdProveedor() != null) {
            entidad = proveedorRepository.findById(
                    proveedor.getIdProveedor()
            ).orElseThrow(() ->
                    new RuntimeException("Proveedor no encontrado")
            );
        } else {
            entidad = new ProveedorEntity();
            entidad.setEstado(1);
        }

        entidad.setRazonSocial(proveedor.getRazonSocial());
        entidad.setRuc(proveedor.getRuc());
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
                .orElseThrow(() ->
                        new RuntimeException("Proveedor no encontrado"));
    }
}
