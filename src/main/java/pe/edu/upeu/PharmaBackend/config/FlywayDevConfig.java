package pe.edu.upeu.PharmaBackend.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Solo en desarrollo: antes de migrar, Flyway "repara" las sumas de
 * verificacion guardadas en flyway_schema_history.
 *
 * Motivo: el comentario de cabecera de V3 se edito para no versionar las
 * claves de prueba. Una base creada antes de ese cambio guarda la suma
 * anterior y fallaria la validacion. repair() actualiza la suma sin
 * volver a ejecutar ninguna migracion. En produccion no se activa.
 */
@Configuration
@Profile("dev")
public class FlywayDevConfig {

    @Bean
    public FlywayMigrationStrategy repararYMigrar() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
