package moe.yamato.dict.item;

import moe.yamato.dict.Group;
import moe.yamato.dict.token.TokenParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.springframework.data.domain.Range.Bound.inclusive;

@Service
public class ItemService {

    private ReactiveRedisTemplate<String, Item> reactiveRedisTemplate;
    private ReactiveRedisTemplate<String, String> stringReactiveRedisTemplate;
    private StringRedisTemplate stringRedisTemplate;
    private HashMapper<Item, String, String> itemHashMapper;
    private TokenParser itemNameTokenParser;
    private TokenParser itemCodeTokenParser;

    private String itemKey(String group, int order) {
        return "dict:" + group + ":" + Integer.toString(order);
    }

    private String groupKey(String group) {
        return "dict:" + group;
    }

    @Autowired
    public ItemService(
            ReactiveRedisTemplate<String, Item> reactiveRedisTemplate,
            ReactiveRedisTemplate<String, String> stringReactiveRedisTemplate,
            StringRedisTemplate stringRedisTemplate,
            HashMapper<Item, String, String> itemHashMapper,
            @Qualifier("itemNameTokenParser") TokenParser itemNameTokenParser,
            @Qualifier("itemCodeTokenParser") TokenParser itemCodeTokenParser
    ) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.stringReactiveRedisTemplate = stringReactiveRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.itemHashMapper = itemHashMapper;
        this.itemNameTokenParser = itemNameTokenParser;
        this.itemCodeTokenParser = itemCodeTokenParser;
    }

    private String indexKey(String group, String token) {
        return groupKey(group) + ":n_idx:" + token;
    }

    private String codeIndexKey(String group, String token) {
        return groupKey(group) + ":c_idx:" + token;
    }

    public Mono<Boolean> addItem(String group, Item item) {

        Map<String, String> beanMap = itemHashMapper.toHash(item);

        final String itemKey = itemKey(group, item.getOrder());

        var itemMono = stringReactiveRedisTemplate.opsForHash().putAll(itemKey, beanMap);
        var groupItemMono = stringReactiveRedisTemplate.opsForZSet().add(groupKey(group), itemKey, item.getOrder());
        var nameIdxMono = Flux.fromIterable(this.itemNameTokenParser.token(item.getName())).flatMap(t -> this.stringReactiveRedisTemplate.opsForSet().add(indexKey(group, t), itemKey)).all(r -> r == 1);
        var codeIdxMono = Flux.fromIterable(this.itemCodeTokenParser.token(item.getCode())).flatMap(t -> this.stringReactiveRedisTemplate.opsForSet().add(codeIndexKey(group, t), itemKey)).all(r -> r == 1);

        return Mono.zip(itemMono, groupItemMono, nameIdxMono, codeIdxMono)
                .map(t -> Boolean.TRUE);
    }

    public Mono<Boolean> deleteItem(String group, Item item) {
        final String itemKey = itemKey(group, item.getOrder());

        return Mono.zip(
                stringReactiveRedisTemplate.opsForHash().delete(itemKey),
                stringReactiveRedisTemplate.opsForZSet().delete(groupKey(group)),
                Flux.fromIterable(this.itemNameTokenParser.token(item.getName())).flatMap(t -> this.stringReactiveRedisTemplate.opsForSet().delete(indexKey(group, t))).all(Predicate.isEqual(Boolean.TRUE)),
                Flux.fromIterable(this.itemCodeTokenParser.token(item.getCode())).flatMap(t -> this.stringReactiveRedisTemplate.opsForSet().delete(codeIndexKey(group, t))).all(Predicate.isEqual(Boolean.TRUE))
        ).map(t -> Boolean.TRUE);
    }

    public Mono<Item> findItemByOrder(String group, int order) {
        return Mono.just(this.stringRedisTemplate.<String, String>opsForHash()
                .entries(itemKey(group, order)))
                .map(m -> itemHashMapper.fromHash(m));
    }

    public Flux<Item> findItems(String group, long start, long end) {
        return this.stringReactiveRedisTemplate.opsForZSet()
                .range(groupKey(group), Range.of(inclusive(start), inclusive(end)))
                .map(this.stringRedisTemplate.<String, String>opsForHash()::entries)
                .map(m -> itemHashMapper.fromHash(m));
    }

    public Flux<Item> findItemsByName(String group, String itemNameKeyWord) {
        Set<String> tokens = this.itemNameTokenParser.token(itemNameKeyWord);

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
        return null;
    }
}
