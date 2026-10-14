package com.sv.grupo11.asociacioncomunal.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.sv.grupo11.asociacioncomunal.config.AppContext;
import com.sv.grupo11.asociacioncomunal.dto.*;
import com.sv.grupo11.asociacioncomunal.exception.*;
import com.sv.grupo11.asociacioncomunal.model.*;
import com.sv.grupo11.asociacioncomunal.repository.jdbc.JdbcSocioRepository;
import com.sv.grupo11.asociacioncomunal.util.AppProperties;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de integracion contra una base H2 en memoria, usando el DataSource (pool) y los DAO JDBC REALES.
 * Verifican CRUD, restricciones UNIQUE, pagos concurrentes y el cierre mensual concurrente.
 */
class JdbcIntegrationTest {
    private static final YearMonth PERIODO = YearMonth.of(2026, 10);
    private static final LocalDate CORTE = LocalDate.of(2026, 10, 12);

    private AppContext ctx;

    @BeforeEach
    void iniciar() {
        System.setProperty("db.url", "jdbc:h2:mem:it" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        System.setProperty("db.driver", "org.h2.Driver");
        ctx = new AppContext(new AppProperties("application.properties"), true);
    }

    @AfterEach
    void cerrar() {
        ctx.close();
        System.clearProperty("db.url");
        System.clearProperty("db.driver");
    }

    private void loginAdmin() throws CredencialesInvalidasException { ctx.auth.iniciarSesion("admin", "admin123"); }

    // ------------------------------------------------------------------ DataSource / pool
    @Test @DisplayName("DataSourceConfig aplica los parametros del pool documentados")
    void parametrosDelPool() throws Exception {
        org.apache.tomcat.jdbc.pool.DataSource ds = (org.apache.tomcat.jdbc.pool.DataSource) ctx.getDataSource();
        assertEquals("SELECT 1", ds.getValidationQuery());
        assertEquals(3, ds.getInitialSize());
        assertEquals(3, ds.getMinIdle());
        assertEquals(8, ds.getMaxIdle());
        assertEquals(12, ds.getMaxActive());
        assertEquals(10_000, ds.getMaxWait());
        assertTrue(ds.isTestOnBorrow());
        assertFalse(ds.isTestOnReturn());
        assertTrue(ds.isTestWhileIdle());
        assertTrue(ds.isTestOnConnect());
        assertEquals(30_000, ds.getTimeBetweenEvictionRunsMillis());
        assertEquals(60_000, ds.getMinEvictableIdleTimeMillis());
        assertTrue(ds.isRemoveAbandoned());
        assertEquals(60, ds.getRemoveAbandonedTimeout());
        assertEquals(30_000L, ds.getValidationInterval());
        try (Connection con = ds.getConnection()) {
            assertTrue(con.isValid(2));
        }
    }

    @Test @DisplayName("Una BD inaccesible produce ConexionBdException (manejo de errores de conexion)")
    void errorDeConexion() {
        System.setProperty("db.url", "jdbc:h2:tcp://localhost:9/no_existe");
        assertThrows(ConexionBdException.class, () -> new AppContext(new AppProperties("application.properties"), true));
    }

    // ------------------------------------------------------------------ datos de prueba y login
    @Test
    void datosDePruebaYLoginPorRol() throws Exception {
        for (String[] c : new String[][]{{"admin", "admin123", "ADMINISTRADOR"}, {"secretario", "secre123", "SECRETARIO"},
                {"tesorero", "teso123", "TESORERO"}, {"jramirez", "socio123", "SOCIO"}}) {
            Usuario u = ctx.auth.iniciarSesion(c[0], c[1]);
            assertEquals(Rol.valueOf(c[2]), u.getRol());
        }
        assertEquals(2L, ctx.auth.usuarioActual().orElseThrow().getIdSocio());
        assertThrows(CredencialesInvalidasException.class, () -> ctx.auth.iniciarSesion("admin", "incorrecta"));
    }

    @Test
    void socioVeSoloSuCuenta() throws Exception {
        ctx.auth.iniciarSesion("jramirez", "socio123");
        CuentaSocio cuenta = ctx.cuotas.miCuenta();
        assertEquals("Jose Antonio Ramirez", cuenta.socio().getNombre());
        assertEquals(2, cuenta.historial().size());
        assertEquals(0, new BigDecimal("5.00").compareTo(cuenta.totalPendiente()));
        assertThrows(AccesoDenegadoException.class, () -> ctx.socios.buscar(""));
    }

    // ------------------------------------------------------------------ CRUD
    @Test @DisplayName("CRUD de socios: insertar, consultar, actualizar y cambiar estado")
    void crudSocios() throws Exception {
        loginAdmin();
        assertEquals(5, ctx.socios.buscar("").size());

        Socio nuevo = ctx.socios.registrar(new SocioRequest("Pedro Lopez", "09999999-9", "7000-1111", "Calle 1", null, null, null));
        assertNotNull(nuevo.getId());
        assertEquals(1, ctx.socios.buscar("09999999").size());

        Socio actualizado = ctx.socios.actualizarContacto(nuevo.getId(), "7000-2222", "Calle 2");
        assertEquals("7000-2222", actualizado.getTelefono());
        assertEquals("7000-2222", ctx.socios.buscar("Pedro").get(0).getTelefono());

        assertEquals(EstadoSocio.INACTIVO, ctx.socios.cambiarEstado(nuevo.getId()).getEstado());
        assertEquals(EstadoSocio.INACTIVO, ctx.socios.buscar("Pedro").get(0).getEstado());
    }

    @Test
    void directivaSePersisteComoMiembroDirectiva() throws Exception {
        loginAdmin();
        ctx.socios.registrar(new SocioRequest("Luis Vocal", "08888888-8", "7000-3333", "x", null, "Vocal", "2026-2028"));
        Socio leido = ctx.socios.buscar("08888888").get(0);
        assertTrue(leido instanceof MiembroDirectiva);
        assertEquals("Vocal", leido.getCargo());
        assertEquals(0, new BigDecimal("7.50").compareTo(leido.calcularCuotaMensual()));
    }

    @Test
    void duiRepetidoSeRechazaEnNegocioYEnLaBaseDeDatos() throws Exception {
        loginAdmin();
        assertThrows(ReglaNegocioException.class, () ->
                ctx.socios.registrar(new SocioRequest("Copia", "04512378-9", "7000-4444", "x", null, null, null)));
        // aunque se salte el servicio, la restriccion UNIQUE de la BD protege la integridad
        Socio directo = new Socio(null, "Copia", "04512378-9", "7000-4444", "x", LocalDate.now(), EstadoSocio.ACTIVO);
        assertThrows(DuplicateKeyException.class, () -> new JdbcSocioRepository(ctx.getDataSource()).guardar(directo));
    }

    @Test
    void usuarioNuevoPuedeIniciarSesion() throws Exception {
        loginAdmin();
        ctx.usuarios.crear(new NuevoUsuarioRequest("lucia", "clave123", Rol.SOCIO, 3L));
        ctx.auth.cerrarSesion();
        assertEquals(3L, ctx.auth.iniciarSesion("LUCIA", "clave123").getIdSocio());
        assertThrows(AccesoDenegadoException.class, () -> ctx.usuarios.listar());
    }

    // ------------------------------------------------------------------ concurrencia
    @Test @DisplayName("Varios hilos pagan la MISMA cuota: solo uno lo logra (UPDATE atomico)")
    void pagoConcurrenteDeLaMismaCuota() throws Exception {
        loginAdmin();
        long idCuota = ctx.cuotas.cuentaDeSocio(3).pendientes().get(0).getId();
        int hilos = 8;
        ExecutorService ex = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            resultados.add(ex.submit(() -> {
                salida.await();
                try {
                    ctx.cuotas.registrarPago(idCuota, null);
                    return true;
                } catch (ReglaNegocioException e) {
                    return false;
                }
            }));
        }
        salida.countDown();
        int exitosos = 0;
        for (Future<Boolean> f : resultados) if (f.get(20, TimeUnit.SECONDS)) exitosos++;
        ex.shutdown();

        assertEquals(1, exitosos);
        assertTrue(ctx.cuotas.cuentaDeSocio(3).historial().stream().filter(c -> c.getId() == idCuota).findFirst().orElseThrow().estaPagada());
    }

