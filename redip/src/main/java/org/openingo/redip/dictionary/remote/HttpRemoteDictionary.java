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

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.openingo.redip.configuration.RemoteConfiguration;
import org.openingo.redip.constants.DictionaryType;
import org.openingo.redip.constants.RemoteDictionaryEtymology;
import org.openingo.redip.dictionary.IDictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpRemoteDictionary
 *
 * @author Qicz
 * @since 2021/7/11 15:38
 */
@Slf4j
class HttpRemoteDictionary extends AbstractRemoteDictionary {

    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    /**
     * 超时设置
     */
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000)
            .setConnectTimeout(10 * 1000).setSocketTimeout(15 * 1000).build();

    private static final Map<String, Modifier> MODIFIER_MAPPING = new ConcurrentHashMap<>();

    public HttpRemoteDictionary(RemoteConfiguration remoteConfiguration) {
        super(remoteConfiguration);
    }

    @Override
    protected boolean addWord(DictionaryType dictionaryType, String domain, String... words) {
        log.info("'{}' remote dictionary add new word 'not support", this.etymology());
        return false;
    }

    @Override
    protected void closeResource() {

    }

    @Override
    public Set<String> getRemoteWords(DictionaryType dictionaryType,
                                      URI domainUri) {
        log.info("'http' remote dictionary get new words from domain '{}' dictionary '{}'", domainUri, dictionaryType);
        Set<String> words = new HashSet<>();
        CloseableHttpResponse response;
        BufferedReader in;
        String location = this.getLocation(dictionaryType, domainUri);
        HttpGet get = new HttpGet(location);
        get.setConfig(REQUEST_CONFIG);
        try {
            response = HTTP_CLIENT.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String charset = "UTF-8";
                // 获取编码，默认为utf-8
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    Header contentType = entity.getContentType();
                    if (contentType != null && contentType.getValue() != null) {
                        String typeValue = contentType.getValue();
                        if (typeValue != null && typeValue.contains("charset=")) {
                            charset = typeValue.substring(typeValue.lastIndexOf("=") + 1);
                        }
                    }

                    if (entity.getContentLength() > 0 || entity.isChunked()) {
                        in = new BufferedReader(new InputStreamReader(entity.getContent(), charset));
                        String line;
                        while ((line = in.readLine()) != null) {
                            words.add(line);
                        }
                        in.close();
                        response.close();
                        return words;
                    }
                }
            }
            response.close();
        } catch (IllegalStateException | IOException e) {
            log.error("getRemoteWords error '{}' location '{}'", e, location);
        }
        return words;
    }

    /**
     * ①向词库服务器发送Head请求
     * ②从响应中获取Last-Modify、ETags字段值，判断是否变化
     * ③如果未变化，休眠1min，返回第①步
     * ④如果有变化，重新加载词典
     * ⑤休眠1min，返回第①步
     */
    @Override
    protected void reloadDictionary(IDictionary dictionary,
                                    DictionaryType dictionaryType,
                                    URI domainUri) {
        log.info("'http' remote dictionary reload dictionary from domain '{}' dictionary '{}'", domainUri, dictionaryType);
        String location = this.getLocation(dictionaryType, domainUri);
        HttpHead head = new HttpHead(location);
        head.setConfig(REQUEST_CONFIG);
        // 上次更改时间
        String lastModified = null;
        // 资源属性
        String eTags = null;
        Modifier modifier = MODIFIER_MAPPING.get(location);
        if (Objects.nonNull(modifier)) {
            lastModified = modifier.lastModified;
            eTags = modifier.eTags;
        }

        //设置请求头
        if (lastModified != null) {
            head.setHeader("If-Modified-Since", lastModified);
        }
        if (eTags != null) {
            head.setHeader("If-None-Match", eTags);
        }

        CloseableHttpResponse response = null;
        try {
            response = HTTP_CLIENT.execute(head);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
                log.info("[Remote DictFile Reloading] Not modified!");
                return;
            }

            //返回200 才做操作
            if (statusCode == HttpStatus.SC_OK) {
                Header lastHeader = response.getLastHeader("Last-Modified");
                Header eTag = response.getLastHeader("ETag");
                boolean needReload = (Objects.nonNull(lastHeader) && !lastHeader.getValue().equalsIgnoreCase(lastModified))
                        || (Objects.nonNull(eTag) && !eTag.getValue().equalsIgnoreCase(eTags));
                if (needReload) {
                    // 远程词库有更新,需要重新加载词典，并修改last_modified,eTags
                    dictionary.reload(dictionaryType);
                    lastModified = Objects.isNull(lastHeader) ? null : lastHeader.getValue();
                    eTags = Objects.isNull(eTag) ? null : eTag.getValue();
                    MODIFIER_MAPPING.put(location, new Modifier(lastModified, eTags));
                }
                return;
            }
            log.info("remote_ext_dict '{}' return bad code '{}'", location, statusCode);
        } catch (Exception e) {
            log.error("remote_ext_dict error '{}' location '{}' !", e, location);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error("remote_ext_dict response close error", e);
            }
        }
    }

    @Override
    public String etymology() {
        return RemoteDictionaryEtymology.HTTP.getEtymology();
    }

    private String getLocation(DictionaryType dictionaryType, URI domainUri) {
        RemoteConfiguration.Http http = this.remoteConfiguration.http();
        // path: ${base}/es-dict/${main}/{domain}
        // or path: ${base}/es-dict/${stop}/{domain}
        return String.format("%s/es-dict/%s/%s", http.getBase(), dictionaryType.getDictName(), domainUri.getAuthority());
    }

    @AllArgsConstructor
    @NoArgsConstructor
    static class Modifier {
        /*
         * 上次更改时间
         */
        String lastModified;
        /*
         * 资源属性
         */
        String eTags;
    }
}
