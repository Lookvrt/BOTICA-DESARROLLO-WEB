package com.example.boticadesarrolloweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class Application {

    // La zona horaria se fija ANTES de arrancar Spring: el pool de conexiones y el driver MySQL leen la
    // zona por defecto de la JVM al crear las conexiones. Con @PostConstruct llegaba tarde y el driver
    // trabajaba en UTC (Render) mientras el código Java ya usaba Lima.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}