package org.openingo.redip.dictionary.remote;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.openingo.redip.configuration.RedipBaseConfigurationProperties;
import org.openingo.redip.configuration.RedipConfigurationProperties;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.IDictionary;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * RedisRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/14 18:49
 */
@Slf4j
public class RedisRemoteDictionary extends AbstractRemoteDictionary {

	private final StatefulRedisConnection<String, String> redisConnection;

	private final static String KEY_PREFIX = "es-ik-words";

	public RedisRemoteDictionary(RedipBaseConfigurationProperties properties) {
		super(properties);
		this.redisConnection = this.getRedisConnection();
	}

	@Override
	public Set<String> getRemoteWords(IDictionary dictionary,
									  DictionaryType dictionaryType,
									  String etymology,
									  String domain) {
		log.info("'redis' remote dictionary get new words from domain '{}' dictionary '{}'", domain, dictionaryType);
		RedisCommands<String, String> sync = this.redisConnection.sync();
		String key = this.getKey(dictionaryType, domain);
		List<String> words = sync.lrange(key, 0, -1);
		return new HashSet<>(words);
	}

	@Override
	protected void reloadDictionary(IDictionary dictionary,
									DictionaryType dictionaryType,
									String domain) {
		log.info("'redis' remote dictionary reload dictionary from domain '{}' dictionary '{}'", domain, dictionaryType);
		RedisCommands<String, String> sync = this.redisConnection.sync();
		// 当前 对应的 *-state key为true时，进行reload
		String key = this.getKey(dictionaryType, domain);
		String state = this.getStateKey(key);
		String currentState = sync.get(state);
		log.info("[Remote Dict File] state '{}' = '{}'", state, currentState);
		if ("true".equals(currentState)) {
			sync.set(state, "false");
			dictionary.reload(dictionaryType);
		}
	}

	@Override
	protected boolean addWord(DictionaryType dictionaryType, String domain, String word) {
		log.info("'{}' remote dictionary add new word '{}' for dictionary '{}'", this.etymology(), word, dictionaryType);
		RedisCommands<String, String> sync = this.redisConnection.sync();
		sync.multi();
		String key = this.getKey(dictionaryType, domain);
		sync.lpush(key, word);
		String state = this.getStateKey(key);
		sync.set(state, "true");
		TransactionResult transactionResult = sync.exec();
		for (Object txRet : transactionResult) {
			log.info("txRet '{}'", txRet);
		}
		log.info("'{} add new word '{}' success.", this.etymology(), word);
		return true;
	}

	@Override
	protected void closeResource() {
		String etymology = this.etymology();
		log.info("'{}' remote dictionary is closing...", etymology);
		this.redisConnection.close();
		log.info("'{}' remote dictionary is closed", etymology);
	}

	@Override
	protected String etymology() {
		return RemoteDictionaryEtymology.REDIS.getEtymology();
	}

	private String getStateKey(String key) {
		return String.format("%s:state", key);
	}

	private String getKey(DictionaryType dictionaryType, String domain) {
		// # main-words key: es-ik-words:{domain}:main-words
		// # stop-words key: es-ik-words:{domain}:stop-words
		return String.format("%s:%s:%s", KEY_PREFIX, domain, dictionaryType.getDictName());
	}

	private StatefulRedisConnection<String, String> getRedisConnection() {
		RedipConfigurationProperties.Redis redis = this.getRemote().getRedis();
		RedisURI.Builder builder = RedisURI.builder()
				.withHost(redis.getHost())
				.withPort(redis.getPort())
				.withDatabase(redis.getDatabase());
		String username = redis.getUsername();
		String password = redis.getPassword();
		if (Objects.nonNull(username) && Objects.nonNull(password)) {
			builder.withAuthentication(redis.getUsername(), password.toCharArray());
		} else if (Objects.nonNull(password)) {
			builder.withPassword(password.toCharArray());
		}
		return RedisClient.create(builder.build()).connect();
	}
}
