package com.sv.grupo11.asociacioncomunal.util;

import com.sv.grupo11.asociacioncomunal.exception.AsociacionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Lee application.properties del classpath; una propiedad -Dclave=valor tiene prioridad (util en Docker/pruebas). */
public class AppProperties {
    private final Properties props = new Properties();

    public AppProperties(String recurso) {
        try (InputStream in = AppProperties.class.getClassLoader().getResourceAsStream(recurso)) {
            if (in == null) throw new AsociacionException("No se encontro el archivo de configuracion " + recurso);
            props.load(in);
        } catch (IOException e) {
            throw new AsociacionException("No se pudo leer la configuracion " + recurso, e);
        }
    }

    public String getString(String clave, String porDefecto) {
        String valor = System.getProperty(clave, props.getProperty(clave));
        return valor == null ? porDefecto : valor.trim();
    }

    public int getInt(String clave, int porDefecto) {
        return Integer.parseInt(getString(clave, String.valueOf(porDefecto)));
    }

    public long getLong(String clave, long porDefecto) {
        return Long.parseLong(getString(clave, String.valueOf(porDefecto)));
    }

    public boolean getBoolean(String clave, boolean porDefecto) {
        return Boolean.parseBoolean(getString(clave, String.valueOf(porDefecto)));
    }
}
