package com.mydomain.ws.notification;


import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class H2ConsoleServletConfig {

    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet() {
        ServletRegistrationBean<JakartaWebServlet> registration =
                new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2-console/*");
        registration.setName("h2Console");
        return registration;
    }
}
