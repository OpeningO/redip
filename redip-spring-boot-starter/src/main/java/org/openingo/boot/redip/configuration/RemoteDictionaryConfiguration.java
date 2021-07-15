package org.openingo.boot.redip.configuration;

import org.openingo.redip.dictionary.remote.AbstractRemoteDictionary;
import org.openingo.redip.dictionary.remote.RemoteDictionary;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * RemoteDictionaryConfiguration
 *
 * @author Qicz
 * @since 2021/7/15 13:48
 */
public class RemoteDictionaryConfiguration implements ApplicationContextAware {

	public RemoteDictionaryConfiguration() {
		RemoteDictionary.initial();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Map<String, AbstractRemoteDictionary> remoteDictionaryMap = applicationContext.getBeansOfType(AbstractRemoteDictionary.class);
		remoteDictionaryMap.values().forEach(RemoteDictionary::addRemoteDictionary);
	}
}
