package br.com.imovelconecte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import br.com.imovelconecte.config.HubSpotConfig;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(HubSpotConfig.class)
public class ImovelConecteApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImovelConecteApplication.class, args);
    }
}
