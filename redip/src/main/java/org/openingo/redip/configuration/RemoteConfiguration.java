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

/**
 * RemoteConfiguration
 *
 * @author Qicz
 * @since 2021/7/15 20:24
 */
@Data
public class RemoteConfiguration {

    /**
     * mysql 配置
     */
    MySQL mysql = new MySQL();

    /**
     * redis 配置
     */
    Redis redis = new Redis();

    public Http http() {
        return new Http();
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
