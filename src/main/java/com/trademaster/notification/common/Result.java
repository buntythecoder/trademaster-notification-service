package com.trademaster.notification.common;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Result Type for Railway Programming Pattern
 * 
 * MANDATORY: Error Handling Patterns - Rule #11
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Pattern Matching - Rule #14
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    /**
     * Create successful result
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    /**
     * Create failure result
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    /**
     * Try to execute operation and wrap result
     */
    static <T> Result<T, Exception> tryExecute(Supplier<T> operation) {
        try {
            return success(operation.get());
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    /**
     * Try to execute operation that may throw checked exceptions
     */
    static <T> Result<T, Exception> tryExecuteChecked(CheckedSupplier<T> operation) {
        try {
            return success(operation.get());
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    /**
     * Functional interface for operations that may throw checked exceptions
     */
    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
    }
    
    /**
     * Check if result is successful
     */
    boolean isSuccess();
    
    /**
     * Check if result is failure
     */
    default boolean isFailure() {
        return !isSuccess();
    }
    
    /**
     * Get value if success, otherwise throw
     */
    T getValue();
    
    /**
     * Get error if failure, otherwise throw
     */
    E getError();
    
    /**
     * Get value as Optional
     */
    Optional<T> getValueOptional();
    
    /**
     * Get error as Optional
     */
    Optional<E> getErrorOptional();
    
    /**
     * Map success value to new type
     */
    <U> Result<U, E> map(Function<T, U> mapper);
    
    /**
     * FlatMap for chaining operations
     */
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);
    
    /**
     * Map error to new type
     */
    <F> Result<T, F> mapError(Function<E, F> mapper);
    
    /**
     * Filter with predicate
     */
    Result<T, E> filter(Predicate<T> predicate, Supplier<E> errorSupplier);
    
    /**
     * Execute side effect on success
     */
    Result<T, E> onSuccess(Consumer<T> action);
    
    /**
     * Execute side effect on failure
     */
    Result<T, E> onFailure(Consumer<E> action);
    
    /**
     * Pattern matching with switch expression
     */
    default <R> R match(Function<T, R> onSuccess, Function<E, R> onFailure) {
        return switch (this) {
            case Success<T, E> success -> onSuccess.apply(success.value());
            case Failure<T, E> failure -> onFailure.apply(failure.error());
        };
    }
    
    /**
     * Get value or default
     */
    T orElse(T defaultValue);
    
    /**
     * Get value or compute default
     */
    T orElseGet(Supplier<T> supplier);
    
    /**
     * Success case
     */
    record Success<T, E>(T value) implements Result<T, E> {
        
        @Override
        public boolean isSuccess() {
            return true;
        }
        
        @Override
        public T getValue() {
            return value;
        }
        
        @Override
        public E getError() {
            throw new IllegalStateException("Success result has no error");
        }
        
        @Override
        public Optional<T> getValueOptional() {
            return Optional.of(value);
        }
        
        @Override
        public Optional<E> getErrorOptional() {
            return Optional.empty();
        }
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return success(mapper.apply(value));
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return mapper.apply(value);
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> mapper) {
            return success(value);
        }
        
        @Override
        public Result<T, E> filter(Predicate<T> predicate, Supplier<E> errorSupplier) {
            return predicate.test(value) ? this : failure(errorSupplier.get());
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<T> action) {
            action.accept(value);
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<E> action) {
            return this;
        }
        
        @Override
        public T orElse(T defaultValue) {
            return value;
        }
        
        @Override
        public T orElseGet(Supplier<T> supplier) {
            return value;
        }
    }
    
    /**
     * Failure case
     */
    record Failure<T, E>(E error) implements Result<T, E> {
        
        @Override
        public boolean isSuccess() {
            return false;
        }
        
        @Override
        public T getValue() {
            throw new IllegalStateException("Failure result has no value: " + error);
        }
        
        @Override
        public E getError() {
            return error;
        }
        
        @Override
        public Optional<T> getValueOptional() {
            return Optional.empty();
        }
        
        @Override
        public Optional<E> getErrorOptional() {
            return Optional.of(error);
        }
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return failure(error);
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return failure(error);
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> mapper) {
            return failure(mapper.apply(error));
        }
        
        @Override
        public Result<T, E> filter(Predicate<T> predicate, Supplier<E> errorSupplier) {
            return this;
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<T> action) {
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<E> action) {
            action.accept(error);
            return this;
        }
        
        @Override
        public T orElse(T defaultValue) {
            return defaultValue;
        }
        
        @Override
        public T orElseGet(Supplier<T> supplier) {
            return supplier.get();
        }
    }
}