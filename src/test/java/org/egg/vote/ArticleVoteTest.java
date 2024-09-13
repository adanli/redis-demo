package org.egg.vote;

import junit.framework.TestCase;
import redis.clients.jedis.Jedis;

import java.util.Map;

public class ArticleVoteTest extends TestCase {
    private final Jedis jedis;

    {
        jedis = new Jedis("11.0.0.193", 6379);
    }

    public void testInitArticle() {
        String hKey = "article:100408";

        Map<String, String> article = jedis.hgetAll(hKey);
        article.put("title", "Go To Statement considered harmful");
        article.put("link", "http://www.google.com/kZUSu");
        article.put("poster", "user:83271");
        article.put("time", "1331382699.33");
        article.put("votes", "528");

        jedis.hset(hKey, article);
    }

    public void testInitTime() {
        jedis.zadd("time", 1332065417.47, "article:100408");
        jedis.zadd("time", 1332075503.49, "article:100635");
        jedis.zadd("time", 1332082035.26, "article:100716");
    }

    public void testInitScore() {
        jedis.zadd("score", 1332164063.49, "article:100635");
        jedis.zadd("score", 1332174713.47, "article:100408");
        jedis.zadd("score", 1332225027.26, "article:100716");
    }

    public void testInitSet() {
        jedis.sadd("voted:100408",
                "user:234487",
                "user:253378",
                "user:364680",
                "user:132097",
                "user:350917"
                );
    }

}
