package moe.yamato.dict.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ItemMetrics implements MeterBinder {

    AtomicInteger count = new AtomicInteger(0);

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("item.search.count", count, AtomicInteger::incrementAndGet)
                .tags("host", "localhost")
                .description("item search count")
                .register(registry);
    }
}
