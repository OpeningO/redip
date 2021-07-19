package org.openingo.redip.dictionary.remote;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.openingo.jdkits.sys.SystemClockKit;
import org.openingo.redip.configuration.RemoteConfiguration;
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

	public MySQLRemoteDictionary(RemoteConfiguration remoteConfiguration) {
		super(remoteConfiguration);
		this.dataSource = this.initDataSource();
	}

	@Override
	public Set<String> getRemoteWords(DictionaryType dictionaryType,
									  String etymology,
									  String domain) {
		log.info("'mysql' remote dictionary get new words from domain '{}' dictionary '{}'", domain, dictionaryType);
		Set<String> words = new HashSet<>();
		try (Connection connection = this.dataSource.getConnection()) {
			String sql = "SELECT word FROM ik_words WHERE domain = ? AND word_type = ?";
			final PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, domain);
			statement.setInt(2, dictionaryType.getType());
			final ResultSet resultSet  = statement.executeQuery();
			while (resultSet.next()) {
				String word = resultSet.getString("word");
				words.add(word);
			}
			log.info("'mysql' remote dictionary append '{}' words.", words.size());
			log.info("'mysql' remote dictionary update dictionary state from domain '{}' dictionary '{}'", domain, dictionaryType);
			this.resetState(connection, domain);
			statement.close();
			resultSet.close();
		} catch (SQLException e) {
			log.error("'mysql' remote dictionary error =>", e);
		}
		return words;
	}

	@Override
	protected void reloadDictionary(IDictionary dictionary,
									DictionaryType dictionaryType,
									String domain) {
		log.info("'mysql' remote dictionary reload dictionary from domain '{}' dictionary '{}'", domain, dictionaryType);
		try (Connection connection = this.dataSource.getConnection()) {
			final boolean reload = this.resetState(connection, domain);
			if (reload) {
				dictionary.reload(dictionaryType);
			}
		} catch (SQLException e) {
			log.error("'mysql' remote dictionary error =>", e);
		}
	}

	protected boolean resetState(Connection connection, String domain) throws SQLException {
		DomainDictState state = this.getState(connection, domain);
		log.info("'mysql' remote dictionary domain '{}' state '{}'", domain, state);
		if (!DomainDictState.NEWLY.equals(state)) {
			return false;
		}
		// 更新 state
		String sql = "UPDATE ik_dict_state SET state = ? WHERE domain = ?";
		final PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setString(1, DomainDictState.NON_NEWLY.state);
		preparedStatement.setString(2, domain);
		log.info("update ik_dict_state sql '{}'", sql);
		preparedStatement.execute();
		preparedStatement.close();
		return true;
	}

	@Override
	protected boolean addWord(DictionaryType dictionaryType, String domain, String... words) {
		log.info("'{}' remote dictionary add new word '{}' for dictionary '{}'", this.etymology(), words, dictionaryType);
		boolean ret = true;
		try (Connection connection = this.dataSource.getConnection()) {
			connection.setAutoCommit(false);
			String sql = "INSERT INTO ik_words(word, word_type, domain) VALUES (?, ?, ?)";
			try (final PreparedStatement statement = connection.prepareStatement(sql)) {
				Integer dictionaryTypeType = dictionaryType.getType();
				for (String word : words) {
					statement.setString(1, word);
					statement.setInt(2, dictionaryTypeType);
					statement.setString(3, domain);
					statement.addBatch();
				}
				// add word
				statement.executeBatch();
				DomainDictState state = this.getState(connection, domain);
				log.info("'mysql' remote dictionary domain '{}' state '{}'", domain, state);
				DomainDictState domainState = null;
				if (DomainDictState.NON_NEWLY.equals(state)) {
					// update state to newly
					sql = "UPDATE ik_dict_state SET state = ? WHERE domain = ?";
					domainState = DomainDictState.NEWLY;
				}
				if (DomainDictState.NOT_FOUND.equals(state)) {
					// insert state to newly
					sql = "INSERT INTO ik_dict_state(state, domain) VALUES(?, ?)";
					domainState = DomainDictState.NEWLY;
				}
				if (Objects.nonNull(domainState)) {
					PreparedStatement preparedStatement = connection.prepareStatement(sql);
					preparedStatement.setString(1, domainState.state);
					preparedStatement.setString(2, domain);
					preparedStatement.execute();
					preparedStatement.close();
				}
			} catch (SQLException e) {
				connection.rollback();
				connection.setAutoCommit(true);
				throw e;
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("'{} add new word '{}' failure '{}'.", this.etymology(), words, e);
			ret = false;
		}
		return ret;
	}

	private DomainDictState getState(Connection connection, String domain) {
		DomainDictState state = DomainDictState.NOT_FOUND;
		String sql = "SELECT state FROM ik_dict_state WHERE domain = ? LIMIT 1";
		try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setString(1, domain);
			try (final ResultSet resultSet = preparedStatement.executeQuery()) {
				if (!resultSet.next()) {
					log.info("Cannot find the `ik_dict_state` for domain '{}' data", domain);
					return state;
				}
				state = DomainDictState.newByState(resultSet.getString("state"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("'{} add get domain '{}' state failure '{}'.", this.etymology(), domain, e);
		}
		return state;
	}

	@Override
	protected void closeResource() {
		if (Objects.isNull(this.dataSource)) {
			return;
		}

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
		RemoteConfiguration.MySQL mysql = this.remoteConfiguration.getMysql();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setJdbcUrl(mysql.getUrl());
		dataSource.setUsername(mysql.getUsername());
		dataSource.setPassword(mysql.getPassword());
		return dataSource;
	}
}
