package de.root1.simon.codec.base

import java.util.concurrent.CompletableFuture

fun <T> MutableMap<Int, CompletableFuture<T>>.complete(id: Int, value: T?, exception: Throwable?): Unit {

    assert((value == null) xor (exception == null))

    var existingFuture: CompletableFuture<T>? = null

    synchronized(this) {
        if (containsKey(id)) {
            existingFuture = getValue(id)
        }
        else {
            val newFuture = CompletableFuture<T>().apply {
                if(exception != null) completeExceptionally(exception)
                else complete(value)
            }

            put(id, newFuture)
        }
    }

    existingFuture?.apply {
        if(exception != null) completeExceptionally(exception)
        else complete(value)
    }
}