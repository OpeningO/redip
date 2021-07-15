package org.openingo.redip.dictionary.remote;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.Asserts;
import org.openingo.redip.configuration.RedipBaseConfigurationProperties;
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
     * 默认初始化词典实例
     * @param properties 配置信息
     */
    public static void initial(RedipBaseConfigurationProperties properties) {
        initial(properties, true);
    }

    /**
     * 默认初始化词典实例
     * @param properties 配置信息
     * @param initDictionaryInstance 是否初始化词典实例
     */
    public static void initial(RedipBaseConfigurationProperties properties,
                               boolean initDictionaryInstance) {
        if (Objects.isNull(remoteDictionaryHandler)) {
            synchronized (RemoteDictionary.class) {
                if (Objects.isNull(remoteDictionaryHandler)) {
                    remoteDictionaryHandler = new RemoteDictionary();
                    if (initDictionaryInstance) {
                        addRemoteDictionary(new HttpRemoteDictionary(properties));
                        addRemoteDictionary(new RedisRemoteDictionary(properties));
                        addRemoteDictionary(new MySQLRemoteDictionary(properties));
                    }
                    log.info("Remote Dictionary Initialed");
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

    public static Set<String> getRemoteWords(IDictionary dictionary,
                                             DictionaryType dictionaryType,
                                             URI domainUri) {
        checkInitial();
        final AbstractRemoteDictionary remoteDictionary = RemoteDictionary.getRemoteDictionary(domainUri);
        Set<String> remoteWords = Collections.emptySet();
        if (Objects.isNull(remoteDictionary)) {
            return remoteWords;
        }
        synchronized (RemoteDictionary.class) {
            remoteWords = AccessController.doPrivileged((PrivilegedAction<Set<String>>) () -> remoteDictionary.getRemoteWords(dictionary, dictionaryType, domainUri));
            return StringHelper.filterBlank(remoteWords);
        }
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
                                  String word) {
        RemoteDictionaryEtymology etymology = RemoteDictionaryEtymology.newEtymology(domainUri.getScheme());
        return RemoteDictionary.addWord(etymology, dictionaryType, domainUri.getAuthority(), word);
    }

    public static boolean addWord(RemoteDictionaryEtymology etymology,
                                  DictionaryType dictionaryType,
                                  String domain,
                                  String word) {
        checkInitial();
        final AbstractRemoteDictionary dictionary = REMOTE_DICTIONARY.get(etymology.getEtymology());
        synchronized (RemoteDictionary.class) {
            return dictionary.addWord(dictionaryType, domain, word);
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
            log.error("Load Remote Dictionary Error");
        }
        return remoteDictionary;
    }
}
