package net.taskwolf.team.access.stripe;

import com.stripe.StripeClient;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.taskwolf.core.organization.Organization;
import net.taskwolf.core.organization.OrganizationDatabaseTable;
import net.taskwolf.core.stripe.StripeDatabaseTable;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;

import java.security.Key;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Accessors(fluent = true)
public class StripeController extends TeamController {
  private final StripeDatabaseTable stripeDatabaseTable;
  private final StripeClient stripeClient;
  private final UserDatabaseTable userDatabaseTable;
  private final OrganizationDatabaseTable organizationDatabaseTable;

  protected StripeController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    StripeDatabaseTable stripeDatabaseTable,
    StripeClient stripeClient, UserDatabaseTable userDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.stripeDatabaseTable = stripeDatabaseTable;
    this.stripeClient = stripeClient;
    this.userDatabaseTable = userDatabaseTable;
    this.organizationDatabaseTable = organizationDatabaseTable;
  }

  protected CompletableFuture<UUID> findStripeTarget(UUID entityId) {
    return userDatabaseTable.userExists(entityId)
      .thenCompose(userExist -> findStripeTargetUser(entityId, userExist));
  }

  private CompletableFuture<UUID> findStripeTargetUser(
    UUID entityId, boolean userExist
  ) {
    if (!userExist) {
      return organizationDatabaseTable.organizationExists(entityId)
        .thenCompose(organizationExists ->
          findStripeTargetOrganization(entityId, organizationExists));
    }
    return CompletableFuture.completedFuture(entityId);
  }

  private CompletableFuture<UUID> findStripeTargetOrganization(
    UUID entityId, boolean organizationExists
  ) {
    if (!organizationExists) {
      return CompletableFuture.completedFuture(null);
    }
    return organizationDatabaseTable.findOrganization(entityId)
      .thenApply(Organization::owner);
  }

}
