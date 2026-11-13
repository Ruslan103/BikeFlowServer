package com.example.setup;

import com.example.demo.controller.AdminPageController;
import com.example.demo.controller.ApiExceptionHandler;
import com.example.demo.controller.ServerSettingsController;
import com.example.demo.controller.ServerRestartController;
import com.example.demo.controller.ServerLogsController;
import com.example.demo.controller.ServerShutdownController;
import com.example.demo.service.lifecycle.ApplicationRestartService;
import com.example.demo.service.lifecycle.ApplicationShutdownService;
import com.example.demo.service.settings.FileServerSettingsService;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@Import({SetupSecurityConfig.class, SetupAuthController.class, AdminPageController.class,
        ServerSettingsController.class, ServerRestartController.class, ApplicationRestartService.class,
        ServerLogsController.class, ServerShutdownController.class, ApplicationShutdownService.class,
        FileServerSettingsService.class, ApiExceptionHandler.class})
public class SetupApplication {
}
