package com.example.boticadesarrolloweb.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Set;

/**
 * Valida un RUC según las reglas de SUNAT:
 * - 11 dígitos numéricos;
 * - prefijo válido: 10 (persona natural), 15/16/17 (casos especiales) o 20 (persona jurídica);
 * - dígito verificador (módulo 11 con factores 5,4,3,2,7,6,5,4,3,2).
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RucValido.Validador.class)
public @interface RucValido {

    String message() default "El RUC no es válido según SUNAT (verifica los 11 dígitos)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<RucValido, String> {

        private static final int[] FACTORES = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        private static final Set<String> PREFIJOS = Set.of("10", "15", "16", "17", "20");

        @Override
        public boolean isValid(String ruc, ConstraintValidatorContext context) {
            // El campo vacío lo controla @NotBlank
            return ruc == null || ruc.isBlank() || esValido(ruc);
        }

        public static boolean esValido(String ruc) {
            if (ruc == null || !ruc.matches("\\d{11}") || !PREFIJOS.contains(ruc.substring(0, 2))) {
                return false;
            }
            return calcularDigitoVerificador(ruc.substring(0, 10)) == ruc.charAt(10) - '0';
        }

        /** Dígito verificador para los 10 primeros dígitos de un RUC. */
        public static int calcularDigitoVerificador(String diezDigitos) {
            int suma = 0;
            for (int i = 0; i < 10; i++) {
                suma += (diezDigitos.charAt(i) - '0') * FACTORES[i];
            }
            int digito = 11 - (suma % 11);
            return digito == 10 ? 0 : (digito == 11 ? 1 : digito);
        }
    }
}
