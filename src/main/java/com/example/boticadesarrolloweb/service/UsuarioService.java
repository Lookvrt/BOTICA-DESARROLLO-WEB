package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de gestión de usuarios.
 * Maneja CRUD de usuarios con contraseñas cifradas con BCrypt.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Retorna todos los usuarios activos o filtrados por búsqueda */
    public List<UsuarioEntity> listar(String query) {
        if (query == null || query.isBlank()) {
            return usuarioRepository.findAll();
        }
        return usuarioRepository.buscar(query.trim());
    }

    /** Retorna todos los usuarios activos */
    public List<UsuarioEntity> listarTodos() {
        return usuarioRepository.findAll();
    }

    /** Busca usuario por ID */
    public Optional<UsuarioEntity> buscarPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    /** Busca usuario por nombre de usuario (login) */
    public Optional<UsuarioEntity> buscarPorUsuario(String usuario) {
        return usuarioRepository.findByUsuario(usuario);
    }

    /** Busca usuario por email */
    public Optional<UsuarioEntity> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    /** Crea un nuevo usuario cifrando la contraseña */
    public UsuarioEntity crear(UsuarioEntity usuario) {
        usuario.setClave(passwordEncoder.encode(usuario.getClave()));
        if (usuario.getEstado() == null) {
            usuario.setEstado(1);
        }
        return usuarioRepository.save(usuario);
    }

    /** Actualiza datos del usuario (no actualiza la clave si viene vacía) */
    public UsuarioEntity actualizar(UsuarioEntity usuario) {
        UsuarioEntity existente = usuarioRepository.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        existente.setNombres(usuario.getNombres());
        existente.setApellidos(usuario.getApellidos());
        existente.setUsuario(usuario.getUsuario());
        existente.setEmail(usuario.getEmail());
        existente.setRol(usuario.getRol());

        // Solo actualizar clave si viene con valor
        if (usuario.getClave() != null && !usuario.getClave().isBlank()) {
            existente.setClave(passwordEncoder.encode(usuario.getClave()));
        }

        return usuarioRepository.save(existente);
    }

    /** Elimina lógicamente el usuario (soft delete — estado = 0) */
    public void eliminar(Integer id) {
        usuarioRepository.deleteById(id);
    }

    /** Verifica si ya existe un usuario con ese nombre de usuario */
    public boolean existeUsuario(String usuario) {
        return usuarioRepository.existsByUsuario(usuario);
    }

    /** Verifica si ya existe un usuario con ese email */
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    /** Actualiza directamente la clave del usuario cifrándola y limpia tokens OTP */
    public void cambiarClave(Integer idUsuario, String nuevaClave) {
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setClave(passwordEncoder.encode(nuevaClave));
        usuario.setOtpCode(null);
        usuario.setOtpExpiracion(null);
        usuarioRepository.save(usuario);
    }
}
