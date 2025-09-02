package com.pavit.bundl.domain.usecase.base

/**
 * Base interface for use cases that don't take parameters
 */
interface UseCase<out T> {
    suspend operator fun invoke(): Result<T>
}

/**
 * Base interface for use cases that take parameters
 */
interface ParameterizedUseCase<in P, out T> {
    suspend operator fun invoke(parameters: P): Result<T>
}

/**
 * Base interface for use cases that don't return data (void operations)
 */
interface VoidUseCase {
    suspend operator fun invoke(): Result<Unit>
}

/**
 * Base interface for use cases with parameters that don't return data
 */
interface VoidParameterizedUseCase<in P> {
    suspend operator fun invoke(parameters: P): Result<Unit>
}
