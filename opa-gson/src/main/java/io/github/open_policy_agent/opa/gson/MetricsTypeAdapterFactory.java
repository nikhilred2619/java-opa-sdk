package io.github.open_policy_agent.opa.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.open_policy_agent.opa.metrics.Metrics;
import io.github.open_policy_agent.opa.metrics.SimpleMetrics;
import java.io.IOException;

/**
 * Gson {@link TypeAdapterFactory} that serializes {@link Metrics} values the way OPA reports them,
 * mirroring opa-jackson's {@code MetricsModule}: {@link Metrics.Timer} as its value in nanoseconds
 * (matching the {@code timer_<name>_ns} keys written by the decision log plugin), {@link
 * Metrics.Counter} as its {@code int} value and {@link Metrics.Histogram} as its {@link
 * Metrics.Histogram.Values} stats. Without it, Gson serializes the implementation classes, such as
 * the ones returned by {@link SimpleMetrics}, as default beans.
 *
 * <p>The adapters only write; metrics are not meant to be read back from JSON.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * Gson gson = new GsonBuilder().registerTypeAdapterFactory(new MetricsTypeAdapterFactory()).create();
 * String json = gson.toJson(simpleMetrics.timer("foo"));
 * }</pre>
 */
public class MetricsTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Class<? super T> raw = type.getRawType();
    if (Metrics.Timer.class.isAssignableFrom(raw)) {
      return (TypeAdapter<T>) new TimerAdapter();
    }
    if (Metrics.Counter.class.isAssignableFrom(raw)) {
      return (TypeAdapter<T>) new CounterAdapter();
    }
    if (Metrics.Histogram.class.isAssignableFrom(raw)) {
      return (TypeAdapter<T>) new HistogramAdapter(gson.getAdapter(Metrics.Histogram.Values.class));
    }
    return null;
  }

  private static UnsupportedOperationException readUnsupported() {
    return new UnsupportedOperationException("metrics cannot be deserialized");
  }

  private static final class TimerAdapter extends TypeAdapter<Metrics.Timer> {
    @Override
    public void write(JsonWriter out, Metrics.Timer timer) throws IOException {
      if (timer == null) {
        out.nullValue();
        return;
      }
      out.value(timer.value().toNanos());
    }

    @Override
    public Metrics.Timer read(JsonReader in) {
      throw readUnsupported();
    }
  }

  private static final class CounterAdapter extends TypeAdapter<Metrics.Counter> {
    @Override
    public void write(JsonWriter out, Metrics.Counter counter) throws IOException {
      if (counter == null) {
        out.nullValue();
        return;
      }
      out.value(counter.value());
    }

    @Override
    public Metrics.Counter read(JsonReader in) {
      throw readUnsupported();
    }
  }

  private static final class HistogramAdapter extends TypeAdapter<Metrics.Histogram> {
    private final TypeAdapter<Metrics.Histogram.Values> valuesAdapter;

    HistogramAdapter(TypeAdapter<Metrics.Histogram.Values> valuesAdapter) {
      this.valuesAdapter = valuesAdapter;
    }

    @Override
    public void write(JsonWriter out, Metrics.Histogram histogram) throws IOException {
      if (histogram == null) {
        out.nullValue();
        return;
      }
      valuesAdapter.write(out, histogram.value());
    }

    @Override
    public Metrics.Histogram read(JsonReader in) {
      throw readUnsupported();
    }
  }
}