    @Test @DisplayName("Cierre mensual: genera cuotas y audita morosidad en paralelo contra la BD")
    void cierreMensualConcurrente() throws Exception {
        ctx.auth.iniciarSesion("tesorero", "teso123");

        ResultadoCierre r = ctx.cierre.ejecutar(PERIODO, CORTE);

        assertEquals(5, r.cuotasGeneradas());       // 5 socios activos
        assertEquals(5, r.cuotasVencidas());        // pendientes de jul/ago con vencimiento anterior al corte
        assertEquals("tesorero", r.ejecutadoPor());
        assertEquals(r.periodo(), ctx.cierre.ultimo().orElseThrow().periodo());

        ctx.auth.iniciarSesion("admin", "admin123");
        List<MorosoDTO> morosos = ctx.reportes.morosos();
        assertEquals(3, morosos.size());
        assertEquals(java.util.Set.of(2L, 3L, 5L),
                morosos.stream().map(m -> m.socio().getId()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(0, new BigDecimal("10.00").compareTo(morosos.get(0).montoAdeudado()));   // orden descendente
        assertEquals(0, new BigDecimal("5.00").compareTo(morosos.get(2).montoAdeudado()));

        Cuota nueva = ctx.cuotas.cuentaDeSocio(1).historial().stream()
                .filter(c -> c.getPeriodo().equals(PERIODO)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("7.50").compareTo(nueva.getMonto()));    // presidente: cuota + aporte
        assertEquals(EstadoCuota.PENDIENTE, nueva.getEstado());               // la recien generada NO se marca vencida
    }

    @Test @DisplayName("El cierre es idempotente: repetirlo no duplica cuotas")
    void cierreRepetidoNoDuplica() throws Exception {
        loginAdmin();
        ctx.cierre.ejecutar(PERIODO, CORTE);
        ResultadoCierre segundo = ctx.cierre.ejecutar(PERIODO, CORTE);
        assertEquals(0, segundo.cuotasGeneradas());
        assertEquals(0, segundo.cuotasVencidas());
    }

    @Test @DisplayName("Cierre con 40 socios repartidos en lotes y dos cierres simultaneos: sin cuotas duplicadas")
    void cierresSimultaneosNoDuplicanCuotas() throws Exception {
        loginAdmin();
        for (int i = 0; i < 35; i++) {
            String dui = String.format("%08d-%d", 7000000 + i, i % 10);
            ctx.socios.registrar(new SocioRequest("Socio Masivo " + i, dui, "7100-" + String.format("%04d", i), "x", null, null, null));
        }
        Callable<String> intento = () -> {
            try {
                ctx.cierre.ejecutar(YearMonth.of(2026, 11), CORTE);
                return "OK";
            } catch (OperacionEnCursoException e) {
                return "RECHAZADO";
            }
        };
        ExecutorService ex = Executors.newFixedThreadPool(2);
        List<Future<String>> res = ex.invokeAll(List.of(intento, intento));
        boolean algunoOk = false;
        for (Future<String> f : res) algunoOk |= f.get(60, TimeUnit.SECONDS).equals("OK");
        ex.shutdown();
        assertTrue(algunoOk);

        for (Socio s : ctx.socios.buscar("")) {
            long deNoviembre = ctx.cuotas.cuotasDeSocio(s.getId()).stream()
                    .filter(c -> c.getPeriodo().equals(YearMonth.of(2026, 11))).count();
            assertEquals(1, deNoviembre, "socio " + s.getId() + " debe tener exactamente 1 cuota de 2026-11");
        }
    }

    @Test
    void sociosInactivosNoRecibenCuota() throws Exception {
        loginAdmin();
        ctx.socios.cambiarEstado(5);   // Rosa Hernandez pasa a INACTIVO
        ResultadoCierre r = ctx.cierre.ejecutar(PERIODO, CORTE);
        assertEquals(4, r.cuotasGeneradas());
    }
}
