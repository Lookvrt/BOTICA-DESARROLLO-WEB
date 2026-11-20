package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.ClienteEntity;
import com.example.boticadesarrolloweb.model.Cliente;
import com.example.boticadesarrolloweb.repository.ClienteRepository;
import com.example.boticadesarrolloweb.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;

    /** Error de negocio cuyo mensaje se muestra al usuario. */
    public static class ClienteException extends RuntimeException {
        public ClienteException(String mensaje) {
            super(mensaje);
        }
    }

    @Transactional(readOnly = true)
    public List<ClienteEntity> listar(String query, String orden) {
        return clienteRepository.buscar(query != null ? query.trim() : null, orden);
    }

    @Transactional(readOnly = true)
    public List<ClienteEntity> listarActivos() {
        return clienteRepository.findByEstadoOrderByRazonSocialAsc(1);
    }

    @Transactional(readOnly = true)
    public Cliente cargarParaEditar(Integer id) {
        ClienteEntity c = buscarPorId(id);
        return Cliente.builder()
                .idCliente(c.getIdCliente())
                .ruc(c.getRuc())
                .razonSocial(c.getRazonSocial())
                .direccion(c.getDireccion())
                .telefono(c.getTelefono())
                .correo(c.getCorreo())
                .estado(c.getEstado())
                .build();
    }

    @Transactional(readOnly = true)
    public ClienteEntity buscarPorId(Integer id) {
        return clienteRepository.findById(id).orElseThrow(() -> new ClienteException("Cliente no encontrado."));
    }

    @Transactional
    public ClienteEntity guardar(Cliente cliente) {
        String ruc = cliente.getRuc().trim();
        clienteRepository.findByRuc(ruc)
                .filter(otro -> !otro.getIdCliente().equals(cliente.getIdCliente()))
                .ifPresent(otro -> {
                    throw new ClienteException("Ya existe un cliente con el RUC " + ruc + ": " + otro.getRazonSocial() + ".");
                });

        ClienteEntity entidad = cliente.getIdCliente() != null ? buscarPorId(cliente.getIdCliente()) : new ClienteEntity();
        entidad.setRuc(ruc);
        entidad.setRazonSocial(cliente.getRazonSocial().trim());
        entidad.setDireccion(vacioANulo(cliente.getDireccion()));
        entidad.setTelefono(vacioANulo(cliente.getTelefono()));
        entidad.setCorreo(vacioANulo(cliente.getCorreo()));
        entidad.setEstado(cliente.getEstado() == null ? 1 : cliente.getEstado());
        return clienteRepository.save(entidad);
    }

    /**
     * Elimina el cliente. Si ya tiene ventas no se puede borrar (las facturas lo referencian):
     * en ese caso se desactiva. Devuelve true si se eliminó y false si se desactivó.
     */
    @Transactional
    public boolean eliminar(Integer id) {
        ClienteEntity cliente = buscarPorId(id);
        if (ventaRepository.existsByClienteIdCliente(id)) {
            cliente.setEstado(0);
            return false;
        }
        clienteRepository.delete(cliente);
        return true;
    }

    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
