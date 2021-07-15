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
