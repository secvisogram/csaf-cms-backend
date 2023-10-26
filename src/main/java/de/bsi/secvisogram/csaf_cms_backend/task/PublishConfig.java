package de.bsi.secvisogram.csaf_cms_backend.task;

import de.bsi.secvisogram.csaf_cms_backend.config.CsafConfiguration;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableScheduling
@ComponentScan(basePackages = {"de.bsi.secvisogram.csaf_cms_backend.task"})
public class PublishConfig implements SchedulingConfigurer {

  @Autowired
  private CsafConfiguration configuration;
  private static final Logger LOG = LoggerFactory.getLogger(PublishConfig.class);
    
	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {	
		if (this.configuration.getAutoPublish() != null) {
			if (this.configuration.getAutoPublish().isEnabled()) {
				taskRegistrar.setScheduler(taskExecutor());
				taskRegistrar.addCronTask(task(), this.configuration.getAutoPublish().getCron());
				LOG.info("Autopublish activated. Task created with " + this.configuration.getAutoPublish().getCron());
			}
		}
	}
	
    private SecurityContext createSchedulerSecurityContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_publisher", "ROLE_registred");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "PublisherTask",
                "Publisher",
                authorities
        );
        context.setAuthentication(authentication);
 
        return context;
    }

  @Bean
  Runnable task() {
		return new PublishJob();
	}
    
	@Bean
	Executor taskExecutor() {
		ScheduledThreadPoolExecutor delegateExecutor = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().namingPattern("Publish-Job-%d").build());
		SecurityContext schedulerContext = createSchedulerSecurityContext();
        return new DelegatingSecurityContextScheduledExecutorService(delegateExecutor, schedulerContext);
	}
}
