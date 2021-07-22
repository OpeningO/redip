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

package org.openingo.redip.configuration;

import lombok.Data;
import org.openingo.redip.helper.StringHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	public final Set<String> getLocalMainExtDictFiles() {
		return new HashSet<>(StringHelper.filterBlank(dict.local.main));
	}

	public final Set<String> getLocalStopExtDictFiles() {
		return new HashSet<>(StringHelper.filterBlank(dict.local.stop));
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
