package com.oa.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseProfileConfigTest {

    @Test
    void allDatabaseProfilesShouldBeValidYaml() {
        String[] profiles = {"mysql", "mariadb", "postgresql", "sqlite", "h2", "sqlserver", "oracle", "dm", "kingbase"};
        for (String profile : profiles) {
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(new ClassPathResource("application-" + profile + ".yml"));
            Properties properties = factory.getObject();
            assertNotNull(properties, profile + " profile did not parse");
            assertTrue(properties.containsKey("spring.datasource.url"), profile + " is missing spring.datasource.url");
            assertTrue(properties.containsKey("spring.sql.init.schema-locations"), profile + " is missing schema location");
        }
    }
}