package moe.yamato.dict.item.token;

import moe.yamato.dict.token.SimpleTokenParser;
import org.springframework.stereotype.Component;

@Component
public class ItemNameTokenParser extends SimpleTokenParser {

    @Override
    protected String filterRex() {
        return "[^\\u4e00-\\u9fa5]";
    }
}
