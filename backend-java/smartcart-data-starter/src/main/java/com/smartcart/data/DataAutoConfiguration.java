package com.smartcart.data;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@AutoConfiguration
@EnableJpaAuditing
@ComponentScan("com.smartcart.data")
public class DataAutoConfiguration {
}
