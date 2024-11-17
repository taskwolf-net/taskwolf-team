package com.dulno.team.access.mail;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.error.ErrorRepository;
import com.dulno.core.mail.*;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.UUID;

@RestController
public final class MailModificationController extends TeamController {
  private final OutgoingMailDatabaseTable mailDatabaseTable;
  private final MailFactory mailFactory;
  private final ErrorRepository errorRepository;

  private MailModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    OutgoingMailDatabaseTable mailDatabaseTable, MailFactory mailFactory,
    ErrorRepository errorRepository
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.mailDatabaseTable = mailDatabaseTable;
    this.mailFactory = mailFactory;
    this.errorRepository = errorRepository;
  }

  @RequestMapping(path = "/mail/resend/", method = RequestMethod.POST)
  public void resendMail(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var mailId = body.getUUID("mail");
    memberHasPermission(findMemberId(request), "mail.resend")
      .thenAccept(hasPermission -> mailDatabaseTable.outgoingMailExists(mailId)
        .thenAccept(exists -> resendMail(mailId, hasPermission, exists)));
  }

  private void resendMail(
    UUID mailId, boolean hasPermission, boolean mailExists
  ) {
    if (!hasPermission || !mailExists) {
      return;
    }
    mailDatabaseTable.findOutgoingMail(mailId).thenAccept(this::resendMail);
  }

  private void resendMail(OutgoingMail outgoingMail) {
    try {
      var mail = createMail(outgoingMail.sender());
      mail.resendEmail(outgoingMail);
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }

  private Mail createMail(String sender) throws Exception {
    var mailConfiguration = MailConfiguration.createAndLoad(sender.split("@")[0]);
    return mailFactory.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }
}