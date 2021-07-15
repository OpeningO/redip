package org.openingo.redip.dictionary.remote;

import org.openingo.jdkits.validate.AssertKit;
import org.openingo.redip.configuration.RemoteConfiguration;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.dictionary.IDictionary;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/**
 * AbstractRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/14 18:49
 */
public abstract class AbstractRemoteDictionary {

	protected final RemoteConfiguration remoteConfiguration;

	AbstractRemoteDictionary(RemoteConfiguration remoteConfiguration) {
		this.remoteConfiguration = remoteConfiguration;
	}

	/**
	 * 获取远程词库
	 * @param dictionaryType 词典类型
	 * @param domainUri 领域词源Uri
	 * @return words
	 */
	public Set<String> getRemoteWords(DictionaryType dictionaryType,
									  URI domainUri) {
		return this.getRemoteWords(dictionaryType,
				domainUri.getScheme(),
				domainUri.getAuthority());
	}

	/**
	 * 获取远程词库
	 * @param dictionaryType 词典类型
	 * @param etymology 词源
	 * @param domain 领域
	 * @return words
	 */
	public Set<String> getRemoteWords(DictionaryType dictionaryType,
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
	 * 添加主词
	 * @param domain 业务
	 * @param words 新词
	 * @return true成功
	 */
	public boolean addMainWord(String domain, String... words) {
		synchronized (this) {
			return processAddingWords(DictionaryType.MAIN_WORDS, domain, words);
		}
	}

	/**
	 * 添加stop词
	 * @param domain 业务
	 * @param words 新词
	 * @return true成功
	 */
	public boolean addStopWord(String domain, String... words) {
		synchronized (this) {
			return processAddingWords(DictionaryType.STOP_WORDS, domain, words);
		}
	}

	/**
	 * 处理添加新词
	 * @param dictionaryType 词典类型
	 * @param domain 业务
	 * @param words 新词
	 * @return true成功
	 */
	private boolean processAddingWords(DictionaryType dictionaryType, String domain, String... words) {
		AssertKit.notEmpty(words, "the words is 'null' or 'empty'.");
		return this.addWord(dictionaryType, domain, words);
	}
	/**
	 * 添加新词
	 * @param dictionaryType 词典类型
	 * @param domain 业务
	 * @param words 新词
	 * @return true成功
	 */
	protected abstract boolean addWord(DictionaryType dictionaryType, String domain, String... words);

	/**
	 * close resources
	 */
	protected abstract void closeResource();

	/**
	 * 词典词源
	 * @return etymology
	 */
	protected abstract String etymology();

	/**
	 * domain dict state
	 */
	protected enum DomainDictState {

		/**
		 * 存在新词
		 */
		NEWLY("newly"),

		/**
		 * 不存在新词
		 */
		NON_NEWLY("non-newly"),

		/**
		 * 没有找到
		 */
		NOT_FOUND("not-found");

		String state;

		DomainDictState(String state) {
			this.state = state;
		}

		static DomainDictState newByState(String state) {
			return Stream.of(values()).filter(s -> s.state.equals(state)).findFirst().orElse(DomainDictState.NOT_FOUND);
		}
	}
}
