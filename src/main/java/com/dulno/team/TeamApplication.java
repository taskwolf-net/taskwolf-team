package com.dulno.team;

import com.dulno.core.command.CommandRegistry;
import com.dulno.core.command.CommandTask;
import com.dulno.core.command.implementation.ClearCommand;
import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.error.ErrorRepository;
import com.dulno.core.intro.Intro;
import com.dulno.core.log.Log;
import com.dulno.team.access.cluster.ClusterContextInitializer;
import com.dulno.team.access.entity.EntityContextInitializer;
import com.dulno.team.access.mail.MailContextInitializer;
import com.dulno.team.access.maintenance.MaintenanceContextInitializer;
import com.dulno.team.access.offer.OfferContextInitializer;
import com.dulno.team.access.question.QuestionContextInitializer;
import com.dulno.team.access.sale.SaleContextInitializer;
import com.dulno.team.access.setting.SettingContextInitializer;
import com.dulno.team.access.template.TemplateContextInitializer;
import com.dulno.team.access.ticket.TicketContextInitializer;
import com.dulno.team.access.verification.VerificationContextInitializer;
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
    var errorRepository = injector.getInstance(ErrorRepository.class);
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
      errorRepository.processError(throwable));
    injector.getInstance(DatabaseConnection.class).errorRepository(errorRepository);
    injector.getInstance(Intro.class).print();
    var log = injector.getInstance(Log.class);
    log.info("Initializing Dulno - Team");
    var commandRegistry = CommandRegistry.create();
    registerCommands(commandRegistry, injector);
    new Thread(() -> CommandTask.create(log, errorRepository, commandRegistry)
      .start()).start();
    var application = new SpringApplication(TeamApplication.class);
    application.setDefaultProperties(Collections.singletonMap("server.port",
      injector.getInstance(TeamConfiguration.class).restPort()));
    registerContextInitializers(application, injector);
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

  private static void registerContextInitializers(
    SpringApplication application, Injector injector
  ) {
    application.addInitializers(injector.getInstance(TeamContextInitializer.class));
    application.addInitializers(injector.getInstance(MaintenanceContextInitializer.class));
    application.addInitializers(injector.getInstance(VerificationContextInitializer.class));
    application.addInitializers(injector.getInstance(TicketContextInitializer.class));
    application.addInitializers(injector.getInstance(QuestionContextInitializer.class));
    application.addInitializers(injector.getInstance(SaleContextInitializer.class));
    application.addInitializers(injector.getInstance(EntityContextInitializer.class));
    application.addInitializers(injector.getInstance(OfferContextInitializer.class));
    application.addInitializers(injector.getInstance(MailContextInitializer.class));
    application.addInitializers(injector.getInstance(TemplateContextInitializer.class));
    application.addInitializers(injector.getInstance(SettingContextInitializer.class));
    application.addInitializers(injector.getInstance(ClusterContextInitializer.class));
  }
}
