package com.example.boticadesarrolloweb.model;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    private Integer idUsuario;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no debe superar los 100 caracteres")
    private String nombres;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no debe superar los 100 caracteres")
    private String apellidos;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 50, message = "El usuario no debe superar los 50 caracteres")
    private String usuario;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 255, message = "La contraseña excede el límite de caracteres")
    private String clave;

    @NotNull(message = "Debe seleccionar un rol")
    @Builder.Default
    private Rol rol = Rol.VENDEDOR;

    @Builder.Default
    private Integer estado = 1;

    public enum Rol {
        ADMINISTRADOR,
        VENDEDOR,
        ALMACENERO
    }
}