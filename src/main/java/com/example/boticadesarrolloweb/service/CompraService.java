package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.CompraEntity;
import com.example.boticadesarrolloweb.entity.ProveedorEntity;
import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.enums.EstadoCompra;
import com.example.boticadesarrolloweb.model.Compra;
import com.example.boticadesarrolloweb.repository.CompraRepository;
import com.example.boticadesarrolloweb.repository.ProveedorRepository;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompraService {
    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;

    public CompraService(
            CompraRepository compraRepository,
            ProveedorRepository proveedorRepository,
            UsuarioRepository usuarioRepository) {
        this.compraRepository = compraRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<CompraEntity> listarCompras() {
        return compraRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CompraEntity buscarPorId(Integer id) {
        return compraRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Compra no encontrada"));
    }

    public List<ProveedorEntity> listarProveedores() {
        return proveedorRepository.findAll();
    }

    public List<UsuarioEntity> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Transactional
    public CompraEntity guardarCompra(Compra compra) {

        CompraEntity entidad;

        if (compra.getIdCompra() != null) {
            entidad = buscarPorId(compra.getIdCompra());

            if (entidad.getEstado() == EstadoCompra.ANULADA) {
                throw new IllegalStateException(
                        "No se puede editar una compra anulada");
            }
        } else {
            entidad = new CompraEntity();
            entidad.setEstado(EstadoCompra.REGISTRADA);
        }

        ProveedorEntity proveedor =
                proveedorRepository.findById(compra.getIdProveedor())
                        .orElseThrow(() ->
                                new RuntimeException("Proveedor no encontrado"));

        UsuarioEntity usuario =
                usuarioRepository.findById(compra.getIdUsuario())
                        .orElseThrow(() ->
                                new RuntimeException("Usuario no encontrado"));

        entidad.setProveedor(proveedor);
        entidad.setUsuario(usuario);
        entidad.setTipoComprobante(compra.getTipoComprobante());
        entidad.setNumeroComprobante(compra.getNumeroComprobante());
        entidad.setTotal(compra.getTotal());

        return compraRepository.save(entidad);
    }

    @Transactional
    public void anularCompra(Integer id) {
        CompraEntity compra = compraRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("La compra no existe."));

        if (compra.getEstado() == EstadoCompra.ANULADA) {
            throw new IllegalStateException("La compra ya está anulada.");
        }

        compra.setEstado(EstadoCompra.ANULADA);
        compraRepository.save(compra);
    }
}
