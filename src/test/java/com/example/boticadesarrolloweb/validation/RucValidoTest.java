package com.example.boticadesarrolloweb.validation;

import org.junit.jupiter.api.Test;

import static com.example.boticadesarrolloweb.validation.RucValido.Validador.calcularDigitoVerificador;
import static com.example.boticadesarrolloweb.validation.RucValido.Validador.esValido;
import static org.junit.jupiter.api.Assertions.*;

class RucValidoTest {

    @Test
    void aceptaRucsConDigitoVerificadorCorrecto() {
        assertTrue(esValido("20100070970"));   // persona jurídica (dígito 10 → 0)
        assertTrue(esValido("20" + "12345678" + calcularDigitoVerificador("2012345678")));
        assertTrue(esValido("10" + "45678912" + calcularDigitoVerificador("1045678912")));
    }

    @Test
    void rechazaDigitoVerificadorIncorrecto() {
        assertFalse(esValido("20100070971"));
    }

    @Test
    void rechazaPrefijoNoValido() {
        String base = "3012345678";
        assertFalse(esValido(base + calcularDigitoVerificador(base)));
    }

    @Test
    void rechazaLongitudOCaracteresInvalidos() {
        assertFalse(esValido("2010007097"));
        assertFalse(esValido("201000709700"));
        assertFalse(esValido("2010007097A"));
        assertFalse(esValido(null));
    }

    @Test
    void casosEspecialesDelDigito() {
        // 11 - (suma % 11) = 11 → dígito 1; = 10 → dígito 0
        for (long n = 0; n < 2000; n++) {
            String base = String.format("20%08d", n);
            int d = calcularDigitoVerificador(base);
            assertTrue(d >= 0 && d <= 9);
            assertTrue(esValido(base + d));
        }
    }
}
