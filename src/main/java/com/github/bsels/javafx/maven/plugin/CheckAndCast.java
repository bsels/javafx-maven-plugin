package com.github.bsels.javafx.maven.plugin;

import java.util.Objects;
import java.util.stream.Gatherer;

/// The `CheckAndCast` class is a utility designed for type-checking and casting elements
/// during stream processing. It ensures that input elements match a desired target type
/// and then casts them to that type before passing them downstream. This class is implemented
/// as a record and conforms to the `Gatherer` interface.
///
/// @param <I> the input type to check and potentially cast
/// @param <O> the target type to cast input elements to
public record CheckAndCast<I, O>(Class<O> clazz) implements Gatherer<I, Void, O> {

    /// Constructs a new `CheckAndCast` instance.
    ///
    /// @param clazz the `Class` object representing the target type to cast elements to;
    ///                           must not be `null`
    /// @throws NullPointerException if `clazz` is `null`
    public CheckAndCast {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
    }

    /// Returns an [Integrator] that processes elements by checking if they are instances of the
    /// target class and casting them to the target type, if applicable.
    ///
    /// @return an [Integrator] that checks the input element type, casts it if possible, and pushes
    ///         it downstream.
    @Override
    public Integrator<Void, I, O> integrator() {
        return this::integrate;
    }

    /// Processes the input element by checking if it is an instance of the specified class and,
    /// if so, casts it to the target type and pushes it downstream.
    ///
    /// @param state      the state of the integration process (unused in this implementation)
    /// @param element    the input element to check and potentially cast
    /// @param downstream the downstream consumer that processes casted elements of the target type
    /// @return `true` if processing should continue; otherwise `false` to stop further processing
    private boolean integrate(Void state, I element, Downstream<? super O> downstream) {
        if (clazz.isInstance(element)) {
            return downstream.push(clazz.cast(element));
        }
        return true;
    }

    /// Creates an instance of `CheckAndCast` for the specified target class type.
    ///
    /// @param <I>   the input type that will be checked and potentially cast
    /// @param <O>   the target type to which elements will be cast if they match
    /// @param clazz the `Class` object representing the target type to cast elements to;
    ///                           must not be `null`
    /// @return a new `CheckAndCast` instance that checks and casts elements to the specified target class
    /// @throws NullPointerException if `clazz` is `null`
    public static <I, O> CheckAndCast<I, O> of(Class<O> clazz) {
        return new CheckAndCast<>(clazz);
    }
}
