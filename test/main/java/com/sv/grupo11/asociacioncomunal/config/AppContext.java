package com.sv.grupo11.asociacioncomunal.config;

import com.sv.grupo11.asociacioncomunal.controller.*;
import com.sv.grupo11.asociacioncomunal.repository.*;
import com.sv.grupo11.asociacioncomunal.repository.jdbc.*;
import com.sv.grupo11.asociacioncomunal.service.*;
import com.sv.grupo11.asociacioncomunal.service.cierre.CierreServiceImpl;
import com.sv.grupo11.asociacioncomunal.service.impl.*;
import com.sv.grupo11.asociacioncomunal.util.AppProperties;
import com.sv.grupo11.asociacioncomunal.util.DatabaseInitializer;
import com.sv.grupo11.asociacioncomunal.util.SesionUsuario;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;

/**
 * Raiz de composicion: el UNICO lugar donde se crean implementaciones concretas y se conectan entre si
 * (inyeccion de dependencias manual). El resto de las clases solo conoce interfaces.
 */
public class AppContext implements AutoCloseable {
    private final DataSource dataSource;
    private final CierreServiceImpl cierreService;

    public final SesionUsuario sesion = new SesionUsuario();
    public final AuthController auth;
    public final SocioController socios;
    public final CuotaController cuotas;
    public final ReporteController reportes;
    public final CierreController cierre;
    public final UsuarioController usuarios;

    public AppContext(AppProperties props, boolean cargarDatosDePrueba) {
        this.dataSource = DataSourceConfig.crear(props);
        DatabaseInitializer.inicializar(dataSource, cargarDatosDePrueba);

        SocioRepository socioRepo = new JdbcSocioRepository(dataSource);
        CuotaRepository cuotaRepo = new JdbcCuotaRepository(dataSource);
        UsuarioRepository usuarioRepo = new JdbcUsuarioRepository(dataSource);
        CierreRepository cierreRepo = new JdbcCierreRepository(dataSource);
        Clock reloj = Clock.systemDefaultZone();

        ExecutorService pool = Executors.newFixedThreadPool(props.getInt("cierre.hilos", 4), new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger(1);
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "cierre-worker-" + n.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
        this.cierreService = new CierreServiceImpl(socioRepo, cuotaRepo, cierreRepo, pool,
                props.getInt("cierre.tamanoLote", 2), props.getLong("cierre.timeoutSegundos", 60), reloj);

        this.auth = new AuthController(new AuthServiceImpl(usuarioRepo), sesion);
        this.socios = new SocioController(new SocioServiceImpl(socioRepo, reloj), sesion);
        this.cuotas = new CuotaController(new CuotaServiceImpl(cuotaRepo, socioRepo, reloj), sesion);
        this.reportes = new ReporteController(new ReporteServiceImpl(socioRepo, cuotaRepo, cierreRepo), sesion);
        this.cierre = new CierreController(cierreService, sesion);
        this.usuarios = new UsuarioController(new UsuarioServiceImpl(usuarioRepo, socioRepo), sesion);
    }

    public DataSource getDataSource() { return dataSource; }

    @Override
    public void close() {
        cierreService.close();
        DataSourceConfig.cerrar(dataSource);
    }
}
