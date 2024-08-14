package net.taskwolf.team;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import net.taskwolf.core.access.AccessInjectionModule;
import net.taskwolf.core.bundle.BundleInjectionModule;
import net.taskwolf.core.command.CommandInjectionModule;
import net.taskwolf.core.database.DatabaseInjectionModule;
import net.taskwolf.core.intro.IntroInjectionModule;
import net.taskwolf.core.locale.LocaleInjectionModule;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.mail.TaskwolfMailConfiguration;
import net.taskwolf.core.offer.OfferInjectionModule;
import net.taskwolf.core.organization.OrganizationInjectionModule;
import net.taskwolf.core.question.QuestionInjectionModule;
import net.taskwolf.core.sale.SaleInjectionModule;
import net.taskwolf.core.stripe.StripeInjectionModule;
import net.taskwolf.core.template.TemplateInjectionModule;
import net.taskwolf.core.ticket.TicketInjectionModule;
import net.taskwolf.core.user.UserInjectionModule;
import net.taskwolf.core.whitelist.WhitelistInjectionModule;
import net.taskwolf.team.configuration.TeamConfiguration;
import net.taskwolf.team.group.GroupInjectionModule;
import net.taskwolf.team.member.TeamMemberInjectionModule;
import net.taskwolf.team.offer.TeamOfferInjectionModule;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@RequiredArgsConstructor(staticName = "create")
public final class TeamInjectionModule extends AbstractModule {
  @Override
  protected void configure() {
    install(IntroInjectionModule.create());
    install(DatabaseInjectionModule.create());
    install(UserInjectionModule.create());
    install(OrganizationInjectionModule.create());
    install(BundleInjectionModule.create());
    install(TicketInjectionModule.create());
    install(QuestionInjectionModule.create());
    install(SaleInjectionModule.create());
    install(WhitelistInjectionModule.create());
    install(LocaleInjectionModule.create());
    install(AccessInjectionModule.create());
    install(StripeInjectionModule.create());
    install(OfferInjectionModule.create());
    install(TemplateInjectionModule.create());
    install(TeamOfferInjectionModule.create());
    install(CommandInjectionModule.create());
    install(TeamMemberInjectionModule.create());
    install(GroupInjectionModule.create());
  }

  @Provides
  @Singleton
  Log provideTeamLog() throws Exception {
    return Log.create("Team", "/logs/");
  }

  @Provides
  @Singleton
  TeamConfiguration provideTeamConfiguration() throws Exception {
    return TeamConfiguration.createAndLoad();
  }

  @Provides
  @Singleton
  @Named("teamKey")
  Key provideTeamKey(TeamConfiguration configuration) {
    return new SecretKeySpec(configuration.secret().getBytes(StandardCharsets.UTF_8),
      SignatureAlgorithm.HS256.getJcaName());
  }

  @Provides
  @Singleton
  @Named("verificationMail")
  TaskwolfMail provideVerificationMail() throws Exception {
    var mailConfiguration = TaskwolfMailConfiguration.createAndLoad("verification");
    return TaskwolfMail.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }

  @Provides
  @Singleton
  @Named("supportMail")
  TaskwolfMail provideSupportMail() throws Exception {
    var mailConfiguration = TaskwolfMailConfiguration.createAndLoad("support");
    return TaskwolfMail.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }

  @Provides
  @Singleton
  @Named("saleMail")
  TaskwolfMail provideSaleMail() throws Exception {
    var mailConfiguration = TaskwolfMailConfiguration.createAndLoad("sale");
    return TaskwolfMail.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }
}
