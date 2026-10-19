package com.example.boticadesarrolloweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReporteController {

    @GetMapping("/reporte")
    public String reporte(){
        return "reporte";
    }
}
