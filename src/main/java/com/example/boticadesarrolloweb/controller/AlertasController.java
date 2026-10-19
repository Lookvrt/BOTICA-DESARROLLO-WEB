package com.example.boticadesarrolloweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AlertasController {

    @GetMapping("/alertas")
    public String alertas(){
        return "alertas";
    }
}
