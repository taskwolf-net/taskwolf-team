package net.taskwolf.team;

import net.taskwolf.core.command.CommandRegistry;
import net.taskwolf.core.command.CommandTask;
import net.taskwolf.core.command.implementation.ClearCommand;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.error.ErrorRepository;
import net.taskwolf.core.intro.Intro;
import net.taskwolf.core.log.Log;
import net.taskwolf.team.access.cluster.ClusterContextInitializer;
import net.taskwolf.team.access.entity.EntityContextInitializer;
import net.taskwolf.team.access.mail.MailContextInitializer;
import net.taskwolf.team.access.maintenance.MaintenanceContextInitializer;
import net.taskwolf.team.access.offer.OfferContextInitializer;
import net.taskwolf.team.access.question.QuestionContextInitializer;
import net.taskwolf.team.access.sale.SaleContextInitializer;
import net.taskwolf.team.access.setting.SettingContextInitializer;
import net.taskwolf.team.access.template.TemplateContextInitializer;
import net.taskwolf.team.access.ticket.TicketContextInitializer;
import net.taskwolf.team.access.verification.VerificationContextInitializer;
import net.taskwolf.team.command.GroupCommand;
import net.taskwolf.team.command.PermissionCommand;
import net.taskwolf.team.command.TeamCommand;
import net.taskwolf.team.configuration.TeamConfiguration;
import net.taskwolf.team.maintenance.TeamMaintenanceSchedule;
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
    log.info("Initializing Taskwolf - Team");
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
    log.info("Successfully booted Taskwolf - Team");
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
