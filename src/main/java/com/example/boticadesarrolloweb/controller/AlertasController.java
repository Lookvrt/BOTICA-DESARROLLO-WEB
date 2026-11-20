package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.model.InventarioItemDto;
import com.example.boticadesarrolloweb.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AlertasController {

    private final AlertaService alertaService;

    @GetMapping("/alertas")
    public String alertas(Model model) {
        List<InventarioItemDto> items = alertaService.listarProductosBajoStockMinimo();
        model.addAttribute("items", items);
        model.addAttribute("totalAlertas", items.size());
        return "alertas";
    }
}
