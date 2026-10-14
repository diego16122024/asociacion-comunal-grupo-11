package com.sv.grupo11.asociacioncomunal.view;

import com.sv.grupo11.asociacioncomunal.config.AppContext;
import com.sv.grupo11.asociacioncomunal.dto.*;
import com.sv.grupo11.asociacioncomunal.exception.AsociacionException;
import com.sv.grupo11.asociacioncomunal.exception.CierreMensualException;
import com.sv.grupo11.asociacioncomunal.model.*;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Predicate;

/** Interfaz de consola: solo lee/imprime y delega TODA la logica en los controladores. */
public class ConsoleView {
    private record Opcion(String etiqueta, Predicate<Usuario> visible, Runnable accion) { }

    private final AppContext ctx;
    private final Scanner in;
    private final PrintStream out;

    public ConsoleView(AppContext ctx, Scanner in, PrintStream out) {
        this.ctx = ctx;
        this.in = in;
        this.out = out;
    }

    public void ejecutar() {
        out.println("=== Asociacion Comunal Grupo 11 ===");
        try {
            while (true) {
                if (ctx.auth.usuarioActual().isEmpty() && !login()) return;
                menuPrincipal();
            }
        } catch (NoSuchElementException fin) {
            out.println("\nFin de la entrada. Hasta luego.");
        }
    }

