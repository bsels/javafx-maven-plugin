package io.github.bsels.javafx.maven.plugin.utils;

import java.util.Objects;
import java.util.stream.Gatherer;

/// Utility for type-checking and casting elements during stream processing.
///
/// @param <I>   The input type
/// @param <O>   The target type
/// @param clazz The target class type
public record CheckAndCast<I, O>(Class<O> clazz) implements Gatherer<I, Void, O> {

    /// Initializes a new [CheckAndCast] record instance.
    ///
    /// @param clazz The target class type
    /// @throws NullPointerException If `clazz` is null
    public CheckAndCast {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
    }

    /// Creates a [CheckAndCast] instance for the specified target class.
    ///
    /// @param <I>   The input type
    /// @param <O>   The target type
    /// @param clazz The target class type
    /// @return A new [CheckAndCast] instance
    /// @throws NullPointerException If `clazz` is null
    public static <I, O> CheckAndCast<I, O> of(Class<O> clazz) {
        return new CheckAndCast<>(clazz);
    }

    /// Returns an [Integrator] that checks instance types and casts elements.
    ///
    /// @return An [Integrator] for the gatherer
    @Override
    public Integrator<Void, I, O> integrator() {
        return this::integrate;
    }

    /// Processes the input element by checking if it is an instance of the specified class and,
    /// if so, casts it to the target type and pushes it downstream.
    ///
    /// @param ignored    the state of the integration process (unused in this implementation)
    /// @param element    the input element to check and potentially cast
    /// @param downstream the downstream consumer that processes elements of the target type
    /// @return `true` if processing should continue; otherwise `false` to stop further processing
    private boolean integrate(Void ignored, I element, Downstream<? super O> downstream) {
        if (clazz.isInstance(element)) {
            return downstream.push(clazz.cast(element));
        }
        return !downstream.isRejecting();
    }
}
