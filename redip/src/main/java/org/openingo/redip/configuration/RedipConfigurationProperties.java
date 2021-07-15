package org.openingo.redip.configuration;

import lombok.Data;
import org.openingo.redip.helper.StringHelper;

import java.util.Collections;
import java.util.List;

/**
 * RedipConfigurationProperties
 *
 * @author Qicz
 * @since 2021/7/14 18:32
 */
@Data
public class RedipConfigurationProperties {

	/**
	 * 扩展词库
	 */
	Dict dict = new Dict();

	public final List<String> getLocalMainExtDictFiles() {
		return StringHelper.filterBlank(dict.local.main);
	}

	public final List<String> getLocalStopExtDictFiles() {
		return StringHelper.filterBlank(dict.local.stop);
	}

	public final Remote.Refresh getRemoteRefresh() {
		return this.dict.remote.getRefresh();
	}

	public Remote getRemote() {
		return this.dict.remote;
	}

	@Data
	public static class Dict {

		/**
		 * 本地词库文件
		 */
		DictFile local = new DictFile();

		/**
		 * 远程词库文件
		 */
		Remote remote = new Remote();
	}

	/**
	 * 词典文件
	 */
	@Data
	public static class DictFile {

		/**
		 * 主词典文件
		 */
		List<String> main = Collections.emptyList();

		/**
		 * stop词典文件
		 */
		List<String> stop = Collections.emptyList();
	}

	@Data
	public static class Remote extends RemoteConfiguration {

		@Override
		public Http http() {
			return http;
		}

		/**
		 * http 配置
		 */
		Http http = new Http();

		Remote.Refresh refresh = new Remote.Refresh();

		/**
		 * 默认延迟10s，周期60s
		 */
		@Data
		public static class Refresh {
			Integer delay = 10;
			Integer period = 60;
		}
	}
}
