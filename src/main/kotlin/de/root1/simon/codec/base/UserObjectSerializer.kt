package de.root1.simon.codec.base

import de.root1.simon.codec.base.ObjectCode.*
import org.apache.mina.core.buffer.IoBuffer
import java.io.Serializable
import java.util.*
import kotlin.coroutines.experimental.buildSequence

typealias Encoder<T> = (T) -> String
typealias Decoder<T> = (String) -> T


//TODO: convert this to an injected object.
// ok so then, what data type is the most resiliant to serialization? --as in, which serializer is most serializable?
//    - remember that if somebody creates a simon instance with some set of serializers, how do we know that the client has the same set?
//      do we leave that to user configuration? what kinds of errors should the expect if they fuckup the configuration?
//
// one strategy might be to serialize serializers.
//    - This is what mapDB did, and then of course stopped.
//    - in this sense, if you add something client side, it will request loading server side. That gives a nice error, doesnt it?
//      well, except that closures (ie anon inner classes) are going to need constructor arguments, almost always.
//
// best bet might be to detect serialization smartly, with a single bit on the message indicating
// if the default strategy was used or if a custom one was used, then, on the other side, when deserializing,
// if the bit is set but no custom serializer is found it logs a warning/severe.
// this doesnt cover bad configuration, but it does cover _forgetting_ to do configuration.

@Suppress("UsePropertyAccessSyntax")
object UserObjectSerializer {

    val FromUTF8 = Charsets.UTF_8.newDecoder()
    val ToUTF8 = Charsets.UTF_8.newEncoder()

    private var UserDecoders: Map<Class<*>, Decoder<Any>> = emptyMap()
    private var UserEncoders: Map<Class<*>, Encoder<Any>> = emptyMap()

    @JvmStatic fun <T> addSerializer(type: Class<T>, encoder: Encoder<T>, decoder: Decoder<T>){
        UserEncoders += type to (encoder as Encoder<Any>)
        UserDecoders += type to (decoder as Decoder<Any>)
    }

    @JvmStatic fun clear(){
        UserEncoders = emptyMap()
        UserDecoders = emptyMap()
    }

    @Throws(ClassNotFoundException::class)
    @JvmStatic fun readUserObject(input: IoBuffer): Any? {

        val type = input.getEnum(ObjectCode::class.java)!!

        //TODO: check bytecode for kotlin's boxing-unboxing behaviour.
        val result: Any? = when(type){
            NULL -> null

            BOOL -> input.get() == TRUE_BYTE

            BYTE -> input.get()
            SHORT -> input.getShort()
            INT -> input.getInt()
            LONG -> input.getLong()

            FLOAT -> input.getFloat()
            DOUBLE -> input.getDouble()

            CHAR -> input.getChar()
            STRING -> input.getPrefixedString(4, FromUTF8)

            UNKNOWN -> {
                val name = input.getPrefixedString(FromUTF8)
                val type = Class.forName(name)
                val value = input.getObject()

                val supertypeSequence = superClassSequence(type) + superInterfaceSequence(type)
                val userDecoder = supertypeSequence.firstOrNull { it in UserDecoders.keys }?.let { UserDecoders[it] }

                return userDecoder?.invoke(value as String) ?: value
            }
        }

        return result
    }

    @JvmStatic fun writeUserObject(obj: Any?, output: IoBuffer){

        val type = ObjectCode[obj]

        output.putEnum(type)

        when(type){
            NULL -> { /*noop, written header will signal reader*/ }

            BOOL -> output.put(if(obj as Boolean) TRUE_BYTE else FALSE_BYTE)

            BYTE -> output.put(obj as Byte)
            SHORT -> output.putShort(obj as Short)
            INT -> output.putInt(obj as Int)
            LONG -> output.putLong(obj as Long)

            FLOAT -> output.putFloat(obj as Float)
            DOUBLE -> output.putDouble(obj as Double)

            CHAR -> output.putChar(obj as Char)
            STRING -> output.putPrefixedString(obj as String, 4, ToUTF8)

            UNKNOWN -> {
                val name = obj!!.javaClass
                output.putPrefixedString(name.name, ToUTF8)

                val supertypeSequence = superClassSequence(name) + superInterfaceSequence(name)
                val userEncoder = supertypeSequence.firstOrNull { it in UserEncoders.keys }?.let { UserEncoders[it] }

                val value: Any = userEncoder?.invoke(obj) ?: obj

                output.putObject(value)
            }
        }

    }
}

enum class ObjectCode {
    NULL,

    BOOL,

    BYTE,
    SHORT,
    INT,
    LONG,

    FLOAT,
    DOUBLE,

    CHAR,
    STRING,

    // other potential special cases:
    // exceptions - dont do this, since a user exception could have custom serialization rules.
    //

    UNKNOWN,
    ;

    companion object {
        @JvmStatic operator fun get(obj: Any?) = when(obj){
            null -> NULL

            is Boolean -> BOOL

            is Byte -> BYTE
            is Short -> SHORT
            is Int -> INT
            is Long -> LONG

            is Float -> FLOAT
            is Double -> DOUBLE

            is Char -> CHAR
            is String -> STRING

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

val FALSE_BYTE = 0.toByte()
val TRUE_BYTE = 1.toByte()