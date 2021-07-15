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



