package io.github.open_policy_agent.opa.gson;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.open_policy_agent.opa.metrics.Metrics;
import io.github.open_policy_agent.opa.metrics.SimpleMetrics;
import org.junit.jupiter.api.Test;

class MetricsTypeAdapterFactoryTest {

  private final Gson gson =
      new GsonBuilder().registerTypeAdapterFactory(new MetricsTypeAdapterFactory()).create();

  @Test
  void timer_serializesAsNanoseconds() {
    SimpleMetrics metrics = new SimpleMetrics();
    Metrics.Timer timer = metrics.timer("rego_query_eval");
    timer.start();
    timer.stop();

    // Same unit as the timer_<name>_ns keys written by DecisionLogPlugin.
    assertThat(gson.toJson(timer)).isEqualTo(Long.toString(timer.value().toNanos()));
  }

  @Test
  void counter_serializesAsItsValue() {
    SimpleMetrics metrics = new SimpleMetrics();
    Metrics.Counter counter = metrics.counter("server_query_cache_hit");
    counter.add(3);

    assertThat(gson.toJson(counter)).isEqualTo("3");
  }

  @Test
  void histogram_serializesAsItsStats() {
    SimpleMetrics metrics = new SimpleMetrics();
    Metrics.Histogram histogram = metrics.histogram("sizes");
    histogram.update(11);

    String histogramJson = gson.toJson(histogram);

    assertThat(histogramJson).isEqualTo(gson.toJson(histogram.value()));
    assertThat(histogramJson).contains("\"count\":1", "\"99%\":11");
  }

  @Test
  void otherTypes_areLeftToGson() {
    assertThat(gson.toJson("plain")).isEqualTo("\"plain\"");
  }
}
