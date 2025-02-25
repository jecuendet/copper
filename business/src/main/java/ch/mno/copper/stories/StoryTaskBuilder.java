package ch.mno.copper.stories;

import ch.mno.copper.collect.AbstractCollectorWrapper;
import ch.mno.copper.collect.StoryTask;
import ch.mno.copper.collect.StoryTaskImpl;
import ch.mno.copper.report.AbstractReporterWrapper;
import ch.mno.copper.store.ValuesStore;
import ch.mno.copper.stories.data.Story;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class StoryTaskBuilder {

    public static final int TIMEOUT_SEC = 10;
    private static Logger LOG = LoggerFactory.getLogger(StoryTaskBuilder.class);

    public static StoryTask build(Story story, ValuesStore valuesStore) {
        return new StoryTaskImpl(story, () -> {
            // This code execute at every trigger (cron, ...) for the given story


            // Collect
            Map<String, String> values = collect(story, valuesStore);

            // When
            if (!story.matchWhen(values, valuesStore)) {
                // Not matching WHEN->ignore report
                return;
            }

            // Should never happen
            if (values == null) {
                LOG.warn("Story " + story.getName() + " returned null values; trying to continue with valuesStore...");
                values = valuesStore.getValuesMapString();
            }

            // Report
            AbstractReporterWrapper reporter = story.getReporterWrapper();
            if (reporter == null) {
                values.forEach((key, value) -> valuesStore.put(key, value));
            } else {
                reporter.execute(values, valuesStore);
            }
        }, story.getCron());
    }

    private static Map<String, String> collect(Story story, ValuesStore valuesStore) {
        AbstractCollectorWrapper collectorWrapper = story.getCollectorWrapper();
        if (collectorWrapper == null) { // Null means to readInstant value store
            Map<String, String> values = new HashMap<>();
            values.putAll(valuesStore.getValuesMapString());
            return values;
        }

        // Execute with timeout
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Map<String, String>> future = executor.submit(() -> {
            try {
                return collectorWrapper.execute();
            } catch (Exception e) {
                return buildValuesMapAsError( collectorWrapper);
            }
        });
        try {
            return future.get(TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Log and set values as ERR
            LOG.warn("Collector of story " + story.getName() + " error: " + e.getMessage());
            if (LOG.isTraceEnabled()) {
                LOG.trace("Exception", e);
            }
            // Put all values as error
            return buildValuesMapAsError(collectorWrapper);
        }
    }

    private static Map<String, String> buildValuesMapAsError(AbstractCollectorWrapper collectorWrapper) {
        return collectorWrapper.getAs().stream().collect(Collectors.toMap(as->as, as->"ERR"));
    }

}
