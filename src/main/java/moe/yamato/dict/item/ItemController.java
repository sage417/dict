package moe.yamato.dict.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/{group}")
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

    @GetMapping(value = "/items", params = "name")
    public Flux<Item> findItemsByScore(@PathVariable String group, String name) {
        return itemService.findItemsByName(group, name);
    }

    @PostMapping("/item")
    public Mono<Boolean> addItem(@PathVariable String group, @RequestBody Item item) {
        return this.itemService.addItem(group, item);
    }

    @DeleteMapping
    public Mono<Boolean> deleteItem(
            @PathVariable String group,
            @RequestBody Item item) {
        return this.itemService.deleteItem(group, item);
    }

    @GetMapping("/{order}")
    public Mono<Item> addItem(
            @PathVariable String group,
            @PathVariable int order) {
        return this.itemService.findItemByOrder(group, order);
    }
}