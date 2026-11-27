document.addEventListener("DOMContentLoaded", function () {
    // Escapa texto de la BD antes de insertarlo con innerHTML (evita XSS almacenado)
    const escapar = (s) => String(s ?? '').replace(/[&<>"']/g, c =>
        ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));

    // --- Referencias del DOM: Catálogo y Búsqueda ---
    const inputBuscar = document.getElementById("buscarProductoVenta");
    const btnLimpiarBusqueda = document.getElementById("btnLimpiarBusquedaVenta");
    const itemsProducto = document.querySelectorAll(".item-producto-venta");
    const sinResultados = document.getElementById("sinResultadosVenta");
    const contadorProductos = document.getElementById("contadorProductosVenta");

    // --- Referencias del DOM: Carrito y Pago ---
    const tbodyCarrito = document.getElementById("tbodyCarritoVenta");
    const filaCarritoVacio = document.getElementById("filaCarritoVacio");
    const btnVaciarCarrito = document.getElementById("btnVaciarCarrito");
    const selectMetodoPago = document.getElementById("metodoPago");
    const grupoMontoRecibido = document.getElementById("grupoMontoRecibido");
    const inputMontoRecibido = document.getElementById("montoRecibido");
    const cantidadItemsDisplay = document.getElementById("cantidadItemsVenta");
    const filaVuelto = document.getElementById("filaVueltoVenta");
    const vueltoDisplay = document.getElementById("vueltoVentaDisplay");
    const totalDisplay = document.getElementById("totalVentaDisplay");
    const btnRegistrarVenta = document.getElementById("btnRegistrarVenta");

    // --- Referencias del DOM: Alertas y Modal ---
    const alertaVenta = document.getElementById("alertaVenta");
    const alertaVentaIcono = document.getElementById("alertaVentaIcono");
    const alertaVentaTexto = document.getElementById("alertaVentaTexto");
    const btnCerrarAlerta = document.getElementById("btnCerrarAlertaVenta");
    const modalComprobanteEl = document.getElementById("modalComprobanteVenta");

    // --- Estado del Carrito ---
    let carrito = [];
    let ventaRegistradaConExito = false;

    // --- Utilidades ---
    function normalizarTexto(texto) {
        return (texto || "").toLowerCase().replace(/\s+/g, "");
    }

    function formatearMoneda(valor) {
        const numero = Number(valor) || 0;
        return "S/ " + numero.toFixed(2);
    }

    function mostrarAlerta(mensaje, tipo = "warning") {
        if (!alertaVenta || !alertaVentaTexto || !alertaVentaIcono) return;

        alertaVenta.classList.remove("d-none", "alert-warning", "alert-danger", "alert-success", "alert-info");
        alertaVenta.classList.add("d-flex", `alert-${tipo}`);

        alertaVentaIcono.className = "fs-5 bi " + (
            tipo === "danger" ? "bi-exclamation-triangle-fill" :
                tipo === "success" ? "bi-check-circle-fill" :
                    "bi-info-circle-fill"
        );

        alertaVentaTexto.textContent = mensaje;
    }

    function ocultarAlerta() {
        if (!alertaVenta) return;
        alertaVenta.classList.add("d-none");
        alertaVenta.classList.remove("d-flex");
    }

    if (btnCerrarAlerta) {
        btnCerrarAlerta.addEventListener("click", ocultarAlerta);
    }

    // --- 1. Filtrado Instantáneo del Catálogo ---
    function filtrarCatalogo() {
        if (!inputBuscar) return;
        const termino = normalizarTexto(inputBuscar.value);
        let visibles = 0;

        if (btnLimpiarBusqueda) {
            btnLimpiarBusqueda.classList.toggle("d-none", termino.length === 0);
        }

        itemsProducto.forEach(card => {
            const nombreProducto = normalizarTexto(card.getAttribute("data-nombre"));
            const coincide = termino === "" || nombreProducto.includes(termino);
            card.classList.toggle("d-none", !coincide);
            if (coincide) visibles++;
        });

        if (sinResultados) {
            sinResultados.classList.toggle("d-none", visibles > 0 || itemsProducto.length === 0);
        }

        if (contadorProductos) {
            contadorProductos.textContent = `${visibles} producto${visibles === 1 ? "" : "s"} disponible${visibles === 1 ? "" : "s"}`;
        }
    }

    if (inputBuscar) {
        inputBuscar.addEventListener("input", filtrarCatalogo);
        inputBuscar.addEventListener("keydown", function (e) {
            if (e.key === "Enter") e.preventDefault();
        });
    }

    if (btnLimpiarBusqueda) {
        btnLimpiarBusqueda.addEventListener("click", function () {
            inputBuscar.value = "";
            filtrarCatalogo();
            inputBuscar.focus();
        });
    }

    // --- 2. Gestión del Carrito ---
    function agregarProductoAlCarrito(producto) {
        const existente = carrito.find(item => item.idProducto === producto.idProducto);

        if (existente) {
            if (existente.cantidad + 1 > producto.stockMax) {
                mostrarAlerta(`Stock máximo alcanzado para "${producto.nombre}" (${producto.stockMax} unid.).`, "warning");
                return;
            }
            existente.cantidad += 1;
        } else {
            carrito.push({
                idProducto: producto.idProducto,
                nombre: producto.nombre,
                precio: producto.precio,
                stockMax: producto.stockMax,
                requiereReceta: producto.requiereReceta,
                cantidad: 1
            });

            if (producto.requiereReceta) {
                mostrarAlerta(`Atención: "${producto.nombre}" requiere receta médica.`, "warning");
            } else {
                ocultarAlerta();
            }
        }

        renderizarCarrito();
    }

    function actualizarCantidad(idProducto, nuevaCantidad) {
        const index = carrito.findIndex(item => item.idProducto === idProducto);
        if (index === -1) return;

        const item = carrito[index];

        if (nuevaCantidad <= 0) {
            carrito.splice(index, 1);
            ocultarAlerta();
        } else if (nuevaCantidad > item.stockMax) {
            mostrarAlerta(`Solo hay ${item.stockMax} unidades disponibles de "${item.nombre}".`, "warning");
            item.cantidad = item.stockMax;
        } else {
            item.cantidad = nuevaCantidad;
            ocultarAlerta();
        }

        renderizarCarrito();
    }

    function eliminarDelCarrito(idProducto) {
        carrito = carrito.filter(item => item.idProducto !== idProducto);
        ocultarAlerta();
        renderizarCarrito();
    }

    function vaciarCarrito() {
        carrito = [];
        if (inputMontoRecibido) inputMontoRecibido.value = "";
        ocultarAlerta();
        renderizarCarrito();
    }

    if (btnVaciarCarrito) {
        btnVaciarCarrito.addEventListener("click", vaciarCarrito);
    }

    document.querySelectorAll(".btn-agregar-carrito").forEach(btn => {
        btn.addEventListener("click", function () {
            agregarProductoAlCarrito({
                idProducto: parseInt(this.dataset.id, 10),
                nombre: this.dataset.nombre,
                precio: parseFloat(this.dataset.precio) || 0,
                stockMax: parseInt(this.dataset.stock, 10) || 0,
                requiereReceta: this.dataset.receta === "1"
            });
        });
    });

    // --- 3. Renderizado de Tabla y Cálculo de Totales / Vuelto ---
    function calcularTotalVenta() {
        return carrito.reduce((acc, item) => acc + (item.precio * item.cantidad), 0);
    }

    function actualizarResumenPago() {
        const total = calcularTotalVenta();
        const totalUnidades = carrito.reduce((acc, item) => acc + item.cantidad, 0);
        const esEfectivo = selectMetodoPago && selectMetodoPago.value === "EFECTIVO";

        if (cantidadItemsDisplay) {
            cantidadItemsDisplay.textContent = totalUnidades;
        }

        if (totalDisplay) {
            totalDisplay.textContent = formatearMoneda(total);
        }

        if (grupoMontoRecibido) {
            grupoMontoRecibido.classList.toggle("d-none", !esEfectivo);
        }

        if (inputMontoRecibido) {
            inputMontoRecibido.required = esEfectivo;
        }

        if (filaVuelto) {
            filaVuelto.classList.toggle("d-none", !esEfectivo);
        }

        if (esEfectivo && vueltoDisplay) {
            const valorRaw = inputMontoRecibido ? inputMontoRecibido.value.trim() : "";
            const recibido = parseFloat(valorRaw);
            const totalCentavos = Math.round(total * 100);
            const recibidoCentavos = isNaN(recibido) ? 0 : Math.round(recibido * 100);

            if (carrito.length > 0 && valorRaw !== "" && recibidoCentavos < totalCentavos) {
                const faltante = (totalCentavos - recibidoCentavos) / 100;
                vueltoDisplay.textContent = "Falta " + formatearMoneda(faltante);
                vueltoDisplay.classList.remove("text-success");
                vueltoDisplay.classList.add("text-danger");
            } else {
                const vuelto = recibidoCentavos > totalCentavos ? (recibidoCentavos - totalCentavos) / 100 : 0;
                vueltoDisplay.textContent = formatearMoneda(vuelto);
                vueltoDisplay.classList.remove("text-danger");
                vueltoDisplay.classList.add("text-success");
            }
        }

        if (btnRegistrarVenta) {
            btnRegistrarVenta.disabled = carrito.length === 0;
        }

        if (btnVaciarCarrito) {
            btnVaciarCarrito.classList.toggle("d-none", carrito.length === 0);
        }
    }

    function renderizarCarrito() {
        if (!tbodyCarrito) return;

        tbodyCarrito.querySelectorAll("tr.fila-item-carrito").forEach(tr => tr.remove());

        if (carrito.length === 0) {
            if (filaCarritoVacio) filaCarritoVacio.classList.remove("d-none");
            actualizarResumenPago();
            return;
        }

        if (filaCarritoVacio) filaCarritoVacio.classList.add("d-none");

        carrito.forEach(item => {
            const subtotal = item.precio * item.cantidad;
            const tr = document.createElement("tr");
            tr.className = "fila-item-carrito";

            tr.innerHTML = `
                <td>
                    <div class="fw-semibold text-dark small">${escapar(item.nombre)}</div>
                    <div class="text-muted" style="font-size: 0.75rem;">
                        ${formatearMoneda(item.precio)} c/u
                        ${item.requiereReceta ? '<span class="badge bg-danger bg-opacity-10 text-danger ms-1">Receta</span>' : ''}
                    </div>
                </td>
                <td class="text-center">
                    <div class="input-group input-group-sm justify-content-center" style="width: 105px; margin: 0 auto;">
                        <button type="button" class="btn btn-outline-secondary btn-restar-cant" data-id="${item.idProducto}">-</button>
                        <input type="number" class="form-control text-center px-1 input-cant-carrito"
                               value="${item.cantidad}" min="1" max="${item.stockMax}" data-id="${item.idProducto}">
                        <button type="button" class="btn btn-outline-secondary btn-sumar-cant" data-id="${item.idProducto}">+</button>
                    </div>
                </td>
                <td class="text-end fw-semibold text-dark small text-nowrap">
                    ${formatearMoneda(subtotal)}
                </td>
                <td class="text-end">
                    <button type="button" class="btn btn-sm btn-link text-danger p-0 btn-quitar-item"
                            data-id="${item.idProducto}" title="Quitar producto">
                        <i class="bi bi-x-circle"></i>
                    </button>
                </td>
            `;

            tbodyCarrito.appendChild(tr);
        });

        tbodyCarrito.querySelectorAll(".btn-restar-cant").forEach(btn => {
            btn.addEventListener("click", function () {
                const id = parseInt(this.dataset.id, 10);
                const item = carrito.find(i => i.idProducto === id);
                if (item) actualizarCantidad(id, item.cantidad - 1);
            });
        });

        tbodyCarrito.querySelectorAll(".btn-sumar-cant").forEach(btn => {
            btn.addEventListener("click", function () {
                const id = parseInt(this.dataset.id, 10);
                const item = carrito.find(i => i.idProducto === id);
                if (item) actualizarCantidad(id, item.cantidad + 1);
            });
        });

        tbodyCarrito.querySelectorAll(".input-cant-carrito").forEach(input => {
            input.addEventListener("change", function () {
                const id = parseInt(this.dataset.id, 10);
                const val = parseInt(this.value, 10);
                actualizarCantidad(id, isNaN(val) ? 1 : val);
            });
        });

        tbodyCarrito.querySelectorAll(".btn-quitar-item").forEach(btn => {
            btn.addEventListener("click", function () {
                const id = parseInt(this.dataset.id, 10);
                eliminarDelCarrito(id);
            });
        });

        actualizarResumenPago();
    }

    if (selectMetodoPago) {
        selectMetodoPago.addEventListener("change", () => {
            ocultarAlerta();
            actualizarResumenPago();
        });
    }

    if (inputMontoRecibido) {
        inputMontoRecibido.addEventListener("input", actualizarResumenPago);
    }

    // --- 4. Registro de Venta (POST /punto-de-venta/api/registrar) ---
    async function registrarVenta() {
        if (carrito.length === 0) return;

        const total = calcularTotalVenta();
        const totalCentavos = Math.round(total * 100);
        const metodoPago = selectMetodoPago ? selectMetodoPago.value : "EFECTIVO";
        const esEfectivo = metodoPago === "EFECTIVO";

        const valorRaw = inputMontoRecibido ? inputMontoRecibido.value.trim() : "";
        const montoRecibido = parseFloat(valorRaw);
        const recibidoCentavos = isNaN(montoRecibido) ? 0 : Math.round(montoRecibido * 100);

        if (esEfectivo) {
            if (valorRaw === "" || isNaN(montoRecibido) || recibidoCentavos <= 0) {
                mostrarAlerta("Debe ingresar el monto de efectivo recibido mayor a S/ 0.00.", "danger");
                inputMontoRecibido?.focus();
                return;
            }

            if (recibidoCentavos < totalCentavos) {
                mostrarAlerta(`El efectivo recibido (${formatearMoneda(montoRecibido)}) es menor al total a pagar (${formatearMoneda(total)}).`, "danger");
                inputMontoRecibido?.focus();
                return;
            }
        }

        const payload = {
            metodoPago: metodoPago,
            tipoComprobante: "BOLETA",
            montoPagado: esEfectivo ? montoRecibido : total,
            items: carrito.map(item => ({
                idProducto: item.idProducto,
                cantidad: item.cantidad
            }))
        };

        const headers = {
            "Content-Type": "application/json"
        };

        const csrfToken = document.getElementById("csrfToken")?.value;
        const csrfHeader = document.getElementById("csrfHeader")?.value;
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        btnRegistrarVenta.disabled = true;
        const contenidoOriginal = btnRegistrarVenta.innerHTML;
        btnRegistrarVenta.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Procesando...`;

        try {
            const response = await fetch("/punto-de-venta/api/registrar", {
                method: "POST",
                headers: headers,
                body: JSON.stringify(payload)
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || "No se pudo registrar la venta.");
            }

            ocultarAlerta();
            ventaRegistradaConExito = true;

            const totalCobrado = data.total != null ? Number(data.total) : total;
            const vueltoCalculado = data.vuelto != null
                ? Number(data.vuelto)
                : (esEfectivo && montoRecibido > totalCobrado ? montoRecibido - totalCobrado : 0);

            document.getElementById("modalNumeroComprobante").textContent =
                data.numeroComprobante || (data.idVenta ? `B001-${String(data.idVenta).padStart(5, "0")}` : "GENERADO");
            document.getElementById("modalMetodoPago").textContent = data.metodoPago || metodoPago;
            document.getElementById("modalTotalVenta").textContent = formatearMoneda(totalCobrado);

            const modalFilaVuelto = document.getElementById("modalFilaVuelto");
            const modalVueltoVenta = document.getElementById("modalVueltoVenta");
            if (modalFilaVuelto && modalVueltoVenta) {
                modalFilaVuelto.classList.toggle("d-none", !esEfectivo);
                modalVueltoVenta.textContent = formatearMoneda(vueltoCalculado);
            }

            if (modalComprobanteEl) {
                const modal = new bootstrap.Modal(modalComprobanteEl);
                modal.show();
            }

        } catch (error) {
            mostrarAlerta(error.message, "danger");
        } finally {
            btnRegistrarVenta.innerHTML = contenidoOriginal;
            btnRegistrarVenta.disabled = carrito.length === 0;
        }
    }

    if (btnRegistrarVenta) {
        btnRegistrarVenta.addEventListener("click", registrarVenta);
    }

    if (modalComprobanteEl) {
        modalComprobanteEl.addEventListener("hidden.bs.modal", function () {
            if (ventaRegistradaConExito) {
                window.location.reload();
            }
        });
    }

    actualizarResumenPago();
});