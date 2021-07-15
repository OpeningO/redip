package org.openingo.boot.redip.configuration;

import lombok.Data;
import org.openingo.redip.configuration.RedipBaseConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RedipConfigurationProperties
 *
 * @author Qicz
 * @since 2021/7/15 10:08
 */
@Data
@ConfigurationProperties(prefix = RedipConfigurationProperties.CONFIGURATION_PROPERTIES_PREFIX)
public class RedipConfigurationProperties extends RedipBaseConfigurationProperties {

	protected final static String CONFIGURATION_PROPERTIES_PREFIX = "openingo.redip";

	private Remote remote = new Remote();

	@Override
	public Remote getRemote() {
		return remote;
	}
}
