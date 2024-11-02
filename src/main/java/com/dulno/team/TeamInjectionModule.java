package com.dulno.team;

import com.dulno.core.access.AccessInjectionModule;
import com.dulno.core.bundle.BundleInjectionModule;
import com.dulno.core.command.CommandInjectionModule;
import com.dulno.core.database.DatabaseInjectionModule;
import com.dulno.core.intro.IntroInjectionModule;
import com.dulno.core.locale.LocaleInjectionModule;
import com.dulno.core.log.Log;
import com.dulno.core.mail.Mail;
import com.dulno.core.mail.MailConfiguration;
import com.dulno.core.mail.MailFactory;
import com.dulno.core.mail.MailInjectionModule;
import com.dulno.core.maintenance.MaintenanceInjectionModule;
import com.dulno.core.offer.OfferInjectionModule;
import com.dulno.core.organization.OrganizationInjectionModule;
import com.dulno.core.question.QuestionInjectionModule;
import com.dulno.core.sale.SaleInjectionModule;
import com.dulno.core.session.SessionInjectionModule;
import com.dulno.core.stripe.StripeInjectionModule;
import com.dulno.core.template.TemplateInjectionModule;
import com.dulno.core.ticket.TicketInjectionModule;
import com.dulno.core.user.UserInjectionModule;
import com.dulno.core.whitelist.WhitelistInjectionModule;
import com.dulno.core.workflow.WorkflowInjectionModule;
import com.dulno.team.configuration.TeamConfiguration;
import com.dulno.team.distribution.TeamDistributionConfiguration;
import com.dulno.team.group.GroupInjectionModule;
import com.dulno.team.member.TeamMemberInjectionModule;
import com.dulno.team.offer.TeamOfferInjectionModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;

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
    install(SessionInjectionModule.create());
    install(OrganizationInjectionModule.create());
    install(MailInjectionModule.create());
    install(BundleInjectionModule.create());
    install(TicketInjectionModule.create());
    install(QuestionInjectionModule.create());
    install(SaleInjectionModule.create());
    install(WhitelistInjectionModule.create());
    install(LocaleInjectionModule.create());
    install(WorkflowInjectionModule.create());
    install(AccessInjectionModule.create());
    install(StripeInjectionModule.create());
    install(OfferInjectionModule.create());
    install(TemplateInjectionModule.create());
    install(TeamOfferInjectionModule.create());
    install(CommandInjectionModule.create());
    install(TeamMemberInjectionModule.create());
    install(GroupInjectionModule.create());
    install(MaintenanceInjectionModule.create());
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
  TeamDistributionConfiguration provideTeamDistributionConfiguration() throws Exception {
    return TeamDistributionConfiguration.createAndLoad();
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
  Mail provideVerificationMail(MailFactory mailFactory) throws Exception {
    var mailConfiguration = MailConfiguration.createAndLoad("verification");
    return mailFactory.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }

  @Provides
  @Singleton
  @Named("supportMail")
  Mail provideSupportMail(MailFactory mailFactory) throws Exception {
    var mailConfiguration = MailConfiguration.createAndLoad("support");
    return mailFactory.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }

  @Provides
  @Singleton
  @Named("saleMail")
  Mail provideSaleMail(MailFactory mailFactory) throws Exception {
    var mailConfiguration = MailConfiguration.createAndLoad("sale");
    return mailFactory.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }

  @Provides
  @Singleton
  @Named("changeMail")
  Mail provideChangeMail(MailFactory mailFactory) throws Exception {
    var mailConfiguration = MailConfiguration.createAndLoad("change");
    return mailFactory.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }
}
