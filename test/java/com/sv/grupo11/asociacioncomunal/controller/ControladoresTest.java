package com.sv.grupo11.asociacioncomunal.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.sv.grupo11.asociacioncomunal.TestData;
import com.sv.grupo11.asociacioncomunal.dto.ResultadoCierre;
import com.sv.grupo11.asociacioncomunal.dto.SocioRequest;
import com.sv.grupo11.asociacioncomunal.exception.AccesoDenegadoException;
import com.sv.grupo11.asociacioncomunal.exception.CierreMensualException;
import com.sv.grupo11.asociacioncomunal.exception.CredencialesInvalidasException;
import com.sv.grupo11.asociacioncomunal.exception.RecursoNoEncontradoException;
import com.sv.grupo11.asociacioncomunal.model.Rol;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import com.sv.grupo11.asociacioncomunal.service.*;
import com.sv.grupo11.asociacioncomunal.util.SesionUsuario;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Los controladores se prueban con la sesion REAL y los servicios simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ControladoresTest {
    @Mock private SocioService socioService;
    @Mock private CuotaService cuotaService;
    @Mock private CierreService cierreService;
    @Mock private UsuarioService usuarioService;
    @Mock private AuthService authService;
    @Mock private ReporteService reporteService;

    private SesionUsuario sesion;
    private SocioController socios;
    private CuotaController cuotas;
    private CierreController cierre;
    private UsuarioController usuarios;
    private AuthController auth;
    private ReporteController reportes;

    @BeforeEach
    void preparar() {
        sesion = new SesionUsuario();
        socios = new SocioController(socioService, sesion);
        cuotas = new CuotaController(cuotaService, sesion);
        cierre = new CierreController(cierreService, sesion);
        usuarios = new UsuarioController(usuarioService, sesion);
        auth = new AuthController(authService, sesion);
        reportes = new ReporteController(reporteService, sesion);
    }

    private void entrarComo(Rol rol, Long idSocio) { sesion.iniciar(TestData.usuario(rol, idSocio)); }

    private static SocioRequest solicitud() {
        return new SocioRequest("Ana", "04512378-9", "7412-3698", "x", null, null, null);
    }

    @Test
    void secretarioPuedeRegistrarSocios() {
        entrarComo(Rol.SECRETARIO, null);
        socios.registrar(solicitud());
        verify(socioService).registrar(any());
    }

    @Test
    void tesoreroNoPuedeRegistrarSociosYElServicioNoSeInvoca() {
        entrarComo(Rol.TESORERO, null);
        assertThrows(AccesoDenegadoException.class, () -> socios.registrar(solicitud()));
        verify(socioService, never()).registrar(any());
    }

    @Test
    void soloElAdministradorCambiaElEstadoDeUnSocio() {
        entrarComo(Rol.SECRETARIO, null);
        assertThrows(AccesoDenegadoException.class, () -> socios.cambiarEstado(1));
        entrarComo(Rol.ADMINISTRADOR, null);
        socios.cambiarEstado(1);
        verify(socioService).cambiarEstado(1L);
    }

    @Test
    void sinSesionNingunaOperacionEsPermitida() {
        assertThrows(AccesoDenegadoException.class, () -> socios.buscar(""));
        assertThrows(AccesoDenegadoException.class, () -> cuotas.miCuenta());
        assertThrows(AccesoDenegadoException.class, () -> reportes.morosos());
    }

    @Test
    void tesoreroRegistraPagos() {
        entrarComo(Rol.TESORERO, null);
        cuotas.registrarPago(5, LocalDate.of(2026, 10, 1));
        verify(cuotaService).registrarPago(5L, LocalDate.of(2026, 10, 1));
    }

    @Test
    void secretarioNoPuedeVerFinanzasNiMorosos() {
        entrarComo(Rol.SECRETARIO, null);
        assertThrows(AccesoDenegadoException.class, () -> cuotas.registrarPago(5, null));
        assertThrows(AccesoDenegadoException.class, () -> reportes.morosos());
        verify(cuotaService, never()).registrarPago(anyLong(), any());
    }

    @Test
    void miCuentaUsaElSocioAsociadoAlUsuario() {
        entrarComo(Rol.SOCIO, 2L);
        cuotas.miCuenta();
        verify(cuotaService).cuentaDe(2L);
    }

    @Test
    void miCuentaSinSocioAsociadoEsError() {
        entrarComo(Rol.SOCIO, null);
        assertThrows(RecursoNoEncontradoException.class, () -> cuotas.miCuenta());
    }

    @Test
    void unSocioNoPuedeConsultarLaCuentaDeOtro() {
        entrarComo(Rol.SOCIO, 2L);
        assertThrows(AccesoDenegadoException.class, () -> cuotas.cuentaDeSocio(3));
    }

    @Test
    void tesoreroEjecutaElCierreIdentificandoseConSuNombre() throws CierreMensualException {
        entrarComo(Rol.TESORERO, null);
        ResultadoCierre esperado = new ResultadoCierre(YearMonth.of(2026, 10), 4, 1, 12, LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 12), "tesorero");
        when(cierreService.ejecutarCierre(any(), any(), any())).thenReturn(esperado);

        ResultadoCierre r = cierre.ejecutar(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 12));

        assertSame(esperado, r);
        verify(cierreService).ejecutarCierre(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 12), "tesorero");
    }

    @Test
    void secretarioNoPuedeEjecutarElCierre() throws CierreMensualException {
        entrarComo(Rol.SECRETARIO, null);
        assertThrows(AccesoDenegadoException.class, () -> cierre.ejecutar(YearMonth.of(2026, 10), LocalDate.now()));
        verify(cierreService, never()).ejecutarCierre(any(), any(), any());
    }

    @Test
    void gestionDeUsuariosSoloParaAdministrador() {
        entrarComo(Rol.TESORERO, null);
        assertThrows(AccesoDenegadoException.class, () -> usuarios.listar());
        entrarComo(Rol.ADMINISTRADOR, null);
        when(usuarioService.listar()).thenReturn(List.of());
        assertTrue(usuarios.listar().isEmpty());
    }

    @Test
    void loginExitosoGuardaElUsuarioEnLaSesion() throws CredencialesInvalidasException {
        Usuario admin = TestData.usuario(Rol.ADMINISTRADOR, null);
        when(authService.autenticar("admin", "admin123")).thenReturn(admin);

        auth.iniciarSesion("admin", "admin123");

        assertEquals(Rol.ADMINISTRADOR, auth.usuarioActual().orElseThrow().getRol());
        auth.cerrarSesion();
        assertTrue(auth.usuarioActual().isEmpty());
    }

    @Test
    void loginFallidoNoCreaSesion() throws CredencialesInvalidasException {
        when(authService.autenticar(any(), any())).thenThrow(new CredencialesInvalidasException("mal"));

        assertThrows(CredencialesInvalidasException.class, () -> auth.iniciarSesion("admin", "x"));
        assertTrue(auth.usuarioActual().isEmpty());
    }
}
