package moe.yamato.dict.token;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class SimpleTokenParser implements TokenParser {
    @Override
    public Set<String> token(String input) {
        String token = input.replaceAll(filterRex(), "");
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int i = 0; i < token.length(); i++) {
            result.add(String.valueOf(token.charAt(i)));
        }
        return result;
    }

    abstract protected String filterRex();
}
