package com.sv.grupo11.asociacioncomunal;

import com.sv.grupo11.asociacioncomunal.config.AppContext;
import com.sv.grupo11.asociacioncomunal.exception.AsociacionException;
import com.sv.grupo11.asociacioncomunal.util.AppProperties;
import com.sv.grupo11.asociacioncomunal.view.ConsoleView;
import java.util.Scanner;

/** Punto de entrada. Uso: java -jar asociacion-comunal.jar   (opcional: --sin-datos para no cargar datos de prueba) */
public class App {
    public static void main(String[] args) {
        boolean cargarDatos = true;
        for (String a : args) if ("--sin-datos".equals(a)) cargarDatos = false;

        try (AppContext ctx = new AppContext(new AppProperties("application.properties"), cargarDatos);
             Scanner in = new Scanner(System.in)) {
            new ConsoleView(ctx, in, System.out).ejecutar();
        } catch (AsociacionException e) {
            System.err.println("No se pudo iniciar la aplicacion: " + e.getMessage());
            if (e.getCause() != null) System.err.println("Causa: " + e.getCause().getMessage());
            System.exit(1);
        }
    }
}
