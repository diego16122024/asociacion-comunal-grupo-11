package com.sv.grupo11.asociacioncomunal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sv.grupo11.asociacioncomunal.TestData;
import com.sv.grupo11.asociacioncomunal.dto.NuevoUsuarioRequest;
import com.sv.grupo11.asociacioncomunal.exception.*;
import com.sv.grupo11.asociacioncomunal.model.Rol;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import com.sv.grupo11.asociacioncomunal.repository.UsuarioRepository;
import com.sv.grupo11.asociacioncomunal.service.impl.AuthServiceImpl;
import com.sv.grupo11.asociacioncomunal.service.impl.UsuarioServiceImpl;
import com.sv.grupo11.asociacioncomunal.util.PasswordHasher;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioYAuthServiceTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SocioRepository socioRepository;
    private UsuarioServiceImpl usuarioService;
    private AuthServiceImpl authService;

    @BeforeEach
    void preparar() {
        usuarioService = new UsuarioServiceImpl(usuarioRepository, socioRepository);
        authService = new AuthServiceImpl(usuarioRepository);
        when(usuarioRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void crearUsuarioGuardaElHashNoLaClave() {
        Usuario u = usuarioService.crear(new NuevoUsuarioRequest("tesorero2", "clave123", Rol.TESORERO, null));

        assertEquals("tesorero2", u.getNombreUsuario());
        assertNotEquals("clave123", u.getClaveHash());
        assertTrue(PasswordHasher.verificar("clave123", u.getClaveHash()));
    }

    @Test
    void claveCortaEsRechazada() {
        assertThrows(ValidacionException.class,
                () -> usuarioService.crear(new NuevoUsuarioRequest("x", "123", Rol.TESORERO, null)));
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void usuarioDuplicadoEsErrorDeNegocio() {
        when(usuarioRepository.existePorNombre("admin")).thenReturn(true);
        assertThrows(ReglaNegocioException.class,
                () -> usuarioService.crear(new NuevoUsuarioRequest("admin", "clave123", Rol.ADMINISTRADOR, null)));
    }

    @Test
    void rolSocioRequiereSocioAsociadoExistente() {
        assertThrows(ValidacionException.class,
                () -> usuarioService.crear(new NuevoUsuarioRequest("juan", "clave123", Rol.SOCIO, null)));
        when(socioRepository.buscarPorId(77L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class,
                () -> usuarioService.crear(new NuevoUsuarioRequest("juan", "clave123", Rol.SOCIO, 77L)));
    }

    @Test
    void rolSocioConSocioValidoSeVincula() {
        when(socioRepository.buscarPorId(2L)).thenReturn(Optional.of(TestData.socio(2, "22222222-2")));
        Usuario u = usuarioService.crear(new NuevoUsuarioRequest("juan", "clave123", Rol.SOCIO, 2L));
        assertEquals(2L, u.getIdSocio());
    }

    @Test
    void autenticarConCredencialesCorrectas() {
        Usuario guardado = new Usuario(1L, "admin", PasswordHasher.hash("admin123"), Rol.ADMINISTRADOR, null);
        when(usuarioRepository.buscarPorNombre("admin")).thenReturn(Optional.of(guardado));

        assertSame(guardado, authService.autenticar(" admin ", "admin123"));
    }

    @Test
    void autenticarConClaveIncorrecta() {
        Usuario guardado = new Usuario(1L, "admin", PasswordHasher.hash("admin123"), Rol.ADMINISTRADOR, null);
        when(usuarioRepository.buscarPorNombre("admin")).thenReturn(Optional.of(guardado));

        assertThrows(CredencialesInvalidasException.class, () -> authService.autenticar("admin", "mala"));
    }

    @Test
    void autenticarUsuarioInexistenteOVacio() {
        when(usuarioRepository.buscarPorNombre(any())).thenReturn(Optional.empty());
        assertThrows(CredencialesInvalidasException.class, () -> authService.autenticar("nadie", "x"));
        assertThrows(CredencialesInvalidasException.class, () -> authService.autenticar("", "x"));
        assertThrows(CredencialesInvalidasException.class, () -> authService.autenticar("admin", null));
    }
}
