package org.openingo.redip.dictionary.remote;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.openingo.jdkits.sys.SystemClockKit;
import org.openingo.redip.configuration.RedipBaseConfigurationProperties;
import org.openingo.redip.configuration.RedipConfigurationProperties;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.IDictionary;

import java.sql.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * MySQLRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/14 18:48
 */
@Slf4j
public class MySQLRemoteDictionary extends AbstractRemoteDictionary {

	private final HikariDataSource dataSource;

	public MySQLRemoteDictionary(RedipBaseConfigurationProperties properties) {
		super(properties);
		this.dataSource = this.initDataSource();
	}

	@Override
	public Set<String> getRemoteWords(IDictionary dictionary,
									  DictionaryType dictionaryType,
									  String etymology,
									  String domain) {
		log.info("'mysql' remote dictionary get new words from domain '{}' dictionary '{}'", domain, dictionaryType);
		Set<String> words = new HashSet<>();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			connection = this.dataSource.getConnection();
			statement = connection.prepareStatement("SELECT word FROM ik_words WHERE domain = ? AND word_type = ?");
			statement.setString(1, domain);
			statement.setInt(2, dictionaryType.getType());
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				String word = resultSet.getString("word");
				words.add(word);
			}
			log.info("[Remote DictFile Loading] append {} words.", words.size());
		} catch (SQLException e) {
			log.error(" [Remote DictFile Loading] error =>", e);
		} finally {
			this.closeResources(connection, statement, resultSet);
		}
		return words;
	}

	@Override
	protected void reloadDictionary(IDictionary dictionary,
									DictionaryType dictionaryType,
									String domain) {
		log.info("'mysql' remote dictionary reload dictionary from domain '{}' dictionary '{}'", domain, dictionaryType);
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			connection = this.dataSource.getConnection();
			String sql = "SELECT state FROM ik_dict_state WHERE domain = ? LIMIT 1";
			statement = connection.prepareStatement(sql);
			statement.setString(1, domain);
			resultSet = statement.executeQuery();
			if (!resultSet.next()) {
				log.info("Cannot find the `ik_dict_state` and dictionary '{}' data", domain);
				return;
			}
			String state = resultSet.getString("state");
			log.info("[Remote DictFile] state '{}'", state);
			if ("true".equals(state)) {
				// 更新 state
				sql = String.format("UPDATE ik_dict_state SET state = 'false' WHERE domain = '%s'", domain);
				log.info("update ik_dict_state sql '{}'", sql);
				statement.execute(sql);
				dictionary.reload(dictionaryType);
			}
		} catch (SQLException e) {
			log.error(" [Remote DictFile Reloading] error =>", e);
		} finally {
			this.closeResources(connection, statement, resultSet);
		}
	}

	@Override
	protected boolean addWord(DictionaryType dictionaryType, String domain, String word) {
		log.info("'{}' remote dictionary add new word '{}' for dictionary '{}'", this.etymology(), word, dictionaryType);
		boolean ret = true;
		try (Connection connection = this.dataSource.getConnection()) {
			connection.setAutoCommit(false);
			String sql = "INSERT INTO ik_words(word, word_type, domain, create_time) VALUES (?, ?, ?, ?)";
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setString(1, word);
				statement.setInt(2, dictionaryType.getType());
				statement.setString(3, domain);
				statement.setDate(4, new Date(SystemClockKit.now()));
				// add word
				statement.execute();
				// todo find state
				// update state
				sql = String.format("UPDATE ik_dict_state SET state = 'true' WHERE domain = '%s'", domain);
				statement.execute(sql);
			} catch (SQLException e) {
				connection.rollback();
				connection.setAutoCommit(true);
				throw e;
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("'{} add new word '{}' failure '{}'.", this.etymology(), word, e);
			ret = false;
		}
		return ret;
	}

	private void closeResources(Connection connection, Statement statement, ResultSet resultSet) {
		try {
			if (Objects.nonNull(connection)) {
				connection.close();
			}
			if (Objects.nonNull(statement)) {
				statement.close();
			}
			if (Objects.nonNull(resultSet)) {
				resultSet.close();
			}
		} catch (SQLException e) {
			log.error("[Remote DictFile 'mysql'] closeResources error =>", e);
		}
	}

	@Override
	protected void closeResource() {
		String etymology = this.etymology();
		log.info("'{}' remote dictionary is closing...", etymology);
		this.dataSource.close();
		log.info("'{}' remote dictionary is closed", etymology);
	}

	@Override
	protected String etymology() {
		return RemoteDictionaryEtymology.MYSQL.getEtymology();
	}

	private HikariDataSource initDataSource() {
		HikariDataSource dataSource = new HikariDataSource();
		RedipConfigurationProperties.MySQL mysql = this.getRemote().getMysql();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setJdbcUrl(mysql.getUrl());
		dataSource.setUsername(mysql.getUsername());
		dataSource.setPassword(mysql.getPassword());
		return dataSource;
	}
}
