package com.dulno.team;

import com.dulno.core.command.CommandRegistry;
import com.dulno.core.command.CommandTask;
import com.dulno.core.command.implementation.ClearCommand;
import com.dulno.core.intro.Intro;
import com.dulno.core.log.Log;
import com.dulno.team.command.GroupCommand;
import com.dulno.team.command.PermissionCommand;
import com.dulno.team.command.TeamCommand;
import com.dulno.team.configuration.TeamConfiguration;
import com.dulno.team.maintenance.TeamMaintenanceSchedule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

@SpringBootApplication
public class TeamApplication {
  public static void main(String[] args) {
    var injector = Guice.createInjector(TeamInjectionModule.create());
    injector.getInstance(Intro.class).print();
    var log = injector.getInstance(Log.class);
    log.info("Initializing Dulno - Team");
    var commandRegistry = CommandRegistry.create();
    registerCommands(commandRegistry, injector);
    new Thread(() -> CommandTask.create(log, commandRegistry).start()).start();
    var application = new SpringApplication(TeamApplication.class);
    application.setDefaultProperties(Collections.singletonMap("server.port",
      injector.getInstance(TeamConfiguration.class).restPort()));
    application.addInitializers(injector.getInstance(TeamContextInitializer.class));
    application.run(args);
    injector.getInstance(TeamMaintenanceSchedule.class).start();
    log.info("Successfully booted Dulno - Team");
  }

  private static void registerCommands(
    CommandRegistry registry, Injector injector
  ) {
    registry.register(injector.getInstance(ClearCommand.class));
    registry.register(injector.getInstance(TeamCommand.class));
    registry.register(injector.getInstance(GroupCommand.class));
    registry.register(injector.getInstance(PermissionCommand.class));
  }
}
