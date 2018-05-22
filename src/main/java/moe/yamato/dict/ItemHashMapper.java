package moe.yamato.dict;

import org.apache.kafka.common.protocol.types.Field;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ItemHashMapper implements HashMapper<Item, String, String> {
    @Override
    public Map<String, String> toHash(Item object) {
        final Map<String, String> map = new HashMap<>(3);
        map.put("name", object.getName());
        map.put("code", object.getCode());
        map.put("order", Integer.toString(object.getOrder()));
        return map;
    }

    @Override
    public Item fromHash(Map<String, String> hash) {
        Item item = new Item();
        item.setName(hash.get("name"));
        item.setCode(hash.get("code"));
        item.setOrder(Integer.parseInt(hash.get("order")));
        return item;
//        item.setName(hash.get("name"));
//        return Item.builder()
//                .name(hash.get("name"))
//                .code(hash.get("code"))
//                .order(Integer.parseInt(hash.get("order")))
//                .build();
    }
}
