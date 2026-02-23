package org.example.platon.web.config;

import org.example.platon.web.filter.CorrelationIdFilter;
import org.example.platon.web.servlet.HealthServlet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebRegistrationConfig {
    @Bean
    public ServletRegistrationBean<HealthServlet> healthServlet(){
        ServletRegistrationBean<HealthServlet> bean =new ServletRegistrationBean<>(new HealthServlet(), "/health");
        bean.setLoadOnStartup(1);
        return bean;
    }
}
