package com.example.boticadesarrolloweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("")
public class controller {

    @GetMapping("")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(){
        return "dashboard";
    }

    @GetMapping("/venta")
    public String venta(){
        return "venta";
    }

    @GetMapping("/compras")
    public String compras(){
        return "compras";
    }

    @GetMapping("/inventario")
    public String inventario(){
        return "inventario";
    }

    @GetMapping("/proveedores")
    public String proveedores(){
        return "proveedores";
    }

    @GetMapping("/usuarios")
    public String usuarios(){
        return "usuarios";
    }

    @GetMapping("/reporte")
    public String reporte(){
        return "reporte";
    }

    @GetMapping("/alertas")
    public String alertas(){
        return "alertas";
    }
}
