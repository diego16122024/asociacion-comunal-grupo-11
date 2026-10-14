package com.sv.grupo11.asociacioncomunal.util;

import java.util.ArrayList;
import java.util.List;

/** Utilidades genericas sobre colecciones. */
public final class Listas {
    private Listas() { }

    /** Divide una lista en sublistas de a lo sumo "tamano" elementos (para repartir trabajo entre hilos). */
    public static <T> List<List<T>> partir(List<T> origen, int tamano) {
        if (tamano < 1) throw new IllegalArgumentException("El tamano del lote debe ser >= 1");
        List<List<T>> lotes = new ArrayList<>();
        for (int i = 0; i < origen.size(); i += tamano) {
            lotes.add(List.copyOf(origen.subList(i, Math.min(i + tamano, origen.size()))));
        }
        return lotes;
    }
}
