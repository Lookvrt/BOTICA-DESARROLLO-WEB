package com.example.boticadesarrolloweb.entity;

import com.example.boticadesarrolloweb.enums.Rol;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "usuarios")
@SQLDelete(sql = "UPDATE usuarios SET estado = 0 WHERE id_usuario = ?")
@SQLRestriction("estado = 1")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "usuario", nullable = false, length = 50, unique = true)
    private String usuario;

    @Column(name = "email", nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "clave", nullable = false, length = 255)
    private String clave;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    @Builder.Default
    private Rol rol = Rol.VENDEDOR;

    @Column(name = "estado", nullable = false)
    @Builder.Default
    private Integer estado = 1;

    // --- 2FA: Código OTP ---
    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expiracion")
    private LocalDateTime otpExpiracion;
}