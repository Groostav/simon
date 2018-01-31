package de.root1.simon.codec.base

import de.root1.simon.codec.base.ObjectCode.*
import org.apache.mina.core.buffer.IoBuffer
import java.io.Serializable
import java.util.*
import kotlin.coroutines.experimental.buildSequence

typealias Encoder = (Any) -> String
typealias Decoder = (String) -> Any

@Suppress("UsePropertyAccessSyntax")
object UserObjectSerializer {

    val FromUTF8 = Charsets.UTF_8.newDecoder()
    val ToUTF8 = Charsets.UTF_8.newEncoder()

    private var UserDecoders: Map<Class<*>, Decoder> = emptyMap()
    private var UserEncoders: Map<Class<*>, Encoder> = emptyMap()

    @Throws(ClassNotFoundException::class)
    @JvmStatic fun readUserObject(input: IoBuffer): Any {

        val type = input.getEnum(ObjectCode::class.java)!!

        //TODO: checkbytecode for kotlin's boxing-unboxing behaviour.
        val result: Any = when(type){
            INT -> input.getInt()
            DOUBLE -> input.getDouble()
            //...etcetc
            UNKNOWN -> {
                val name = Class.forName(input.getPrefixedString(FromUTF8))
                val value = input.getObject()

                val supertypeSequence = superClassSequence(name) + superInterfaceSequence(name)
                val userDecoder = supertypeSequence.firstOrNull { it in UserDecoders.keys }?.let { UserDecoders[it] }

                return userDecoder?.invoke(value as String) ?: value
            }
        }

        return result
    }

    @JvmStatic fun writeUserObject(obj: Any, output: IoBuffer){

        val type = ObjectCode[obj]

        output.putEnum(type)

        when(type){
            INT -> output.putInt(obj as Int)
            DOUBLE -> output.putDouble(obj as Double)
            UNKNOWN -> {
                val name = obj.javaClass
                output.putPrefixedString(name.name, ToUTF8)

                val supertypeSequence = superClassSequence(name) + superInterfaceSequence(name)
                val userEncoder = supertypeSequence.firstOrNull { it in UserEncoders.keys }?.let { UserEncoders[it] }

                val value = userEncoder?.invoke(obj) ?: obj

                output.putObject(value)
            }
        }

    }
}

enum class ObjectCode {
    INT,
    DOUBLE,
    //TODO: all primatives, maybe String also?
    UNKNOWN,
    ;

    companion object {
        operator fun get(obj: Any) = when(obj){
            is Double -> DOUBLE
            is Int -> INT
            else -> UNKNOWN
        }
    }
}


private fun <T> generateBreadthFirstSequence(seed: T, children: (T) -> Iterable<T>): Sequence<T>{

    val queue: Queue<T> = LinkedList<T>().apply { add(seed) }

    return buildSequence {
        while( ! queue.isEmpty()){
            val next = queue.remove()
            yield(next)
            val newElements = children(next)
            queue.addAll(newElements)
        }
    }
}


private fun superInterfaceSequence(name: Class<*>): Sequence<Class<*>> =
        generateBreadthFirstSequence(name) { it.interfaces.asList() }

private fun superClassSequence(name: Class<*>) =
        generateSequence(name) { it.superclass.takeUnless { it == Any::class } }

internal data class CompletableFutureSurrogate(val outstandingId: Int): Serializable