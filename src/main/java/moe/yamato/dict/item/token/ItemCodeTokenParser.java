package moe.yamato.dict.item.token;

import moe.yamato.dict.token.SimpleTokenParser;
import org.springframework.stereotype.Component;

@Component
public class ItemCodeTokenParser extends SimpleTokenParser {

    @Override
    protected String filterRex() {
        return "[^a-zA-Z0-9]";
    }
}
