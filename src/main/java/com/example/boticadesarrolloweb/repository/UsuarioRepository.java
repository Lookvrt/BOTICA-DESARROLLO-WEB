package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface    UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {

    Optional<UsuarioEntity> findByUsuario(String usuario);

    Optional<UsuarioEntity> findByEmail(String email);

    Optional<UsuarioEntity> findByEmailIgnoreCase(String email);

    boolean existsByUsuario(String usuario);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UsuarioEntity u WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(REPLACE(u.nombres, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:query, ' ', ''), '%')) OR " +
            "LOWER(REPLACE(u.apellidos, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:query, ' ', ''), '%')) OR " +
            "LOWER(REPLACE(CONCAT(u.nombres, ' ', u.apellidos), ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:query, ' ', ''), '%'))) " +
            "ORDER BY " +
            "CASE WHEN :orden = 'desc' THEN u.nombres END DESC, " +
            "CASE WHEN :orden = 'asc' OR :orden IS NULL OR :orden = '' THEN u.nombres END ASC")
    List<UsuarioEntity> buscarYOrdenarPorNombre(@Param("query") String query,
                                                @Param("orden") String orden);

    // ── OTP: cada cambio de estado es un UPDATE atómico en la BD ──

    /** Guarda un OTP nuevo y reinicia su contador de intentos. */
    @Modifying
    @Transactional
    @Query("UPDATE UsuarioEntity u SET u.otpCode = :codigo, u.otpExpiracion = :expiracion, u.otpIntentos = 0 " +
            "WHERE u.idUsuario = :id")
    void guardarOtp(@Param("id") Integer id,
                    @Param("codigo") String codigo,
                    @Param("expiracion") LocalDateTime expiracion);

    /**
     * Consume un intento del OTP vigente. Devuelve 1 si aún quedaban intentos y 0 si ya se agotaron
     * (o no hay OTP). Al ser un solo UPDATE condicional, aunque lleguen peticiones simultáneas un
     * mismo código nunca se puede comprobar más de {@code max} veces.
     */
    @Modifying
    @Transactional
    @Query("UPDATE UsuarioEntity u SET u.otpIntentos = COALESCE(u.otpIntentos, 0) + 1 " +
            "WHERE u.idUsuario = :id AND u.otpCode IS NOT NULL AND COALESCE(u.otpIntentos, 0) < :max")
    int consumirIntentoOtp(@Param("id") Integer id, @Param("max") int max);

    /** Borra el OTP y su contador (tras un acierto). */
    @Modifying
    @Transactional
    @Query("UPDATE UsuarioEntity u SET u.otpCode = null, u.otpExpiracion = null, u.otpIntentos = 0 " +
            "WHERE u.idUsuario = :id")
    void limpiarOtp(@Param("id") Integer id);
}