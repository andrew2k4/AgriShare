package com.gaming.agricul.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables @Async (used to send e-mails off the request thread). */
@Configuration
@EnableAsync
public class AsyncConfig {
}
