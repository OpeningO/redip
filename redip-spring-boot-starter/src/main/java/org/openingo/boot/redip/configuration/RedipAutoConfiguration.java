package org.openingo.boot.redip.configuration;

import org.openingo.redip.dictionary.remote.MySQLRemoteDictionary;
import org.openingo.redip.dictionary.remote.RedisRemoteDictionary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(RedipConfigurationProperties.class)
@Import(RemoteDictionaryConfiguration.class)
public class RedipAutoConfiguration {

	@Bean(destroyMethod = "closeResource")
	public MySQLRemoteDictionary mysqlRemoteDictionary(RedipConfigurationProperties properties) {
		return new MySQLRemoteDictionary(properties);
	}

	@Bean(destroyMethod = "closeResource")
	public RedisRemoteDictionary redisRemoteDictionary(RedipConfigurationProperties properties) {
		return new RedisRemoteDictionary(properties);
	}
}
