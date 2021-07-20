/*
 * MIT License
 *
 * Copyright (c) 2021 OpeningO Co.,Ltd.
 *
 *    https://openingo.org
 *    contactus(at)openingo.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openingo.redip.dictionary.remote;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.openingo.jdkits.sys.SystemClockKit;
import org.openingo.redip.configuration.RemoteConfiguration;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.IDictionary;

import java.util.*;

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

	public RedisRemoteDictionary(RemoteConfiguration remoteConfiguration) {
		super(remoteConfiguration);
		this.redisConnection = this.getRedisConnection();
	}

	@Override
	public Set<String> getRemoteWords(DictionaryType dictionaryType,
									  String etymology,
									  String domain) {
		log.info("'redis' remote dictionary get new words from domain '{}' dictionary '{}'", domain, dictionaryType);
		RedisCommands<String, String> sync = this.redisConnection.sync();
		String key = this.getKey(dictionaryType, domain);
		List<String> words = sync.zrange(key, 0, -1);
		this.resetState(dictionaryType, domain);
		return new HashSet<>(words);
	}

	@Override
	protected void reloadDictionary(IDictionary dictionary,
									DictionaryType dictionaryType,
									String domain) {
		log.info("'redis' remote dictionary reload dictionary from domain '{}' dictionary '{}'", domain, dictionaryType);
		final boolean reload = this.resetState(dictionaryType, domain);
		if (reload) {
			dictionary.reload(dictionaryType);
		}
	}

	private boolean resetState(DictionaryType dictionaryType, String domain) {
		RedisCommands<String, String> sync = this.redisConnection.sync();
		// 当前 对应的 *-state key为true时，进行reload
		String key = this.getKey(dictionaryType, domain);
		String state = this.getStateKey(key);
		String currentState = sync.get(state);
		final DomainDictState domainDictState = DomainDictState.newByState(currentState);
		log.info("'redis' remote dictionary state '{}' = '{}' for domain '{}'.", state, currentState, domain);
		if (!DomainDictState.NEWLY.equals(domainDictState)) {
			return false;
		}
		sync.set(state, DomainDictState.NON_NEWLY.state);
		return true;
	}

	@Override
	protected boolean addWord(DictionaryType dictionaryType, String domain, String... words) {
		log.info("'redis' remote dictionary add new word '{}' for dictionary '{}'", words, dictionaryType);
		RedisCommands<String, String> sync = this.redisConnection.sync();
		sync.multi();
		String key = this.getKey(dictionaryType, domain);
		List<ScoredValue<String>> scoresAndValues = new ArrayList<>(words.length * 2);
		for (int i = 0; i < words.length; i++) {
			scoresAndValues.add(ScoredValue.just(SystemClockKit.now()*1.0 + i, words[i]));
		}
		sync.zadd(key, scoresAndValues.toArray());
		String state = this.getStateKey(key);
		sync.set(state, DomainDictState.NEWLY.state);
		TransactionResult transactionResult = sync.exec();
		for (Object txRet : transactionResult) {
			log.info("txRet '{}'", txRet);
		}
		log.info("'{} add new word '{}' success.", this.etymology(), words);
		return true;
	}

	@Override
	protected void closeResource() {
		if (Objects.isNull(this.redisConnection) || !this.redisConnection.isOpen()) {
			return;
		}
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
		RemoteConfiguration.Redis redis = this.remoteConfiguration.getRedis();
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
