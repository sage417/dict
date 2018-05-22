package moe.yamato.dict;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.domain.Range.Bound.*;

@Service
public class ItemService {

    private ReactiveRedisTemplate<String, Item> reactiveRedisTemplate;

    private ReactiveRedisTemplate<String, String> stringReactiveRedisTemplate;

    private StringRedisTemplate stringRedisTemplate;

    private HashMapper<Item, String, String> itemHashMapper;

    private String itemKey(String group, int order) {
        return "dict:" + group + ":" + Integer.toString(order);
    }

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
    public ItemService(
            ReactiveRedisTemplate<String, Item> reactiveRedisTemplate,
            ReactiveRedisTemplate<String, String> stringReactiveRedisTemplate,
            StringRedisTemplate stringRedisTemplate,
            HashMapper<Item, String, String> itemHashMapper
    ) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.stringReactiveRedisTemplate = stringReactiveRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.itemHashMapper = itemHashMapper;
    }

    public Mono<Boolean> addItem(String group, Item item) {

        Map<String, String> beanMap = itemHashMapper.toHash(item);

        Set<String> tokens = this.token(item.getName());

        final String itemKey = itemKey(group, item.getOrder());

        return Flux.fromIterable(tokens)
                .flatMap(t -> this.stringReactiveRedisTemplate.opsForSet().add(indexKey(group, t), itemKey))
                .last()
                .flatMap(r -> stringReactiveRedisTemplate.opsForHash().putAll(itemKey, beanMap))
                .filter(b -> b)
                .flatMap(r -> stringReactiveRedisTemplate.opsForZSet().add(groupKey(group), itemKey, item.getOrder()));
    }

    public Mono<Boolean> deleteItem(String group, Item item) {
        Set<String> tokens = this.token(item.getName());

        return Flux.fromIterable(tokens)
                .flatMap(t -> this.stringReactiveRedisTemplate.opsForSet().delete(indexKey(group, t)))
                .last()
                .flatMap(r -> this.reactiveRedisTemplate.opsForHash().delete(itemKey(group, item.getOrder())))
                .filter(b -> b)
                .flatMap(r -> this.stringReactiveRedisTemplate.opsForZSet().delete(groupKey(group)));
    }

    public Flux<Item> findItems(String group, long start, long end) {
        return this.stringReactiveRedisTemplate.opsForZSet()
                .range(groupKey(group), Range.of(inclusive(start), inclusive(end)))
                .map(itemKey -> this.stringRedisTemplate.<String, String>opsForHash().entries(itemKey))
                .map(m -> itemHashMapper.fromHash(m));
    }

    public Flux<Item> findItemsByName(String group, String itemNameKeyWord) {
        Set<String> tokens = this.token(itemNameKeyWord);

        if (tokens.isEmpty()) {
            return Flux.empty();
        }

        String first = tokens.iterator().next();
        Flux<String> itemKeyFlux = tokens.size() < 2 ?
                this.stringReactiveRedisTemplate.opsForSet().members(indexKey(group, first)) :
                this.stringReactiveRedisTemplate.opsForSet()
                        .intersect(
                                indexKey(group, first),
                                tokens.stream().skip(1).map(t -> this.indexKey(group, t)).collect(Collectors.toList())
                        );

        return itemKeyFlux
                .map(itemKey -> this.stringRedisTemplate.<String, String>opsForHash().entries(itemKey))
                .map(m -> itemHashMapper.fromHash(m));
    }

    public List<Group> findGroups() {
        return null;
    }

    public Mono<Long> deleteGroup(String group) {
        return this.reactiveRedisTemplate.delete(groupKey(group));
    }
}
