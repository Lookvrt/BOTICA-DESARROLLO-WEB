package com.example.boticadesarrolloweb.security;

import com.example.boticadesarrolloweb.entity.CategoriaEntity;
import com.example.boticadesarrolloweb.entity.LoteEntity;
import com.example.boticadesarrolloweb.entity.ProductoEntity;
import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.enums.Rol;
import com.example.boticadesarrolloweb.repository.CategoriaRepository;
import com.example.boticadesarrolloweb.repository.LoteRepository;
import com.example.boticadesarrolloweb.repository.ProductoRepository;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Inicializador de datos de prueba para asegurar que siempre haya:
 * 1. Usuario Administrador disponible.
 * 2. Categorías farmacéuticas básicas.
 * 3. Productos y lotes iniciales para pruebas del inventario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

        private final UsuarioRepository usuarioRepository;
        private final CategoriaRepository categoriaRepository;
        private final ProductoRepository productoRepository;
        private final LoteRepository loteRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) {
                inicializarAdmin();
                inicializarCategoriasYProductos();
        }

        private void inicializarAdmin() {
                if (!usuarioRepository.existsByUsuario("admin")) {
                        UsuarioEntity admin = UsuarioEntity.builder()
                                        .usuario("admin")
                                        .clave(passwordEncoder.encode("admin123"))
                                        .nombres("jwen")
                                        .apellidos("Administrador")
                                        .email("jwen.admin@botica.com")
                                        .rol(Rol.ADMINISTRADOR)
                                        .estado(1)
                                        .build();

                        usuarioRepository.save(admin);
                        log.info("==================================================================");
                        log.info("👤 Usuario de prueba creado: admin / clave: admin123 (ADMINISTRADOR)");
                        log.info("==================================================================");
                }
        }

        private void inicializarCategoriasYProductos() {
                if (categoriaRepository.count() == 0) {
                        log.info("Inicializando categorías farmacéuticas...");

                        CategoriaEntity catAnalgesicos = categoriaRepository.save(CategoriaEntity.builder()
                                        .nombre("Analgésicos y Antipiréticos")
                                        .descripcion("Alivio del dolor y reducción de fiebre")
                                        .estado(1).build());

                        CategoriaEntity catAntibioticos = categoriaRepository.save(CategoriaEntity.builder()
                                        .nombre("Antibióticos")
                                        .descripcion("Tratamiento de infecciones bacterianas")
                                        .estado(1).build());

                        CategoriaEntity catAntiinflamatorios = categoriaRepository.save(CategoriaEntity.builder()
                                        .nombre("Antiinflamatorios")
                                        .descripcion("Reducción de inflamación y dolores musculares/articulares")
                                        .estado(1).build());

                        CategoriaEntity catAntihistaminicos = categoriaRepository.save(CategoriaEntity.builder()
                                        .nombre("Antihistamínicos")
                                        .descripcion("Control de alergias y rinitis")
                                        .estado(1).build());

                        CategoriaEntity catVitaminas = categoriaRepository.save(CategoriaEntity.builder()
                                        .nombre("Vitaminas y Suplementos")
                                        .descripcion("Fortalecimiento inmunológico y nutricional")
                                        .estado(1).build());

                        CategoriaEntity catGastro = categoriaRepository.save(CategoriaEntity.builder()
                                        .nombre("Gastrointestinales")
                                        .descripcion("Protectores gástricos, antiácidos y digestivos")
                                        .estado(1).build());

                        CategoriaEntity catCuidado = categoriaRepository.save(CategoriaEntity.builder()
                                        .nombre("Cuidado Personal y Primeros Auxilios")
                                        .descripcion("Antisépticos, alcohol, gasas y cuidado dérmico")
                                        .estado(1).build());

                        // Si no hay productos, sembrar productos iniciales con lotes
                        if (productoRepository.count() == 0) {
                                log.info("Inicializando productos y lotes de farmacia para inventario...");

                                // 1. Paracetamol
                                crearProductoConLote("MED001", "Paracetamol 500mg x100",
                                                "Paracetamol genérico para dolor y fiebre", "Paracetamol", "500 mg",
                                                "Caja x 100 tabletas", catAnalgesicos,
                                                new BigDecimal("8.50"), new BigDecimal("15.00"), 10, 0,
                                                "LT-2024-PAR1", LocalDate.now().plusMonths(18), 120);

                                // 2. Amoxicilina (Requiere receta - Stock bajo para alerta)
                                crearProductoConLote("MED002", "Amoxicilina 500mg x20",
                                                "Antibiótico de amplio espectro", "Amoxicilina", "500 mg",
                                                "Caja x 20 cápsulas", catAntibioticos,
                                                new BigDecimal("12.00"), new BigDecimal("22.50"), 15, 1,
                                                "LT-2024-AMX2", LocalDate.now().plusMonths(8), 6);

                                // 3. Ibuprofeno 400mg
                                crearProductoConLote("MED003", "Ibuprofeno 400mg x50",
                                                "Antiinflamatorio no esteroideo (AINE)", "Ibuprofeno", "400 mg",
                                                "Caja x 50 tabletas recubiertas", catAntiinflamatorios,
                                                new BigDecimal("6.00"), new BigDecimal("12.00"), 10, 0,
                                                "LT-2024-IBU1", LocalDate.now().plusMonths(24), 85);

                                // 4. Cetirizina 10mg
                                crearProductoConLote("MED004", "Cetirizina 10mg x30",
                                                "Antihistamínico para alergias y urticaria", "Cetirizina Clorhidrato",
                                                "10 mg",
                                                "Caja x 30 comprimidos", catAntihistaminicos,
                                                new BigDecimal("5.50"), new BigDecimal("11.00"), 8, 0,
                                                "LT-2024-CET1", LocalDate.now().plusMonths(14), 45);

                                // 5. Omeprazol 20mg
                                crearProductoConLote("MED005", "Omeprazol 20mg x30",
                                                "Inhibidor de bomba de protones, protector gástrico", "Omeprazol",
                                                "20 mg",
                                                "Caja x 30 cápsulas con microgránulos", catGastro,
                                                new BigDecimal("7.00"), new BigDecimal("14.50"), 10, 0,
                                                "LT-2024-OME1", LocalDate.now().plusMonths(16), 30);

                                // 6. Complejo B Forte
                                crearProductoConLote("MED006", "Complejo B Forte x60",
                                                "Suplemento multivitamínico del complejo B", "Vitaminas B1, B6, B12",
                                                "Forte",
                                                "Frasco x 60 cápsulas blandas", catVitaminas,
                                                new BigDecimal("18.00"), new BigDecimal("32.00"), 5, 0,
                                                "LT-2024-CBF1", LocalDate.now().plusMonths(20), 25);

                                // 7. Azitromicina 500mg (Próximo a vencer para pruebas de alerta)
                                crearProductoConLote("MED007", "Azitromicina 500mg x3",
                                                "Antibiótico macrólido de dosis diaria", "Azitromicina", "500 mg",
                                                "Caja x 3 tabletas", catAntibioticos,
                                                new BigDecimal("9.00"), new BigDecimal("18.00"), 5, 1,
                                                "LT-2024-AZI1", LocalDate.now().plusDays(25), 14);

                                // 8. Alcohol Medicinal 70°
                                crearProductoConLote("MED008", "Alcohol Medicinal 70° 500ml",
                                                "Antiséptico de uso externo y desinfección", "Etanol 70°", "70%",
                                                "Frasco x 500 ml", catCuidado,
                                                new BigDecimal("3.20"), new BigDecimal("6.50"), 12, 0,
                                                "LT-2024-ALC1", LocalDate.now().plusYears(3), 60);

                                log.info("✅ 8 productos y lotes de farmacia inicializados correctamente.");
                        }
                }
        }

        private void crearProductoConLote(String codigo, String nombre, String desc,
                        String pActivo, String conc, String pres,
                        CategoriaEntity cat, BigDecimal pCompra, BigDecimal pVenta,
                        int stockMin, int receta, String nroLote,
                        LocalDate vencimiento, int cantidad) {

                ProductoEntity p = productoRepository.save(ProductoEntity.builder()
                                .codigo(codigo)
                                .nombre(nombre)
                                .descripcion(desc)
                                .principioActivo(pActivo)
                                .concentracion(conc)
                                .presentacion(pres)
                                .categoria(cat)
                                .precioCompra(pCompra)
                                .precioVenta(pVenta)
                                .stockMinimo(stockMin)
                                .requiereReceta(receta)
                                .estado(1)
                                .build());

                loteRepository.save(LoteEntity.builder()
                                .producto(p)
                                .numeroLote(nroLote)
                                .fechaFabricacion(LocalDate.now().minusMonths(2))
                                .fechaVencimiento(vencimiento)
                                .cantidadInicial(cantidad)
                                .stockActual(cantidad)
                                .precioCompra(pCompra)
                                .build());
        }
}
