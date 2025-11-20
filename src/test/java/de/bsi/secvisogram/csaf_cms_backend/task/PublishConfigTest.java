package de.bsi.secvisogram.csaf_cms_backend.task;

import static org.junit.jupiter.api.Assertions.*;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafAutoPublishConfiguration;
import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.test.util.ReflectionTestUtils;

public class PublishConfigTest {

  @Test
  void taskBean_returnsPublishJob() {
    PublishConfig cfg = new PublishConfig();
    assertNotNull(cfg.task());
    assertTrue(cfg.task() instanceof PublishJob);
  }

  @Test
  void taskExecutor_appliesSecurityContextToRunnable() throws Exception {
    PublishConfig cfg = new PublishConfig();
    Executor ex = cfg.taskExecutor();
    assertNotNull(ex);
    assertTrue(ex instanceof DelegatingSecurityContextScheduledExecutorService);
    DelegatingSecurityContextScheduledExecutorService svc = (DelegatingSecurityContextScheduledExecutorService) ex;

    try {
      Future<String> f = svc.submit((Callable<String>) () -> {
        var ctx = org.springframework.security.core.context.SecurityContextHolder.getContext();
        var auth = ctx == null ? null : ctx.getAuthentication();
        return auth == null ? null : auth.getName();
      });

      String name = f.get(5, TimeUnit.SECONDS);
      // createSchedulerSecurityContext uses "PublisherTask" as principal name
      assertEquals("PublisherTask", name);
    } finally {
      svc.shutdownNow();
    }
  }

  @Test
  void configureTasks_whenAutoPublishEnabled_callsRegistrarMethods() {
    PublishConfig cfg = new PublishConfig();
    CsafConfiguration csaf = new CsafConfiguration();
    CsafAutoPublishConfiguration ap = new CsafAutoPublishConfiguration().setEnabled(true).setCron("*/5 * * * * *");
    csaf.setAutoPublish(ap);
    ReflectionTestUtils.setField(cfg, "configuration", csaf);

    ScheduledTaskRegistrar registrar = Mockito.mock(ScheduledTaskRegistrar.class);
    cfg.configureTasks(registrar);

    Mockito.verify(registrar).setScheduler(Mockito.any());
    Mockito.verify(registrar).addCronTask(Mockito.any(Runnable.class), Mockito.eq("*/5 * * * * *"));
  }

  @Test
  void configureTasks_whenAutoPublishNull_doesNotCallRegistrar() {
    PublishConfig cfg = new PublishConfig();
    CsafConfiguration csaf = new CsafConfiguration();
    csaf.setAutoPublish(null);
    ReflectionTestUtils.setField(cfg, "configuration", csaf);

    ScheduledTaskRegistrar registrar = Mockito.mock(ScheduledTaskRegistrar.class);
    cfg.configureTasks(registrar);

    Mockito.verify(registrar, Mockito.never()).setScheduler(Mockito.any());
    Mockito.verify(registrar, Mockito.never()).addCronTask(Mockito.any(), Mockito.anyString());
  }
}
