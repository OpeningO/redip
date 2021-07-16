### Redip

![maven](https://img.shields.io/maven-central/v/org.openingo/redip-parent.svg)

> redip: the [elasticsearch-analysis-ik](https://github.com/OpeningO/elasticsearch-analysis-ik) remote dictionary words provider build on spring-boot.

#### HOW?

```yaml
openingo:
  redip:
    mysql:
      url: jdbc:mysql://127.0.0.1/ik-db?useSSL=false&serverTimezone=GMT%2B8
      username: root
      password: dbadmin
    redis:
      host: localhost
      port: 6379
      database: 0
```



#### Spring Boot Env

```xml
<dependency>
    <groupId>org.openingo.boot</groupId>
    <artifactId>redip-spring-boot-starter</artifactId>
    <version>${redip-spring-boot-starter.version}</version>
</dependency>
```

```java
@Component
public class RemoteWordProvider {

    @Autowired
    MySQLRemoteDictionary mySQLRemoteDictionary;

    public void addMySQLWord() {
        mySQLRemoteDictionary.addMainWord("order", "new mysql word");
    }

    @Autowired
    RedisRemoteDictionary redisRemoteDictionary;

    public void addRedisWord() {
        redisRemoteDictionary.addMainWord("order", "new redis word");
    }
}
```



#### Pure Java

```xml
<dependency>
    <groupId>org.openingo.kits</groupId>
    <artifactId>redip</artifactId>
    <version>${redip.version}</version>
</dependency>
```

##### ikanalyzer.yml配置

```yml
dict: # 扩展词库配置
  local: # 本地扩展词典配置
    main: # 本地主词典扩展词典文件
      - extra_main.dic
      - extra_single_word.dic
      - extra_single_word_full.dic
      - extra_single_word_low_freq.dic
    stop: # 本地stop词典扩展词典文件
      - extra_stopword.dic
  remote: # 远程扩展词典配置
    http:
      # http 服务地址
      # main-words path: ${base}/es-dict/main-words/{domain}
      # stop-words path: ${base}/es-dict/stop-words/{domain}
      base: http://localhost
    redis:
      # main-words key: es-ik-words:{domain}:main-words
      # stop-words key: es-ik-words:{domain}:stop-words
      host: localhost
      port: 6379
      database: 0
      username:
      password:
    mysql:
      url: jdbc:mysql://127.0.0.1/ik-db?useSSL=false&serverTimezone=GMT%2B8
      username: root
      password: dbadmin
    refresh: # 刷新配置
      delay: 10 # 延迟时间，单位s
      period: 60 # 周期时间，单位s
```

```java
public static void main(String[] args) {
  Yaml yaml = new Yaml(new CustomClassLoaderConstructor(RedipConfigurationProperties.class, TestSettings.class.getClassLoader()));
  InputStream resourceAsStream = TestSettings.class.getClassLoader().getResourceAsStream("ikanalyzer.yml");
  RedipConfigurationProperties properties = yaml.loadAs(resourceAsStream, RedipConfigurationProperties.class);
  RemoteDictionary.initial(properties);

  RemoteDictionary.addWord(RemoteDictionaryEtymology.MYSQL, DictionaryType.MAIN_WORDS, "user", "new words");
  Set<String> userWords = RemoteDictionary.getRemoteWords(RemoteDictionaryEtymology.MYSQL, DictionaryType.MAIN_WORDS, "user");
  System.out.println(userWords);
}
```



#### SQL Script

>  redip jar Include `redip.sql`

```sql
/*
 @author Qicz

 Date: 13/07/2021 10:18:19
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ik_dict_state
-- ----------------------------
DROP TABLE IF EXISTS `ik_dict_state`;
CREATE TABLE `ik_dict_state` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `domain` varchar(100) NOT NULL COMMENT '所属领域',
  `state` varchar(10) NOT NULL COMMENT 'newly有更新non-newly无更新',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `domain` (`domain`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for ik_words
-- ----------------------------
DROP TABLE IF EXISTS `ik_words`;
CREATE TABLE `ik_words` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `word` varchar(200) NOT NULL,
  `word_type` tinyint(4) unsigned NOT NULL COMMENT 'word类型，1主词库，2stop词库',
  `domain` varchar(100) NOT NULL COMMENT '所属领域',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `domain` (`domain`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
```

