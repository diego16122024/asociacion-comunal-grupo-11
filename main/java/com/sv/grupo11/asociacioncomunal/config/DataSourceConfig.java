package com.sv.grupo11.asociacioncomunal.config;

import com.sv.grupo11.asociacioncomunal.util.AppProperties;
import javax.sql.DataSource;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;

/**
 * Configuracion del pool de conexiones (Tomcat JDBC).
 *
 * <p>Por que un pool: abrir una conexion JDBC es caro (red, autenticacion, sesion). El pool mantiene
 * conexiones abiertas y las presta. En este sistema el cierre mensual ejecuta varias tareas a la vez, y cada una
 * necesita su propia conexion; sin pool habria que abrir/cerrar conexiones en cada operacion.
 *
 * <p>Parametros (valores en application.properties, justificacion junto a cada setter):
 */
public final class DataSourceConfig {

    private DataSourceConfig() { }

    public static DataSource crear(AppProperties p) {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();

        ds.setDriverClassName(p.getString("db.driver", "org.h2.Driver"));
        ds.setUrl(p.getString("db.url", "jdbc:h2:mem:asociacion;MODE=MySQL;DB_CLOSE_DELAY=-1"));
        ds.setUsername(p.getString("db.username", "sa"));
        ds.setPassword(p.getString("db.password", ""));

        // validationQuery: consulta minima y barata que confirma que la conexion sigue viva.
        ds.setValidationQuery(p.getString("pool.validationQuery", "SELECT 1"));

        // initialSize: conexiones abiertas al arrancar. 3 = una por tarea concurrente tipica del cierre
        // (generacion + auditoria) + la del hilo de la interfaz, sin pagar el costo de abrir mas.
        ds.setInitialSize(p.getInt("pool.initialSize", 3));

        // minIdle: minimo de conexiones libres que el pool intenta conservar (evita latencia en picos).
        ds.setMinIdle(p.getInt("pool.minIdle", 3));

        // maxIdle: maximo de conexiones libres; las sobrantes se cierran para no ocupar recursos de la BD.
        ds.setMaxIdle(p.getInt("pool.maxIdle", 8));

        // maxActive: tope de conexiones simultaneas. 12 = 4 hilos del cierre + usuario + margen; protege
        // a la BD de saturarse (la app es de una comunidad pequena, no necesita cientos).
        ds.setMaxActive(p.getInt("pool.maxActive", 12));

        // maxWait (ms): cuanto espera un hilo por una conexion si el pool esta lleno antes de fallar con
        // excepcion (se traduce a ConexionBdException). 10 s: suficiente para operaciones normales.
        ds.setMaxWait(p.getInt("pool.maxWait", 10_000));

        // testOnBorrow: valida la conexion al prestarla. Evita entregar conexiones muertas (BD reiniciada).
        ds.setTestOnBorrow(p.getBoolean("pool.testOnBorrow", true));

        // testOnReturn: validar al devolver duplica el costo; con testOnBorrow ya es innecesario -> false.
        ds.setTestOnReturn(p.getBoolean("pool.testOnReturn", false));

        // testWhileIdle: el hilo de mantenimiento valida las conexiones ociosas y descarta las rotas.
        ds.setTestWhileIdle(p.getBoolean("pool.testWhileIdle", true));

        // testOnConnect: valida cada conexion recien creada, para detectar mala configuracion al inicio.
        ds.setTestOnConnect(p.getBoolean("pool.testOnConnect", true));

        // timeBetweenEvictionRunsMillis: cada cuanto corre el hilo de mantenimiento (30 s).
        ds.setTimeBetweenEvictionRunsMillis(p.getInt("pool.timeBetweenEvictionRunsMillis", 30_000));

        // minEvictableIdleTimeMillis: una conexion ociosa mas de 60 s puede ser cerrada (respetando minIdle).
        ds.setMinEvictableIdleTimeMillis(p.getInt("pool.minEvictableIdleTimeMillis", 60_000));

        // removeAbandoned / removeAbandonedTimeout (s): si un hilo toma una conexion y nunca la cierra
        // (fuga), el pool la recupera pasados 60 s. Las operaciones del sistema duran milisegundos.
        ds.setRemoveAbandoned(p.getBoolean("pool.removeAbandoned", true));
        ds.setRemoveAbandonedTimeout(p.getInt("pool.removeAbandonedTimeout", 60));
        ds.setLogAbandoned(true);

        // validationInterval (ms): no revalidar la MISMA conexion mas de una vez cada 30 s;
        // reduce el costo de testOnBorrow en rafagas de peticiones.
        ds.setValidationInterval(p.getLong("pool.validationInterval", 30_000L));

        return ds;
    }

    /** Cierra el pool y todas sus conexiones (se invoca al salir de la aplicacion). */
    public static void cerrar(DataSource ds) {
        if (ds instanceof DataSourceProxy proxy) {
            proxy.close();
        }
    }
}
