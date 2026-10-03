package mx.edu.backendacademico.controller;

// Asocia una ruta HTTP con un metodo y entrega datos para serialización JSON.

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1")
public class SaludoController {
    @GetMapping("/saludo")
    public Map<String, String> saludar(@RequestParam(defaultValue = "Mundo") String nombre) {
        String mensaje = "Hola " + nombre;
        return Map.of("mensaje", mensaje);
    }
}