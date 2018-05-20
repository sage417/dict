package moe.yamato.dict;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/group/{group}")
public class ItemController {

    private ItemService itemService;

    @Autowired
    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/items")
    public Flux<Item> findItems(@PathVariable String group) {
        return this.itemService.findItems(group, 0, 100);
    }

    @GetMapping(value = "/items", params = "score")
    public Flux<Item> findItemsByScore(@PathVariable String group, double score) {
        return itemService.findItemsByScore(group, score);
    }

    @PostMapping("/item")
    public Mono<Boolean> addItem(@PathVariable String group, @RequestBody Item item) {
        return this.itemService.addItem(group, item);
    }

    @DeleteMapping("/item")
    public Mono<Long> deleteItem(@PathVariable String group, @RequestBody Item item) {
        return this.itemService.deleteItem(group, item);
    }

    @DeleteMapping
    public Mono<Long> deleteItemsUnderGroup(@PathVariable String group) {
        return this.itemService.deleteGroup(group);
    }
}