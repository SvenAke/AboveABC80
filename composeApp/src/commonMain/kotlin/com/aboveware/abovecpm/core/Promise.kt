/*
 * Copyright (c) aboveWare 2020.
 */

package com.aboveware.abovecpm.core

import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

class PromiseCheckError : Exception()

class DispatchQueue(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) {
    fun async(work: () -> Unit) {
        MainScope().launch(dispatcher) { work() }
    }

    fun asyncAfter(duration: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, work: () -> Unit) {
        MainScope().launch(dispatcher) {
            delay(timeUnit.toMillis(duration).milliseconds)
            work()
        }
    }
}

class Callback<Value>(
    dispatcher: CoroutineDispatcher,
    val onFulfilled: (Value) -> Unit,
    val onRejected: (Exception) -> Unit
) {
    private val executionContext = DispatchQueue(dispatcher)

    fun callFulfill(value: Value, completion: () -> Unit = { }) {
        executionContext.async {
            onFulfilled(value)
            completion()
        }
    }

    fun callReject(error: Exception, completion: () -> Unit = { }) {
        executionContext.async {
            onRejected(error)
            completion()
        }
    }
}

class State<Value>() {

    enum class InnerState {
        Pending, Fulfilled, Rejected
    }

    var state = InnerState.Pending
    var value: Value? = null
    var error: Exception? = null
    val callbacks: MutableList<Callback<Value>> = mutableListOf()

    constructor(error: Exception) : this() {
        rejected(error)
    }

    constructor(value: Value) : this() {
        fulfilled(value)
    }

    fun pending(callback: Callback<Value>) {
        callbacks.add(callback)
        state = InnerState.Pending
    }

    fun fulfilled(value: Value) {
        this.value = value
        state = InnerState.Fulfilled
    }

    fun rejected(error: Exception) {
        this.error = error
        state = InnerState.Rejected
    }

    val isPending: Boolean
        get() = state == InnerState.Pending

    val isFulfilled: Boolean
        get() = state == InnerState.Fulfilled

    val isRejected: Boolean
        get() = state == InnerState.Rejected
}

