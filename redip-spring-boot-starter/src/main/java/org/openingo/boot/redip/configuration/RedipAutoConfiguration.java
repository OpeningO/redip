package org.openingo.boot.redip.configuration;

import org.openingo.redip.configuration.RemoteConfiguration;
import org.openingo.redip.dictionary.remote.MySQLRemoteDictionary;
import org.openingo.redip.dictionary.remote.RedisRemoteDictionary;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * RedipAutoConfiguration
 *
 * @author Qicz
 * @since 2021/7/15 10:08
 */
@Configuration
@Import({ RemoteDictionaryConfiguration.class })
public class RedipAutoConfiguration {

	protected final static String CONFIGURATION_PROPERTIES_PREFIX = "openingo.redip";

	@Bean
	@ConfigurationProperties(prefix = RedipAutoConfiguration.CONFIGURATION_PROPERTIES_PREFIX)
	RemoteConfiguration remoteConfiguration() {
		return new RemoteConfiguration();
	}

	@Bean(destroyMethod = "closeResource")
	public MySQLRemoteDictionary mysqlRemoteDictionary(RemoteConfiguration remoteConfiguration) {
		return new MySQLRemoteDictionary(remoteConfiguration);
	}

	@Bean(destroyMethod = "closeResource")
	public RedisRemoteDictionary redisRemoteDictionary(RemoteConfiguration remoteConfiguration) {
		return new RedisRemoteDictionary(remoteConfiguration);
	}
}
