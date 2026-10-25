package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.CategoriaEntity;
import com.example.boticadesarrolloweb.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<CategoriaEntity> listarTodas() {
        return categoriaRepository.findAll();
    }

    public List<CategoriaEntity> listarActivas() {
        return categoriaRepository.findByEstado(1);
    }

    public Optional<CategoriaEntity> buscarPorId(Integer id) {
        return categoriaRepository.findById(id);
    }

    public Optional<CategoriaEntity> buscarPorNombre(String nombre) {
        return categoriaRepository.findByNombreIgnoreCase(nombre);
    }

    public boolean existeNombre(String nombre) {
        return categoriaRepository.existsByNombreIgnoreCase(nombre);
    }

    public CategoriaEntity guardar(CategoriaEntity categoria) {
        return categoriaRepository.save(categoria);
    }
}