    // ---------- login ----------
    private boolean login() {
        out.println("\nInicio de sesion (deje el usuario vacio para salir)");
        while (true) {
            String usuario = leer("Usuario: ");
            if (usuario.isBlank()) return false;
            String clave = leer("Contrasena: ");
            try {
                Usuario u = ctx.auth.iniciarSesion(usuario, clave);
                out.println("Bienvenido, " + u.getNombreUsuario() + " (" + u.getRol().getEtiqueta() + ")");
                return true;
            } catch (AsociacionException e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    // ---------- menu ----------
    private void menuPrincipal() {
        Usuario u = ctx.auth.usuarioActual().orElseThrow();
        List<Opcion> todas = List.of(
                new Opcion("Panel", x -> x.puede(Permiso.VER_SOCIOS), this::panel),
                new Opcion("Socios", x -> x.puede(Permiso.VER_SOCIOS), this::socios),
                new Opcion("Cuotas y pagos", x -> x.puede(Permiso.VER_FINANZAS), this::cuotasYPagos),
                new Opcion("Morosos", x -> x.puede(Permiso.VER_MOROSOS), this::morosos),
                new Opcion("Cierre mensual", x -> x.puede(Permiso.EJECUTAR_CIERRE), this::cierre),
                new Opcion("Usuarios", x -> x.puede(Permiso.GESTIONAR_USUARIOS), this::usuarios),
                new Opcion("Mi cuenta", x -> x.getRol() == Rol.SOCIO, this::miCuenta));
        List<Opcion> visibles = new ArrayList<>();
        for (Opcion o : todas) if (o.visible().test(u)) visibles.add(o);

        while (ctx.auth.usuarioActual().isPresent()) {
            out.println("\n--- Menu (" + u.getRol().getEtiqueta() + ") ---");
            for (int i = 0; i < visibles.size(); i++) out.println((i + 1) + ". " + visibles.get(i).etiqueta());
            out.println("0. Cerrar sesion");
            String op = leer("Opcion: ");
            if (op.equals("0")) { ctx.auth.cerrarSesion(); return; }
            try {
                int idx = Integer.parseInt(op) - 1;
                if (idx < 0 || idx >= visibles.size()) { out.println("Opcion no valida."); continue; }
                visibles.get(idx).accion().run();
            } catch (NumberFormatException e) {
                out.println("Opcion no valida.");
            }
        }
    }

    /** Ejecuta una accion mostrando los errores de negocio de forma amigable (sin tumbar el programa). */
    private void seguro(Runnable accion) {
        try {
            accion.run();
        } catch (AsociacionException e) {
            out.println("Error: " + e.getMessage());
        } catch (DateTimeParseException | NumberFormatException e) {
            out.println("Error: formato invalido (" + e.getMessage() + ")");
        }
    }

    // ---------- pantallas ----------
    private void panel() {
        seguro(() -> {
            Usuario u = ctx.auth.usuarioActual().orElseThrow();
            ResumenPanel r = ctx.reportes.resumen();
            out.println("\n== Resumen ==");
            out.printf("Socios activos: %d de %d registrados | Junta directiva: %d%n",
                    r.sociosActivos(), r.sociosRegistrados(), r.miembrosDirectiva());
            if (u.puede(Permiso.VER_FINANZAS)) {
                out.printf("Recaudado: %s (%d cuotas) | Por cobrar: %s (%d) | En mora: %s (%d)%n",
                        dinero(r.recaudado()), r.cuotasPagadas(), dinero(r.porCobrar()), r.cuotasPendientes(),
                        dinero(r.enMora()), r.cuotasVencidas());
                out.println("Ultimos pagos:");
                if (r.ultimosPagos().isEmpty()) out.println("  (sin pagos)");
                for (Cuota c : r.ultimosPagos()) {
                    out.printf("  %s | socio #%d | periodo %s | %s%n", c.getFechaPago(), c.getIdSocio(), c.getPeriodo(), dinero(c.getMonto()));
                }
                imprimirCierre(java.util.Optional.ofNullable(r.ultimoCierre()));
            }
        });
    }

    private void socios() {
        Usuario u = ctx.auth.usuarioActual().orElseThrow();
        while (true) {
            out.println("\n-- Socios --");
            out.println("1. Listar / buscar");
            if (u.puede(Permiso.EDITAR_SOCIOS)) {
                out.println("2. Registrar socio");
                out.println("3. Registrar miembro de junta directiva");
                out.println("4. Actualizar telefono/direccion");
            }
            if (u.puede(Permiso.CAMBIAR_ESTADO_SOCIO)) out.println("5. Dar de baja / reactivar");
            out.println("0. Volver");
            String op = leer("Opcion: ");
            switch (op) {
                case "0" -> { return; }
                case "1" -> seguro(() -> listar(ctx.socios.buscar(leer("Buscar por nombre o DUI (Enter = todos): "))));
                case "2" -> seguro(() -> registrarSocio(false));
                case "3" -> seguro(() -> registrarSocio(true));
                case "4" -> seguro(() -> {
                    long id = leerLong("ID del socio: ");
                    Socio s = ctx.socios.actualizarContacto(id, leer("Nuevo telefono (####-####, Enter = no cambiar): "),
                            leer("Nueva direccion (Enter = no cambiar): "));
                    out.println("Socio actualizado: " + s.descripcionCorta());
                });
                case "5" -> seguro(() -> {
                    Socio s = ctx.socios.cambiarEstado(leerLong("ID del socio: "));
                    out.println(s.getNombre() + ": " + (s.estaActivo() ? "reactivado" : "dado de baja"));
                });
                default -> out.println("Opcion no valida.");
            }
        }
    }

    private void registrarSocio(boolean directiva) {
        String nombre = leer("Nombre completo: ");
        String dui = leer("DUI (########-#): ");
        String tel = leer("Telefono (####-####): ");
        String dir = leer("Direccion: ");
        LocalDate ingreso = leerFechaOpcional("Fecha de ingreso (AAAA-MM-DD, Enter = hoy): ");
        String cargo = null, periodo = null;
        if (directiva) {
            cargo = leer("Cargo (Presidente/Vicepresidente/Secretario/Tesorero/Vocal): ");
            periodo = leer("Periodo (ej. 2026-2028): ");
        }
        Socio s = ctx.socios.registrar(new SocioRequest(nombre, dui, tel, dir, ingreso, cargo, periodo));
        out.println("Registrado: " + s.descripcionCorta() + " | cuota mensual " + dinero(s.calcularCuotaMensual()));
    }

    private void listar(List<Socio> lista) {
        if (lista.isEmpty()) { out.println("No hay socios que coincidan."); return; }
        out.printf("%-4s %-26s %-11s %-10s %-9s %-16s %-8s%n", "ID", "Nombre", "DUI", "Telefono", "Estado", "Cargo", "Cuota");
        for (Socio s : lista) {
            out.printf("%-4d %-26s %-11s %-10s %-9s %-16s %-8s%n", s.getId(), s.getNombre(), s.getDui(), s.getTelefono(),
                    s.getEstado(), s.getCargo() == null ? "-" : s.getCargo(), dinero(s.calcularCuotaMensual()));
        }
    }

    private void cuotasYPagos() {
        seguro(() -> {
            CuentaSocio c = ctx.cuotas.cuentaDeSocio(leerLong("ID del socio: "));
            imprimirCuenta(c);
            if (c.pendientes().isEmpty() || !ctx.auth.usuarioActual().orElseThrow().puede(Permiso.REGISTRAR_PAGOS)) return;
            String id = leer("ID de cuota a pagar (Enter = volver): ");
            if (id.isBlank()) return;
            Cuota pagada = ctx.cuotas.registrarPago(Long.parseLong(id.trim()),
                    leerFechaOpcional("Fecha de pago (AAAA-MM-DD, Enter = hoy): "));
            out.println("Pago registrado: cuota #" + pagada.getId() + " (" + pagada.getPeriodo() + ")");
        });
    }

    private void morosos() {
        seguro(() -> {
            List<MorosoDTO> lista = ctx.reportes.morosos();
            if (lista.isEmpty()) { out.println("No hay socios en mora. (Las cuotas pasan a vencidas al ejecutar el cierre mensual)"); return; }
            out.printf("%-4s %-26s %-11s %-8s %-10s%n", "ID", "Nombre", "DUI", "Cuotas", "Monto");
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (MorosoDTO m : lista) {
                out.printf("%-4d %-26s %-11s %-8d %-10s%n", m.socio().getId(), m.socio().getNombre(), m.socio().getDui(),
                        m.cuotasVencidas(), dinero(m.montoAdeudado()));
                total = total.add(m.montoAdeudado());
            }
            out.println("Total en mora: " + dinero(total) + " | " + lista.size() + " socio(s)");
        });
    }

    private void cierre() {
        out.println("\n-- Cierre mensual --");
        out.println("Genera la cuota del periodo para cada socio activo y, EN PARALELO, marca como vencidas las pendientes atrasadas.");
        try {
            String p = leer("Periodo (AAAA-MM, Enter = mes actual): ");
            YearMonth periodo = p.isBlank() ? YearMonth.now() : YearMonth.parse(p.trim());
            LocalDate corte = leerFechaOpcional("Fecha de corte (AAAA-MM-DD, Enter = hoy): ");
            ResultadoCierre r = ctx.cierre.ejecutar(periodo, corte != null ? corte : LocalDate.now());
            out.println("Cierre completado:");
            imprimirCierre(java.util.Optional.of(r));
        } catch (CierreMensualException e) {
            out.println("El cierre termino con errores: " + e.getMessage());
        } catch (AsociacionException e) {
            out.println("Error: " + e.getMessage());
        } catch (DateTimeParseException e) {
            out.println("Error: formato de fecha invalido (" + e.getParsedString() + ")");
        }
    }

    private void usuarios() {
        seguro(() -> {
            out.println("\n-- Usuarios --");
            for (Usuario x : ctx.usuarios.listar()) {
                out.printf("#%d %-14s %-14s socio asociado: %s%n", x.getId(), x.getNombreUsuario(), x.getRol().getEtiqueta(),
                        x.getIdSocio() == null ? "-" : "#" + x.getIdSocio());
            }
            if (!leer("Crear usuario nuevo? (s/N): ").equalsIgnoreCase("s")) return;
            String nombre = leer("Usuario: ");
            String clave = leer("Contrasena (min. 6): ");
            Rol rol = Rol.valueOf(leer("Rol (ADMINISTRADOR/SECRETARIO/TESORERO/SOCIO): ").trim().toUpperCase(Locale.ROOT));
            Long idSocio = rol == Rol.SOCIO ? leerLong("ID del socio asociado: ") : null;
            Usuario creado = ctx.usuarios.crear(new NuevoUsuarioRequest(nombre, clave, rol, idSocio));
            out.println("Usuario creado: " + creado.getNombreUsuario());
        });
    }

    private void miCuenta() { seguro(() -> imprimirCuenta(ctx.cuotas.miCuenta())); }

    // ---------- impresion ----------
    private void imprimirCuenta(CuentaSocio c) {
        Socio s = c.socio();
        out.println("\n" + s.descripcionCorta() + " | DUI " + s.getDui() + " | " + s.getEstado() + " | cuota mensual " + dinero(s.calcularCuotaMensual()));
        out.println("Cuotas pendientes:");
        if (c.pendientes().isEmpty()) out.println("  (ninguna)");
        for (Cuota q : c.pendientes()) out.printf("  #%-4d %s vence %s %s %s%n", q.getId(), q.getPeriodo(), q.getFechaVencimiento(), dinero(q.getMonto()), q.getEstado());
        out.println("Total pendiente: " + dinero(c.totalPendiente()));
        out.println("Historial:");
        for (Cuota q : c.historial()) out.printf("  #%-4d %s %s %-9s pagada el %s%n", q.getId(), q.getPeriodo(), dinero(q.getMonto()), q.getEstado(), q.getFechaPago() == null ? "-" : q.getFechaPago());
    }

    private void imprimirCierre(java.util.Optional<ResultadoCierre> cierre) {
        out.print("Ultimo cierre: ");
        if (cierre.isEmpty()) { out.println("aun no se ha ejecutado ninguno."); return; }
        ResultadoCierre r = cierre.get();
        out.printf("periodo %s | %d generada(s) | %d vencida(s) | corte %s | %d ms | %s por %s%n",
                r.periodo(), r.cuotasGeneradas(), r.cuotasVencidas(), r.fechaCorte(), r.duracionMs(), r.fechaEjecucion(), r.ejecutadoPor());
    }


    private static String dinero(java.math.BigDecimal n) { return String.format(Locale.US, "$%.2f", n); }

    // ---------- entrada ----------
    private String leer(String prompt) {
        out.print(prompt);
        out.flush();
        return in.nextLine().trim();
    }

    private long leerLong(String prompt) { return Long.parseLong(leer(prompt)); }

    private LocalDate leerFechaOpcional(String prompt) {
        String v = leer(prompt);
        return v.isBlank() ? null : LocalDate.parse(v);
    }
}
