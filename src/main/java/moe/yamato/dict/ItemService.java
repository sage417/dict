package moe.yamato.dict;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ItemService {

    private ReactiveRedisTemplate<String, Item> reactiveRedisTemplate;

    private ReactiveRedisTemplate<String, String> stringReactiveRedisTemplate;

    private String groupKey(String group) {
        return "dict:" + group;
    }

    private String indexKey(String group, String token) {
        return groupKey(group) + ":index:" + token;
    }

    private Set<String> token(String itemName) {
        String token = itemName.replaceAll("[^\\u4e00-\\u9fa5]", "");
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int i = 0; i < token.length(); i++) {
            result.add(String.valueOf(token.charAt(i)));
        }
        return result;
    }

    @Autowired
    public ItemService(ReactiveRedisTemplate<String, Item> reactiveRedisTemplate, ReactiveRedisTemplate<String, String> stringReactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.stringReactiveRedisTemplate = stringReactiveRedisTemplate;
    }

    public Mono<Boolean> addItem(String group, Item item) {
        String groupKey = groupKey(group);

        Set<String> tokens = this.token(item.getName());

        Flux.fromIterable(tokens)
                .flatMap(t-> this.stringReactiveRedisTemplate.opsForSet().add(indexKey(group, t), groupKey)).subscribe();

        return this.reactiveRedisTemplate.opsForZSet().add(groupKey, item, item.getOrder());
    }

    public Mono<Long> deleteItem(String group, Item... items) {
        return this.reactiveRedisTemplate.opsForZSet().remove(groupKey(group), items);
    }

    public Flux<Item> findItems(String group, int start, int count) {
        return this.reactiveRedisTemplate.opsForZSet().rangeByScore(groupKey(group), Range.of(Range.Bound.inclusive(1d), Range.Bound.inclusive(100d)));
    }

    public Flux<Item> findItemsByScore(String group, double score) {
        return this.reactiveRedisTemplate.opsForZSet().rangeByScore(groupKey(group), Range.of(Range.Bound.inclusive(score), Range.Bound.inclusive(score)));
    }

    public List<Group> findGroups() {
        return null;
    }

    public Mono<Long> deleteGroup(String group) {
        return this.reactiveRedisTemplate.delete(groupKey(group));
    }
}
