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

import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.Asserts;
import org.openingo.redip.configuration.RedipConfigurationProperties;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.IDictionary;
import org.openingo.redip.helper.StringHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * RemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 16:46
 */
@Slf4j
public final class RemoteDictionary {

    private static RemoteDictionary remoteDictionaryHandler;
    private static final Map<String, AbstractRemoteDictionary> REMOTE_DICTIONARY = new HashMap<>();

    private RemoteDictionary() {
    }

    private static void checkInitial() {
        Asserts.notNull(remoteDictionaryHandler, "The RemoteDictionary is not initial.");
    }

    /**
     * 初始化词典实例
     * @param properties 配置信息
     */
    public static void initial(RedipConfigurationProperties properties) {
        RedipConfigurationProperties.Remote remoteConfiguration = properties.getRemote();
        initial();
        addRemoteDictionary(new HttpRemoteDictionary(remoteConfiguration));
        addRemoteDictionary(new RedisRemoteDictionary(remoteConfiguration));
        addRemoteDictionary(new MySQLRemoteDictionary(remoteConfiguration));
        log.info("Remote Dictionary Initialed");
    }

    /**
     * 初始化词典实例
     */
    public static void initial() {
        if (Objects.isNull(remoteDictionaryHandler)) {
            synchronized (RemoteDictionary.class) {
                if (Objects.isNull(remoteDictionaryHandler)) {
                    remoteDictionaryHandler = new RemoteDictionary();
                }
            }
        }
    }

    public static void addRemoteDictionary(AbstractRemoteDictionary remoteDictionary) {
        checkInitial();
        String etymology = remoteDictionary.etymology();
        if (!REMOTE_DICTIONARY.containsKey(etymology)) {
            REMOTE_DICTIONARY.put(etymology, remoteDictionary);
        }
        log.info("The Remote Dictionary For etymology '{}' is loaded!", etymology);
    }

    public static Set<String> getRemoteWords(DictionaryType dictionaryType,
                                             URI domainUri) {
        checkInitial();
        log.info("begin to get remote dictionary words...");
        final AbstractRemoteDictionary remoteDictionary = RemoteDictionary.getRemoteDictionary(domainUri);
        Set<String> remoteWords = Collections.emptySet();
        if (Objects.isNull(remoteDictionary)) {
            log.info("the remote dictionary for '{}' not found.", domainUri);
            return remoteWords;
        }
        synchronized (RemoteDictionary.class) {
            remoteWords = AccessController.doPrivileged((PrivilegedAction<Set<String>>) () -> remoteDictionary.getRemoteWords(dictionaryType, domainUri));
            return StringHelper.filterBlank(remoteWords);
        }
    }

    public static Set<String> getRemoteWords(RemoteDictionaryEtymology etymology,
                                             DictionaryType dictionaryType,
                                             String domain) {
        return getRemoteWords(dictionaryType, URI.create(String.format("%s://%s", etymology.getEtymology(), domain)));
    }

    public static void reloadRemoteDictionary(IDictionary dictionary,
                                              DictionaryType dictionaryType,
                                              URI domainUri) {
        checkInitial();
        final AbstractRemoteDictionary remoteDictionary = RemoteDictionary.getRemoteDictionary(domainUri);
        if (Objects.isNull(remoteDictionary)) {
            return;
        }
        synchronized (RemoteDictionary.class) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                remoteDictionary.reloadDictionary(dictionary, dictionaryType, domainUri);
                return null;
            });
        }
    }

    public static boolean addWord(DictionaryType dictionaryType,
                                  URI domainUri,
                                  String... words) {
        RemoteDictionaryEtymology etymology = RemoteDictionaryEtymology.newEtymology(domainUri.getScheme());
        return RemoteDictionary.addWord(etymology, dictionaryType, domainUri.getAuthority(), words);
    }

    public static boolean addWord(RemoteDictionaryEtymology etymology,
                                  DictionaryType dictionaryType,
                                  String domain,
                                  String... words) {
        checkInitial();
        final AbstractRemoteDictionary dictionary = REMOTE_DICTIONARY.get(etymology.getEtymology());
        synchronized (RemoteDictionary.class) {
            return dictionary.addWord(dictionaryType, domain, words);
        }
    }

    private static URI toUri(String location) {
        URI uri;
        try {
            uri = new URI(location);
            log.info("schema {} authority {}", uri.getScheme(), uri.getAuthority());
        } catch (URISyntaxException e) {
            log.error("parser location to uri error {} ", e.getLocalizedMessage());
            throw new IllegalArgumentException(String.format("the location %s is illegal: %s", location, e.getLocalizedMessage()));
        }
        return uri;
    }

    private static AbstractRemoteDictionary getRemoteDictionary(URI uri) {
        String etymology = uri.getScheme();
        log.info("Remote Dictionary etymology '{}'", etymology);
        AbstractRemoteDictionary remoteDictionary = REMOTE_DICTIONARY.get(etymology);
        if (Objects.isNull(remoteDictionary)) {
            log.info("the remote dictionary for '{}' not found.", uri);
        }
        return remoteDictionary;
    }
}
