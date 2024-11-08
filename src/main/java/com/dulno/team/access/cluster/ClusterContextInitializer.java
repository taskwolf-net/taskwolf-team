package com.dulno.team.access.cluster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
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

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("kubernetesCoreApi", kubernetesCoreApi);
    beanFactory.registerSingleton("kubernetesAppsApi", kubernetesAppsApi);
  }
}
