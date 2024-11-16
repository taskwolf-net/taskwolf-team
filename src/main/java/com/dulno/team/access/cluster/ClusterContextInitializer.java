package com.dulno.team.access.cluster;

import com.dulno.team.hetzner.HetznerRequestFactory;
import com.dulno.team.prometheus.PrometheusConfiguration;
import com.dulno.team.prometheus.PrometheusMetricsFactory;
import com.dulno.team.prometheus.PrometheusRequestFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
  private final PrometheusConfiguration prometheusConfiguration;
  private final PrometheusRequestFactory prometheusRequestFactory;
  private final PrometheusMetricsFactory prometheusMetricsFactory;
  private final HetznerRequestFactory hetznerRequestFactory;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("kubernetesCoreApi", kubernetesCoreApi);
    beanFactory.registerSingleton("kubernetesAppsApi", kubernetesAppsApi);
    beanFactory.registerSingleton("kubernetesBatchApi", kubernetesBatchApi);
    beanFactory.registerSingleton("prometheusConfiguration",
      prometheusConfiguration);
    beanFactory.registerSingleton("prometheusRequestFactory",
      prometheusRequestFactory);
    beanFactory.registerSingleton("prometheusMetricsFactory",
      prometheusMetricsFactory);
    beanFactory.registerSingleton("hetznerRequestFactory", hetznerRequestFactory);
  }
}
