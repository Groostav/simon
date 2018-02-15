package d1.root1.simon.test

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.thoughtworks.xstream.XStream
import de.root1.simon.Lookup
import de.root1.simon.Registry
import de.root1.simon.Simon
import de.root1.simon.annotation.SimonRemote
import de.root1.simon.codec.base.UserObjectSerializer
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.time.delay
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.experimental.buildSequence

interface Service {

    fun executeRun(data: Int): CompletableFuture<Double>
    fun executeExceptionRun(data: Int): CompletableFuture<Double>

    // starting to look like our actual code!
    fun executeDependentRuns(dag: Node): CompletableFuture<String>

    fun <T> identity(input: T): T
}

class BlamException(val data: Int): RuntimeException("Blam $data!")

@SimonRemote(Service::class)
class ServiceImpl: Service {

    override fun executeRun(data: Int) = future<Double> {
        42.0 + data
    }

    override fun executeExceptionRun(data: Int) = future<Double> {
        throw BlamException(data)
    }

    override fun executeDependentRuns(dag: Node) = future<String> {
        dag.bfs().mapIndexed { idx, v -> v.value + idx }.joinToString("-")
    }

    override fun <T> identity(input: T): T = input
}

data class Node(val value: String, val parents: List<Node> = emptyList(), var children: List<Node> = emptyList()){

    override fun toString() = value //avoid stack-overflow
    override fun hashCode() = value.hashCode()
    override fun equals(other: Any?) = value == (other as? Node)?.value

    fun deepEquals(other: Node) = bfs().toList() == other.bfs().toList()
}

/**
 * generates a sequence representing a flattened depedency-aware pre-order traversal
 */
fun Node.bfs(): Sequence<Node> {
    val start = generateSequence(this) { it.parents.firstOrNull() }.last()
    val queue: Queue<Node> = LinkedList<Node>().also { it.add(start) }
    val visited = HashSet<Node>()

    fun Node.hasUnvisitedParent() = (parents - visited).any()

    return buildSequence {
        while( ! queue.isEmpty()){

            while(queue.any() && (queue.peek() in visited || queue.peek().hasUnvisitedParent())){
                queue.remove()
            }

            if(queue.isEmpty()){ return@buildSequence }

            val next = queue.remove()

            yield(next)

            visited += next
            queue += next.children
        }
    }
}

/**
 * A simple node DAG that is in the form
 * ```
 *     top
 *    /  \
 * left   right
 *    \  /
 *   bottom
 * ```
 */
val DiamondDag = Node("top").apply top@ {
    children = listOf(
            Node("left", listOf(this@top)),
            Node("right", listOf(this@top))
    )

    val bottom = Node("bottom")
    children.forEach { child -> child.children += bottom }
}

class ThingyEmpoweropsTest {

    companion object {

        private lateinit var registry: Registry
        private lateinit var lookup: Lookup

        @BeforeClass @JvmStatic fun setupServer(){
            val serviceImpl = ServiceImpl()

            registry = Simon.createRegistry().apply { start() }

            registry.bind("service", serviceImpl)

            //technically this is 'setup client'
            lookup = Simon.createNameLookup("127.0.0.1")
        }

        @AfterClass @JvmStatic fun teardownServer(){
            registry.stop()
        }
    }

    @After fun `clean up user supplied encoding decoding`(){ UserObjectSerializer.clear() }

    @Test fun `when using simple happy path completable future should properly send and recieve values`() = runBlocking {
        val service = lookup.lookup("service") as Service
        val futureResult = service.executeRun(42)

        val result: Double = futureResult.await()

        assert(result == 84.0)
    }

    @Test fun `when using exceptional future should properly send and recieve values`() = runBlocking {

        val service = lookup.lookup("service") as Service
        val rawExceptionResult = service.executeExceptionRun(43);

        val exception: Exception =
                try { rawExceptionResult.await().let { TODO("result is $it") } }
                catch (ex: BlamException) { ex }

        assert(exception.message == "Blam 43!")
    }

    @Test fun `when using stupid dag code should bfs properly`(){
        assert(DiamondDag.bfs().joinToString("-") == "top-left-right-bottom")
    }

    @Test fun `when using a complex dag should properly encode and decode`(){
        val xstream = XStream()
        UserObjectSerializer.addSerializer(Node::class.java, { xstream.toXML(it) }, { xstream.fromXML(it) as Node })
        val service = lookup.lookup("service") as Service

        val rawExceptionResult = service.identity(DiamondDag)

        assert(rawExceptionResult == DiamondDag && rawExceptionResult !== DiamondDag)
    }

    @Test fun `when attempting to call code with complex argument and lazy result should properly return to me!`() = runBlocking {

        val xstream = XStream()
        UserObjectSerializer.addSerializer(Node::class.java, { xstream.toXML(it) }, { xstream.fromXML(it) as Node })

        val service = lookup.lookup("service") as Service
        val result = service.executeDependentRuns(DiamondDag).await()

        assert(result == "top0-left1-right2-bottom3")
        //neat.
    }
}

object EventBusTests {
//object EventBusTests {

    @JvmStatic fun main(args: Array<String>){
//    @Test fun `when attempting to move event busses around`(){
        ServerSide.run()
        ClientSide.run()
    }

}

interface ProxyableEventSource{
    fun register(wrapper: ProxyableEventSink)
}

@SimonRemote class ProxyableEventSourceImpl(val eventBus: EventBus): ProxyableEventSource {

   override fun register(wrapper: ProxyableEventSink){
     eventBus.register(wrapper)
   }

}

interface ProxyableEventSink{
    @Subscribe fun syndicateOn(obj: Any)
}
@SimonRemote class ProxyableEventSinkImpl(val eventBus: EventBus): ProxyableEventSink {

   override fun syndicateOn(obj: Any){
     eventBus.post(obj)
   }

}


object ClientSide {

    fun run(){
        val lookup = Simon.createNameLookup("127.0.0.1")

        val eventBus = EventBus("global event bus --client")

        val proxy = lookup.lookup("global event bus") as ProxyableEventSource
        proxy.register(ProxyableEventSinkImpl(eventBus))

        EventBusDuder("Client", eventBus)
    }
}

object ServerSide {

    fun run(){

        val registry = Simon.createRegistry().apply { start() }
        val lookup = Simon.createNameLookup("127.0.0.1")

        val eventBus = EventBus("global event bus")
        val eventBusProxy = ProxyableEventSourceImpl(eventBus)

        registry.bind(eventBus.identifier(), eventBusProxy)

        EventBusDuder("Server", eventBus)
    }

}
private class EventBusDuder(val name: String, val eventBus: EventBus) {

    init {
        eventBus.register(this)
    }

    init {
        launch {
            while(true) {
                delay(5.sec)

                println("$name is posting")
                eventBus.post("Blam-from-$name!")
            }
        }
    }

    @Subscribe fun logMessageOn(event: String){
        println("$name saw message '$event'")
    }
}


val Int.sec: Duration get() = Duration.ofSeconds(this.toLong())