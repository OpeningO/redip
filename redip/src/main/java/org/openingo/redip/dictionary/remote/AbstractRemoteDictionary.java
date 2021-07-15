package org.openingo.redip.dictionary.remote;

import org.openingo.redip.configuration.RedipBaseConfigurationProperties;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.dictionary.IDictionary;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * AbstractRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/14 18:49
 */
public abstract class AbstractRemoteDictionary {

	private final RedipBaseConfigurationProperties properties;

	AbstractRemoteDictionary(RedipBaseConfigurationProperties properties) {
		this.properties = properties;
	}

	RedipBaseConfigurationProperties.Remote getRemote() {
		return this.properties.getRemote();
	}

	/**
	 * 获取远程词库
	 * @param dictionary 词典
	 * @param dictionaryType 词典类型
	 * @param domainUri 领域词源Uri
	 * @return words
	 */
	public Set<String> getRemoteWords(IDictionary dictionary,
									  DictionaryType dictionaryType,
									  URI domainUri) {
		return this.getRemoteWords(dictionary,
				dictionaryType,
				domainUri.getScheme(),
				domainUri.getAuthority());
	}

	/**
	 * 获取远程词库
	 * @param dictionary 词典
	 * @param dictionaryType 词典类型
	 * @param etymology 词源
	 * @param domain 领域
	 * @return words
	 */
	public Set<String> getRemoteWords(IDictionary dictionary,
									  DictionaryType dictionaryType,
									  String etymology,
									  String domain) {
		return Collections.emptySet();
	}

	/**
	 * 重新加载词库
	 * @param dictionary 词典
	 * @param dictionaryType 词典类型
	 * @param domainUri 领域词源Uri
	 */
	protected void reloadDictionary(IDictionary dictionary,
									DictionaryType dictionaryType,
									URI domainUri) {
		this.reloadDictionary(dictionary,
				dictionaryType,
				domainUri.getAuthority());
	}

	/**
	 * 重新加载词库
	 * @param dictionary 词典
	 * @param dictionaryType 词典类型
	 * @param domain 领域
	 */
	protected void reloadDictionary(IDictionary dictionary,
									DictionaryType dictionaryType,
									String domain) {

	}


	/**
	 * 添加一个主词
	 * @param domain 业务
	 * @param word 新词
	 * @return true成功
	 */
	public boolean addMainWord(String domain, String word) {
		synchronized (this) {
			return addWord(DictionaryType.MAIN_WORDS, domain, word);
		}
	}

	/**
	 * 添加一个stop词
	 * @param domain 业务
	 * @param word 新词
	 * @return true成功
	 */
	public boolean addStopWord(String domain, String word) {
		synchronized (this) {
			return addWord(DictionaryType.STOP_WORDS, domain, word);
		}
	}

	/**
	 * 添加一个新词
	 * @param dictionaryType 词典类型
	 * @param domain 业务
	 * @param word 新词
	 * @return true成功
	 */
	protected abstract boolean addWord(DictionaryType dictionaryType, String domain, String word);

	/**
	 * close resources
	 */
	protected abstract void closeResource();

	/**
	 * 词典词源
	 * @return etymology
	 */
	protected abstract String etymology();
}
