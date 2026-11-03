package com.guilherme.entrevistaia.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// A versão da documentação da API segue a do pom (via build-info) em vez de
// um valor fixo; sem build-info cai em "dev".
class OpenApiConfigTest {

    @Test
    void usaAVersaoDoBuildInfo() {
        Properties props = new Properties();
        props.setProperty("version", "9.8.7");
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("buildProperties", new BuildProperties(props));
        ObjectProvider<BuildProperties> provider = beans.getBeanProvider(BuildProperties.class);

        assertThat(new OpenApiConfig().customOpenAPI(provider).getInfo().getVersion()).isEqualTo("9.8.7");
    }

    @Test
    void semBuildInfoFicaDev() {
        ObjectProvider<BuildProperties> provider =
            new StaticListableBeanFactory().getBeanProvider(BuildProperties.class);

        assertThat(new OpenApiConfig().customOpenAPI(provider).getInfo().getVersion()).isEqualTo("dev");
    }
}
