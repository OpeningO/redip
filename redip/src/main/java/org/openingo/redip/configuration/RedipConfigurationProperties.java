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
public class RedipConfigurationProperties extends RedipBaseConfigurationProperties {

	/**
	 * 扩展词库
	 */
	Dict dict = new Dict();

	public final List<String> getMainExtDictFiles() {
		return StringHelper.filterBlank(dict.local.main);
	}

	public final List<String> getExtStopDictFiles() {
		return StringHelper.filterBlank(dict.local.stop);
	}

	public final Remote.Refresh getRemoteRefresh() {
		return this.dict.remote.getRefresh();
	}

	@Override
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
}
