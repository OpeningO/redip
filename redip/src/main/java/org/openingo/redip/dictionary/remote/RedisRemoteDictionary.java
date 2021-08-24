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
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;
import io.lettuce.core.api.sync.RedisStreamCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lombok.extern.slf4j.Slf4j;
import org.openingo.jdkits.sys.SystemClockKit;
import org.openingo.jdkits.validate.ValidateKit;
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
@SuppressWarnings({"unchecked", "rawtypes"})
public class RedisRemoteDictionary extends AbstractRemoteDictionary {

	private final StatefulRedisConnection<String, String> redisConnection;
	private final StatefulRedisClusterConnection<String, String> redisClusterConnection;

	private final static String KEY_PREFIX = "es-ik-words";

	public RedisRemoteDictionary(RemoteConfiguration remoteConfiguration) {
		super(remoteConfiguration);
		RemoteConfiguration.Redis redis = this.remoteConfiguration.getRedis();
		this.redisClusterConnection = this.getRedisClusterConnection(redis);
		this.redisConnection = this.getRedisConnection(redis);
	}

	@Override
	public Set<String> getRemoteWords(DictionaryType dictionaryType,
									  String etymology,
									  String domain) {
		log.info("'redis' remote dictionary get new words from domain '{}' dictionary '{}'", domain, dictionaryType);
		final RedisSortedSetCommands<String, String> sync = this.getCommands();
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
		final RedisStringCommands<String, String> sync = this.getCommands();
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
		final RedisSortedSetCommands<String, String> sync = this.getCommands();
		String key = this.getKey(dictionaryType, domain);
		List<ScoredValue<String>> scoresAndValues = new ArrayList<>(words.length * 2);
		for (int i = 0; i < words.length; i++) {
			scoresAndValues.add(ScoredValue.just(SystemClockKit.now() * 1.0 + i, words[i]));
		}
		sync.zadd(key, scoresAndValues.toArray());
		String state = this.getStateKey(key);
		((RedisStringCommands)sync).set(state, DomainDictState.NEWLY.state);
		log.info("'{} add new word '{}' success.", this.etymology(), words);
		return true;
	}

	@Override
	protected void closeResource() {
		StatefulConnection<String, String> connection = this.redisClusterConnection;
		if (Objects.isNull(connection)) {
			connection = this.redisConnection;
		}
		if (Objects.isNull(connection) || !connection.isOpen()) {
			return;
		}
		String etymology = this.etymology();
		log.info("'{}' remote dictionary is closing...", etymology);
		connection.close();
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

	private <T> T getCommands() {
		if (Objects.nonNull(this.redisClusterConnection)) {
			return (T)this.redisClusterConnection.sync();
		}
		return (T)this.redisConnection.sync();
	}

	private StatefulRedisClusterConnection<String, String> getRedisClusterConnection(RemoteConfiguration.Redis redis) {
		final RemoteConfiguration.Redis.Cluster cluster = redis.getCluster();
		List<String> nodes = null;
		if (Objects.nonNull(cluster) && ValidateKit.isNotEmpty(nodes = cluster.getNodes())) {
			List<RedisURI> initialUris = new ArrayList<>();
			nodes.stream()
					.filter(ValidateKit::isNotEmpty)
					.forEach(node -> {
						final String[] hostPort = node.split(":");
						if (ValidateKit.isNull(hostPort) || hostPort.length != 2) {
							return;
						}
						initialUris.add(this.getRedisUri(redis, hostPort[0], Integer.parseInt(hostPort[1])));
					});
			if (ValidateKit.isNotEmpty(initialUris)) {
				return RedisClusterClient.create(initialUris).connect();
			}
		}
		return null;
	}

	private StatefulRedisConnection<String, String> getRedisConnection(RemoteConfiguration.Redis redis) {
		if (Objects.nonNull(this.redisClusterConnection)) {
			return null;
		}

		return RedisClient.create(this.getRedisUri(redis, redis.getHost(), redis.getPort())).connect();
	}

	private RedisURI getRedisUri(RemoteConfiguration.Redis redis, String host, Integer port) {
		RedisURI.Builder builder = RedisURI.builder()
				.withHost(host)
				.withPort(port)
				.withSsl(redis.isSsl())
				.withDatabase(redis.getDatabase());
		String username = redis.getUsername();
		String password = redis.getPassword();
		if (Objects.nonNull(username) && Objects.nonNull(password)) {
			builder.withAuthentication(redis.getUsername(), password.toCharArray());
		} else if (Objects.nonNull(password)) {
			builder.withPassword(password.toCharArray());
		}
		return builder.build();
	}
}
