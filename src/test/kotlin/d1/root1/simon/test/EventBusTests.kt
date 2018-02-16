package d1.root1.simon.test

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import de.root1.simon.Simon
import de.root1.simon.annotation.SimonRemote
import org.junit.Before
import org.junit.Test
import java.time.Duration


class EventBusTests {

    @Test fun `when attempting to move event busses around`(){
        //setup
        ServerSide.start()
        ClientSide.start()

        //assert
//        ServerSide.fireMessages()
        ClientSide.fireMessages()

        val x = 4;

        //assrt
        println(messages.joinToString())
    }

    @Before fun resetMessages() { messages = emptyList() }

}

interface ProxyableEventSource{
    fun register(wrapper: Any)
}

@SimonRemote
class ProxiedEventSource(val eventBus: EventBus): ProxyableEventSource {

    override fun register(wrapper: Any){
        eventBus.register(wrapper)
    }

}

interface ProxyableEventSink{
    @Subscribe fun syndicateOn(obj: Any)
}


@SimonRemote
class ProxiedEventSink(val eventBus: EventBus): ProxyableEventSink {

    override fun syndicateOn(obj: Any){
        eventBus.post(obj)
    }

}

typealias Event = String

class EventSyndicator(val localEventBus: EventBus, val output: ProxyableEventSink, val input: ProxyableEventSource){

    init {
        localEventBus.register(this)
        input.register(ProxiedCallback())
    }

    interface Subscriber {
        @Subscribe fun handle(event: Event)
    }

    @SimonRemote inner class ProxiedCallback: Subscriber {

        @Subscribe override fun handle(event: Event){
            onForeignEvent(event)
        }
    }

    private var foreignEvents = emptyList<Event>()

    @Subscribe fun onLocalEvent(event: Event){
        val wasAlreadyPosted = event in foreignEvents
        foreignEvents -= event
        if( ! wasAlreadyPosted) { output.syndicateOn(event) }
    }
    fun onForeignEvent(event: Event){
        foreignEvents += event
        localEventBus.post(event)
    }
}


object ClientSide {
    private lateinit var duder: EventBusDuder

    fun start(){
        val lookup = Simon.createNameLookup("127.0.0.1")
        val eventBus = EventBus("global event bus --client")
        duder = EventBusDuder("Client", eventBus)

        val source = lookup.lookup("global event bus-producer") as ProxyableEventSource

        val sink = lookup.lookup("global event bus-consumer") as ProxyableEventSink

        EventSyndicator(eventBus, sink, source)
    }

    fun fireMessages() = duder.fireMessages()
}

object ServerSide {

    private lateinit var duder: EventBusDuder

    fun start(){

        val registry = Simon.createRegistry().apply { start() }
        val lookup = Simon.createNameLookup("127.0.0.1")

        val eventBus = EventBus("global event bus")
        val source = ProxiedEventSource(eventBus)
        val sink = ProxiedEventSink(eventBus)

        registry.bind(eventBus.identifier() + "-producer", source)
        registry.bind(eventBus.identifier() + "-consumer", sink)

        duder = EventBusDuder("Server", eventBus)
    }

    fun fireMessages() = duder.fireMessages()

}
private class EventBusDuder(val name: String, val eventBus: EventBus) {

    init {
        eventBus.register(this)
    }

    fun fireMessages(){
        messages += "$name is posting"
        eventBus.post("Blam-from-$name!")
    }

    @Subscribe
    fun logMessageOn(event: String){
        messages += "$name saw message '$event'"
    }
}


val Int.sec: Duration get() = Duration.ofSeconds(this.toLong())

private var messages: List<String> = emptyList();