package guru.springframework.reactivemongo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.Sink;
import org.zalando.logbook.json.JsonHttpLogFormatter;
import org.zalando.logbook.logstash.LogstashLogbackSink;

@Configuration
public class LogBookConfig {
    @Bean
    public Sink logBookLogStash() {
        HttpLogFormatter jsonHttpLogFormatter = new JsonHttpLogFormatter();
        LogstashLogbackSink logstashLogbackSink = new LogstashLogbackSink(jsonHttpLogFormatter);
        return logstashLogbackSink;
    }
}
