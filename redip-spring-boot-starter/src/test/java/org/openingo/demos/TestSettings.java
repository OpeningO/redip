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

package org.openingo.demos;

import org.junit.Test;
import org.openingo.redip.configuration.RedipConfigurationProperties;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.remote.RemoteDictionary;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.InputStream;
import java.util.Set;

/**
 * TestSettings
 *
 * @author Qicz
 * @since 2021/7/9 10:02
 */
public class TestSettings {

	@Test
	public void loadYml() {
		Yaml yaml = new Yaml(new CustomClassLoaderConstructor(RedipConfigurationProperties.class, TestSettings.class.getClassLoader()));
		InputStream resourceAsStream = TestSettings.class.getClassLoader().getResourceAsStream("ikanalyzer.yml");
		RedipConfigurationProperties map = yaml.loadAs(resourceAsStream, RedipConfigurationProperties.class);
		System.out.println(map);
	}

	private void initRemoteDictionary() {
		Yaml yaml = new Yaml(new CustomClassLoaderConstructor(RedipConfigurationProperties.class, TestSettings.class.getClassLoader()));
		InputStream resourceAsStream = TestSettings.class.getClassLoader().getResourceAsStream("ikanalyzer.yml");
		RedipConfigurationProperties properties = yaml.loadAs(resourceAsStream, RedipConfigurationProperties.class);
		RemoteDictionary.initial(properties);
	}

	@Test
	public void addMySQLWords() {
		this.initRemoteDictionary();
		RemoteDictionary.addWord(RemoteDictionaryEtymology.MYSQL, DictionaryType.MAIN_WORDS, "user", "new words");
		Set<String> userWords = RemoteDictionary.getRemoteWords(RemoteDictionaryEtymology.MYSQL, DictionaryType.MAIN_WORDS, "user");
		System.out.println(userWords);
	}

	@Test
	public void addRedisWords() {
		this.initRemoteDictionary();
		RemoteDictionary.addWord(RemoteDictionaryEtymology.REDIS, DictionaryType.MAIN_WORDS, "user", "new words", "word2", "word3");
		Set<String> userWords = RemoteDictionary.getRemoteWords(RemoteDictionaryEtymology.REDIS, DictionaryType.MAIN_WORDS, "user");
		System.out.println(userWords);
	}
}
