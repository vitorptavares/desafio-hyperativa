package com.hyperativa.desafio.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true", matchIfMissing = true)
public class HttpsConfig {

    @Value("${server.port:8443}")
    private int httpsPort;

    @Value("${server.http-port:8080}")
    private int httpPort;

    /**
     * Adiciona conector HTTP na porta 8080 que redireciona para HTTPS (8443).
     * Garante que nenhuma informacao trafega em texto claro.
     */
    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addAdditionalTomcatConnectors(httpRedirectConnector());
        return factory;
    }

    private Connector httpRedirectConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);
        connector.setRedirectPort(httpsPort);
        return connector;
    }
}
