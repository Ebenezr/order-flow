package com.blind.orderflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@Configuration
public class SecurityUserConfig {

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {

        UserDetails admin = User.withUsername("admin")
                .password("{noop}admin123")
                .roles("ADMIN")
                .build();

        UserDetails kitchen = User.withUsername("kitchen")
                .password("{noop}kitchen123")
                .roles("KITCHEN")
                .build();

        UserDetails cashier = User.withUsername("cashier")
                .password("{noop}cashier123")
                .roles("CASHIER")
                .build();

        return new MapReactiveUserDetailsService(admin, kitchen, cashier);
    }
}