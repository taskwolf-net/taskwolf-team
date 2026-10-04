package net.taskwolf.team.access.entity;

import net.taskwolf.core.bundle.BundleDatabaseTable;
import net.taskwolf.core.bundle.BundlePresetRepository;
import net.taskwolf.core.organization.OrganizationDatabaseTable;
import net.taskwolf.core.session.SessionDatabaseTable;
import net.taskwolf.core.stripe.StripeDatabaseTable;
import net.taskwolf.core.stripe.TerminationDatabaseTable;
import net.taskwolf.core.user.UserTargetDatabaseTable;
import net.taskwolf.workflow.operation.OperationDatabaseTable;
import net.taskwolf.workflow.throttle.WorkflowThrottleDatabaseTable;
import net.taskwolf.team.distribution.TeamDistributionConfiguration;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.stripe.StripeClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class EntityContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final BundleDatabaseTable bundleDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;
  private final TeamDistributionConfiguration teamDistributionConfiguration;
  private final UserTargetDatabaseTable userTargetDatabaseTable;
  private final OperationDatabaseTable operationDatabaseTable;
  private final WorkflowThrottleDatabaseTable workflowThrottleDatabaseTable;
  private final OrganizationDatabaseTable organizationDatabaseTable;
  private final net.taskwolf.core.organization.team.TeamDatabaseTable organizationTeamDatabaseTable;
  private final SessionDatabaseTable sessionDatabaseTable;
  private final StripeDatabaseTable stripeDatabaseTable;
  private final StripeClient stripeClient;
  private final TerminationDatabaseTable terminationDatabaseTable;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("bundleDatabaseTable", bundleDatabaseTable);
    beanFactory.registerSingleton("bundlePresetRepository",
      bundlePresetRepository);
    beanFactory.registerSingleton("teamDistributionConfiguration",
      teamDistributionConfiguration);
    beanFactory.registerSingleton("userTargetDatabaseTable",
      userTargetDatabaseTable);
    beanFactory.registerSingleton("operationDatabaseTable", operationDatabaseTable);
    beanFactory.registerSingleton("workflowThrottleDatabaseTable",
      workflowThrottleDatabaseTable);
    beanFactory.registerSingleton("organizationDatabaseTable",
      organizationDatabaseTable);
    beanFactory.registerSingleton("organizationTeamDatabaseTable",
      organizationTeamDatabaseTable);
    beanFactory.registerSingleton("sessionDatabaseTable", sessionDatabaseTable);
    beanFactory.registerSingleton("stripeDatabaseTable", stripeDatabaseTable);
    beanFactory.registerSingleton("stripeClient", stripeClient);
    beanFactory.registerSingleton("terminationDatabaseTable",
      terminationDatabaseTable);
  }
}