class
Promise<Value>() {
    private var state = State<Value>()

    constructor(value: Value) : this() {
        state.fulfilled(value)
    }

    constructor(error: Exception) : this() {
        state.rejected(error)
    }

    constructor(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        work: (fulfill: (Value) -> Unit, reject: (Exception) -> Unit) -> Unit
    ) : this() {
        DispatchQueue(dispatcher).async {
            try {
                work(::fulfill, ::reject)
            } catch (error: Exception) {
                reject(error)
            }
        }
    }

    fun reject(error: Exception) {
        updateState(State(error))
    }

    fun fulfill(value: Value) {
        updateState(State(value))
    }

    val value: Value?
        get() = state.value

    val error: Exception?
        get() = state.error

    val isPending: Boolean
        get() = !isFulfilled && !isRejected

    private val isFulfilled: Boolean
        get() = value != null

    val isRejected: Boolean
        get() = error != null

    private fun updateState(newState: State<Value>) {
        if (state.isPending) {
            val callbacks = state.callbacks
            state = newState
            fireIfCompleted(callbacks)
        }
    }

    fun `catch`(onRejected: (Exception) -> Unit): Promise<Value> =
        then(Dispatchers.Default, { }, onRejected)

    fun `catch`(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        onRejected: (Exception) -> Unit
    ): Promise<Value> = then(dispatcher, { }, onRejected)

    fun <E : Exception> `catch`(kClass: KClass<E>, onRejected: (E) -> Unit): Promise<Value> {
        return this.catch { error ->
            @Suppress("UNCHECKED_CAST")
            onRejected(error as E)
        }
    }

    fun then(onFulfilled: (Value) -> Unit, onRejected: (Exception) -> Unit = {}): Promise<Value> {
        addCallbacks(onFulfilled, onRejected, Dispatchers.Default)
        return this
    }

    fun then(
        dispatcher: CoroutineDispatcher = Dispatchers.Default, onFulfilled: (Value) -> Unit,
        onRejected: (Exception) -> Unit = {}
    ): Promise<Value> {
        addCallbacks(onFulfilled, onRejected, dispatcher)
        return this
    }

    fun <NewValue> map(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        onFulfilled: (Value) -> Promise<NewValue>
    ): Promise<NewValue> {
        return Promise { fulfill, reject ->
            addCallbacks({ value ->
                try {
                    onFulfilled(value).then(dispatcher, fulfill, reject)
                } catch (error: Exception) {
                    reject(error)
                }
            }, reject, dispatcher)
        }
    }

    fun <NewValue> then(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        onFulfilled: (Value) -> NewValue
    ): Promise<NewValue> {
        return map { value ->
            try {
                Promise(onFulfilled(value))
            } catch (error: Exception) {
                Promise(error)
            }
        }
    }

    fun always(dispatcher: CoroutineDispatcher = Dispatchers.Default, onComplete: () -> Unit) =
        then(dispatcher, {
            onComplete()
        }, {
            onComplete()
        })

    fun addTimeout(timeout: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Promise<Value> =
        Promises.race(this, Promises.timeout(timeout, timeUnit))

    private fun addCallbacks(
        onFulfilled: (Value) -> Unit,
        onRejected: (Exception) -> Unit,
        dispatcher: CoroutineDispatcher
    ) {
        val callback = Callback(dispatcher, onFulfilled, onRejected)
        when {
            state.isPending -> state.pending(callback)
            state.isFulfilled -> state.value?.let { value ->
                callback.callFulfill(value)
            }

            state.isRejected -> state.error?.let { error ->
                callback.callReject(error)
            }
        }
    }

    @Synchronized
    private fun fireIfCompleted(callbacks: MutableList<Callback<Value>>) {
        if (callbacks.isNotEmpty() && !state.isPending) {
            if (state.isFulfilled) {
                val mutableCallbacks = callbacks.toMutableList()
                val callback = mutableCallbacks.removeAt(0)
                state.value?.let { value ->
                    callback.callFulfill(value) {
                        fireIfCompleted(mutableCallbacks)
                    }
                }
            } else if (state.isRejected) {
                val mutableCallbacks = callbacks.toMutableList()
                val callback = mutableCallbacks.removeAt(0)
                state.error?.let { error ->
                    callback.callReject(error) {
                        fireIfCompleted(mutableCallbacks)
                    }
                }
            }
        }
    }

    fun recover(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        recovery: (Exception) -> Promise<Value>
    ): Promise<Value> = recover(Exception::class, dispatcher, recovery)

    inline fun <reified E : Exception> recover(
        kClass: KClass<E>, dispatcher: CoroutineDispatcher = Dispatchers.Default,
        crossinline recovery: (E: Exception) -> Promise<Value>
    ): Promise<Value> {
        return Promise { fulfill, reject ->
            this.then(fulfill).catch { anyError ->
                when (anyError) {
                    is E ->
                        try {
                            recovery(anyError).then(dispatcher, fulfill, reject)
                        } catch (exception: Exception) {
                            reject(Exception("caught", exception))
                        }

                    else ->
                        reject(anyError)
                }
            }
        }
    }

    fun ensure(check: (Value) -> Boolean): Promise<Value> {
        return this.then { value ->
            if (!check(value)) {
                throw PromiseCheckError()
            }
            value
        }
    }

    /**
     * Do all in the iterator with a promise.
     */
    fun <T> iterator(
        iterator: Iterator<T>,
        function: (it: T) -> Promise<Boolean>
    ): Promise<Boolean> {
        val promise = Promise<Boolean>()
        if (iterator.hasNext()) {
            function(iterator.next()).then {
                if (it) iterator(iterator, function).then { it -> promise.fulfill(it) }
                else promise.fulfill(false)
            }
        } else promise.fulfill(true)
        return promise
    }

    fun mapError(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        transformError: (Exception) -> Exception
    ) =
        mapError(Exception::class, dispatcher, transformError)

    private inline fun <reified E : Exception> mapError(
        kClass: KClass<E>, dispatcher: CoroutineDispatcher = Dispatchers.Default,
        crossinline transformError: (E) -> Exception
    ) = recover(kClass, dispatcher) { error ->
        Promise(transformError(error as E))
    }

    class Promises {
        companion object {

            /// Wait for all the promises you give it to fulfill, and once they have, fulfill itself
            /// with the array of all fulfilled values.
            fun <T> all(promises: Iterable<Promise<T>>): Promise<MutableList<T>> {
                return Promise { fulfill, reject ->
                    if (promises.count() == 0) {
                        fulfill(mutableListOf())
                    } else {
                        promises.forEach { promise ->
                            promise.then { _ ->
                                if (null == promises.find { it.isRejected || it.isPending }) {
                                    val map = mutableListOf<T>()
                                    promises.forEach { promise ->
                                        promise.value?.let {
                                            @Suppress("UNCHECKED_CAST")
                                            map.add(promise.value as T)
                                        }
                                        fulfill(map)
                                    }
                                }
                            }.catch { error ->
                                reject(error)
                            }
                        }
                    }
                }
            }

            fun <T> all(vararg input: Promise<T>): Promise<MutableList<T>> =
                all(mutableListOf(*input))

            /// This promise will be rejected after a delay.
            fun <T> timeout(timeout: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Promise<T> =
                Promise { _, reject ->
                    delay(timeout, timeUnit).then {
                        reject(Exception("Timed out"))
                    }
                }

            /// Fulfills or rejects with the first promise that completes
            /// (as opposed to waiting for all of them, like `.all()` does).
            private fun <T> race(promises: Iterable<Promise<out T>>) = Promise { fulfill, reject ->
                promises.forEach {
                    it.then(fulfill, reject)
                }
            }

            fun <T> race(vararg input: Promise<out T>): Promise<T> = race(mutableListOf(*input))

            fun delay(
                delay: Long,
                timeUnit: TimeUnit = TimeUnit.MILLISECONDS
            ): Promise<() -> Unit> = Promise { fulfill, _ ->
                DispatchQueue().asyncAfter(delay, timeUnit) {
                    fulfill {}
                }
            }

            fun <T> retry(count: Int, delay: Long, generate: () -> Promise<T>): Promise<T> {
                if (count <= 0) {
                    return generate()
                }
                return Promise { fulfill, reject ->
                    generate().recover {
                        delay(delay).map {
                            retry(count - 1, delay, generate)
                        }
                    }.then(fulfill).catch(reject)
                }
            }

            private fun <T, U> zip(first: Promise<T>, second: Promise<U>): Promise<Pair<T, U>> {
                return Promise { fulfill, reject ->
                    val resolver: (Any) -> Unit = {
                        first.value?.let { firstValue ->
                            second.value?.let { secondValue ->
                                fulfill(Pair(firstValue, secondValue))
                            }
                        }
                    }
                    @Suppress("UNCHECKED_CAST")
                    first.then(resolver as (T) -> Unit, reject)
                    @Suppress("UNCHECKED_CAST")
                    second.then(resolver as (U) -> Unit, reject)
                }
            }

            private fun <T1, T2, T3> zip(
                p1: Promise<T1>,
                p2: Promise<T2>,
                last: Promise<T3>
            ): Promise<Triple<T1, T2, T3>> {
                return Promise { fulfill, reject ->
                    val zipped = zip(p1, p2)
                    fun resolver() {
                        zipped.value?.let { zippedValue ->
                            last.value?.let { lastValue ->
                                fulfill(Triple(zippedValue.first, zippedValue.second, lastValue))
                            }
                        }
                    }
                    zipped.then({ resolver() }, reject)
                    last.then({ resolver() }, reject)
                }
            }

            data class Quadruple<T1, T2, T3, T4>(
                val first: T1,
                val second: T2,
                val third: T3,
                val forth: T4
            )

            fun <T1, T2, T3, T4> zip(
                p1: Promise<T1>,
                p2: Promise<T2>,
                p3: Promise<T3>,
                last: Promise<T4>
            ): Promise<Quadruple<T1, T2, T3, T4>> {
                return Promise { fulfill, reject ->
                    val zipped = zip(p1, p2, p3)
                    fun resolver() {
                        zipped.value?.let { zippedValue ->
                            last.value?.let { lastValue ->
                                fulfill(
                                    Quadruple(
                                        zippedValue.first,
                                        zippedValue.second,
                                        zippedValue.third,
                                        lastValue
                                    )
                                )
                            }
                        }
                    }
                    zipped.then({ resolver() }, reject)
                    last.then({ resolver() }, reject)
                }
            }

            fun <T> kickoff(block: () -> T): Promise<T> = try {
                Promise(block())
            } catch (e: Exception) {
                Promise(e)
            }
        }
    }
}