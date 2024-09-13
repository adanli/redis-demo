package org.egg.vote;

import com.google.gson.JsonSerializationContext;
import lombok.Data;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Article {

    private final Jedis jedis;

    {
        jedis = new Jedis("11.0.0.193", 6379);
    }

    public static void main(String[] args) {
        Article vote = new Article();
//        vote.vote();
//        vote.post();
        vote.getArticles();
    }

    /**
     * 115423号用户给100408号文章投票
     * 1. 100408增加432分,             zset
     * 2. 115423挂到voted:100408下,    set
     * 3. 该文章的被投票数+1,            hset
     */
    private void vote() {
        double voteScore = 432;

        double score = jedis.zscore("score", "article:100408");
        jedis.zadd("score", score+voteScore, "article:100408");

        jedis.sadd("voted:100408", "user:115423");

        Map<String, String> map = jedis.hgetAll("article:100408");
        String votes = map.get("votes");
        int v;
        if (votes == null) {
            v = 1;
        } else {
            v = Integer.parseInt(votes)+1;
        }
        map.put("votes", v+"");
        jedis.hset("article:100408", map);

    }

    /**
     * 发布文章
     * 1. 创建文章ID                                     String
     * 2. 将文章发布者，作为已投票的人员列表，user:115423     Set
     * 3. 设置文章投票的过期时间，一周过期                   Set
     * 4. 存储文章的相关信息                              Hset
     * 5. 设置文章的初始评分，和发布时间                    Zset
     */
    private void post() {
        long maxArticleNum = jedis.incr("max-article-num");

        String key = String.format("voted:%s",maxArticleNum);
        long now = System.currentTimeMillis();

        jedis.sadd(key, "user:115423");
        jedis.expire(key, now+86400);

        Map<String, String> article = new HashMap<>();
        article.put("title", "new-article");
        article.put("link", "http://www.baidu.com");
        article.put("poster", "user:115423");
        article.put("time", now+"");
        article.put("votes", "1");
        jedis.hset("article:" + maxArticleNum, article);

        jedis.zadd("time", now, String.format("article:%s", maxArticleNum));
        jedis.zadd("score", now+432, String.format("article:%s", maxArticleNum));

    }

    private void getArticles() {
        // 评分最高的文章
        ArticleDTO vote = null;

        // 最新发布的文章
        ArticleDTO newest = null;

        List<String> list = jedis.zrevrange("score", 0, -1);
        for (String id: list) {
            Map<String, String> map = jedis.hgetAll(id);
            if(map==null || map.isEmpty()) continue;
            if(vote == null) {
                vote = new ArticleDTO();
                vote.id = id;
                vote.votes = Double.parseDouble(map.get("votes"));
            } else {
                long s = Long.parseLong(map.get("votes"));
                if(vote.getVotes() < s) {
                    vote.id = id;
                    vote.votes = s;
                }
            }

            if(newest == null) {
                newest = new ArticleDTO();
                newest.id = id;
                newest.time = Double.parseDouble(map.get("time"));
            } else {
                double t = Double.parseDouble(map.get("time"));
                if(newest.time < t) {
                    newest.id = id;
                    newest.time = t;
                }
            }

        }

        if (vote != null) {
            System.out.printf("评分最高的文章: %s%n", vote.id);
        }

        if(newest != null) {
            System.out.printf("最新发布的文章: %s%n", newest.id);
        }

    }

    @Data
    static class ArticleDTO {
        private String id;
        private double votes;
        private double time;
    }

}
