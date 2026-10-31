package io.github.open_policy_agent.opa.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.open_policy_agent.opa.metrics.Metrics;
import io.github.open_policy_agent.opa.metrics.SimpleMetrics;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class MetricsModuleTest {

  private final ObjectMapper mapper =
      JsonMapper.builder().addModule(new MetricsModule()).build();

  @Test
  void timer_serializesAsNanoseconds() {
    SimpleMetrics metrics = new SimpleMetrics();
    Metrics.Timer timer = metrics.timer("rego_query_eval");
    timer.start();
    timer.stop();

    // Same unit as the timer_<name>_ns keys written by DecisionLogPlugin, not Jackson's
    // fractional-seconds Duration format.
    assertThat(mapper.writeValueAsString(timer)).isEqualTo(Long.toString(timer.value().toNanos()));
  }

  @Test
  void counter_serializesAsItsValue() {
    SimpleMetrics metrics = new SimpleMetrics();
    Metrics.Counter counter = metrics.counter("server_query_cache_hit");
    counter.add(3);

    // Without the mixin a Counter has no Jackson-visible property and serialization fails
    // outright, so asserting the shape also asserts that it serializes at all.
    assertThat(mapper.writeValueAsString(counter)).isEqualTo("3");
  }

  @Test
  void histogram_serializesAsItsStats() {
    SimpleMetrics metrics = new SimpleMetrics();
    Metrics.Histogram histogram = metrics.histogram("sizes");
    histogram.update(11);

    String histogramJson = mapper.writeValueAsString(histogram);

    assertThat(histogramJson).isEqualTo(mapper.writeValueAsString(histogram.value()));
    assertThat(histogramJson).contains("\"count\":1", "\"99%\":11");
  }
}
