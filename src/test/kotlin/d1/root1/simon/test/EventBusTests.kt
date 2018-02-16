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
        ServerSide.fireMessages()
        ClientSide.fireMessages()

        val x = 4;

        //assrt
        println(messages.joinToString("\n"))
        assert(messages == listOf(
                "Server is posting",
                "Client saw message 'Blam-from-Server!'",
                "Server saw message 'Blam-from-Server!'",
                "Client is posting",
                "Client saw message 'Blam-from-Client!'",
                "Server saw message 'Blam-from-Client!'"
        ))
    }

    @Before fun resetMessages() { messages = emptyList() }

}

typealias Event = String //TODO make interface
interface RemoteEventHandler { fun handle(event: Event) }

interface ProxyableEventSource {
    fun register(handler: RemoteEventHandler)
    fun post(event: Event)
}

@SimonRemote class ProxiedEventSource(): ProxyableEventSource {

    var handlers: List<RemoteEventHandler> = emptyList()

    override fun register(handler: RemoteEventHandler) {
        handlers += handler
    }

    override fun post(event: Event) {
        handlers.forEach { it.handle(event) }
    }

}

class EventSyndicator(val name: String, val inside: EventBus, val outside: ProxyableEventSource){

    init {
        inside.register(this)
        outside.register(ProxiedCallback())
    }

    @SimonRemote inner class ProxiedCallback: RemoteEventHandler {

        @Subscribe override fun handle(event: Event){
            onForeignEvent(event)
        }
    }

    private var foreignEvents = emptySet<Event>()

    @Subscribe fun onLocalEvent(event: Event){
        val wasAlreadyPosted = event in foreignEvents

        if( ! wasAlreadyPosted) {
            foreignEvents += event
            outside.post(event)
        }
        else {
            foreignEvents -= event
        }
    }
    fun onForeignEvent(event: Event){
        val wasAlreadyPosted = event in foreignEvents

        if( ! wasAlreadyPosted) {
            foreignEvents += event
            inside.post(event)
        }
        else {
            foreignEvents -= event
        }
    }
}


object ClientSide {
    private lateinit var duder: EventBusDuder

    fun start(){
        val lookup = Simon.createNameLookup("127.0.0.1")
        val eventBus = EventBus("global event bus --client")
        duder = EventBusDuder("Client", eventBus)

        val source = lookup.lookup("global event bus-producer") as ProxyableEventSource

        EventSyndicator("Client", eventBus, source)
    }

    fun fireMessages() = duder.fireMessages()
}

object ServerSide {

    private lateinit var duder: EventBusDuder

    fun start(){

        val registry = Simon.createRegistry().apply { start() }
        val lookup = Simon.createNameLookup("127.0.0.1")

        val eventBus = EventBus("global event bus")
        val source = ProxiedEventSource()

        EventSyndicator("Server", eventBus, source)

        registry.bind(eventBus.identifier() + "-producer", source)


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