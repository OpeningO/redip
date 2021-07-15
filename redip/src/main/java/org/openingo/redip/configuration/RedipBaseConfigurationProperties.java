package org.openingo.redip.configuration;

import lombok.Data;

/**
 * RedipBaseConfigurationProperties
 *
 * @author Qicz
 * @since 2021/7/14 18:32
 */
@Data
public abstract class RedipBaseConfigurationProperties {

	/**
	 * 获取 remote 配置
	 * @return remote 配置信息
	 */
	public abstract Remote getRemote();

	@Data
	public static class Remote {

		/**
		 * http 配置
		 */
		Http http = new Http();

		/**
		 * mysql 配置
		 */
		MySQL mysql = new MySQL();

		/**
		 * redis 配置
		 */
		Redis redis = new Redis();

		Refresh refresh = new Refresh();

		/**
		 * 默认延迟10s，周期60s
		 */
		@Data
		public static class Refresh {
			Integer delay = 10;
			Integer period = 60;
		}
	}

	@Data
	public static class MySQL {
		private String url;
		private String username;
		private String password;
	}

	@Data
	public static class Redis {
		private String host = "localhost";
		private Integer port = 6379;
		private String username;
		private String password;
		private Integer database = 0;
	}

	@Data
	public static class Http {
		String base = "http://localhost";
	}
}
