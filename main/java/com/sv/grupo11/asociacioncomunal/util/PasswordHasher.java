package com.sv.grupo11.asociacioncomunal.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash de contrasenas con PBKDF2-HMAC-SHA256 y sal aleatoria. Formato: pbkdf2$iter$sal$hash (Base64). */
public final class PasswordHasher {
    private static final int ITERACIONES = 65_000;
    private static final int LONGITUD_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() { }

    public static String hash(String clave) {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);
        return formatear(ITERACIONES, sal, derivar(clave, sal, ITERACIONES));
    }

    public static boolean verificar(String clave, String almacenado) {
        if (clave == null || almacenado == null) return false;
        String[] partes = almacenado.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2")) return false;
        try {
            int iter = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            // comparacion en tiempo constante
            return MessageDigest.isEqual(esperado, derivar(clave, sal, iter));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(String clave, byte[] sal, int iter) {
        try {
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, iter, LONGITUD_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible en esta JVM", e);
        }
    }

    private static String formatear(int iter, byte[] sal, byte[] hash) {
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + iter + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }
}
