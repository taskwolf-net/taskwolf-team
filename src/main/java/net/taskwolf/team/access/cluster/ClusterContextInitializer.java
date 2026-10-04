package net.taskwolf.team.access.cluster;

import net.taskwolf.core.error.ErrorDatabaseTable;
import net.taskwolf.team.cluster.ClusterConfiguration;
import net.taskwolf.team.error.TeamErrorConfiguration;
import net.taskwolf.team.hetzner.HetznerRequestFactory;
import net.taskwolf.team.telegraf.TelegrafConfiguration;
import net.taskwolf.team.telegraf.TelegrafMetricsFactory;
import net.taskwolf.team.telegraf.TelegrafRequestFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.Copy;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class ClusterContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final CoreV1Api kubernetesCoreApi;
  private final AppsV1Api kubernetesAppsApi;
  private final BatchV1Api kubernetesBatchApi;
  private final Exec kubernetesExecutionApi;
  private final Copy kubernetesCopyApi;
  private final ClusterConfiguration clusterConfiguration;
  private final TelegrafConfiguration telegrafConfiguration;
  private final TelegrafRequestFactory telegrafRequestFactory;
  private final TelegrafMetricsFactory telegrafMetricsFactory;
  private final HetznerRequestFactory hetznerRequestFactory;
  private final ErrorDatabaseTable errorDatabaseTable;
  private final TeamErrorConfiguration teamErrorConfiguration;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("kubernetesCoreApi", kubernetesCoreApi);
    beanFactory.registerSingleton("kubernetesAppsApi", kubernetesAppsApi);
    beanFactory.registerSingleton("kubernetesBatchApi", kubernetesBatchApi);
    beanFactory.registerSingleton("kubernetesExecutionApi", kubernetesExecutionApi);
    beanFactory.registerSingleton("kubernetesCopyApi", kubernetesCopyApi);
    beanFactory.registerSingleton("clusterConfiguration", clusterConfiguration);
    beanFactory.registerSingleton("telegrafConfiguration",
      telegrafConfiguration);
    beanFactory.registerSingleton("telegrafRequestFactory",
      telegrafRequestFactory);
    beanFactory.registerSingleton("telegrafMetricsFactory",
      telegrafMetricsFactory);
    beanFactory.registerSingleton("hetznerRequestFactory", hetznerRequestFactory);
    beanFactory.registerSingleton("errorDatabaseTable", errorDatabaseTable);
    beanFactory.registerSingleton("teamErrorConfiguration", teamErrorConfiguration);
  }
}
