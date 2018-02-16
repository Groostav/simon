package d1.root1.simon.test

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import de.root1.simon.Simon
import de.root1.simon.annotation.SimonRemote
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.time.delay
import java.time.Duration


object EventBusTests {
//object EventBusTests {

    @JvmStatic fun main(args: Array<String>){
//    @Test fun `when attempting to move event busses around`(){
        ServerSide.run()
        ClientSide.run()
    }

}

interface ProxyableEventSource{
    fun register(wrapper: Any)
}

@SimonRemote
class ProxyableEventSourceImpl(val eventBus: EventBus): ProxyableEventSource {

    override fun register(wrapper: Any){
        eventBus.register(wrapper)
    }

}

interface ProxyableEventSink{
    @Subscribe
    fun syndicateOn(obj: Any)
}


//TODO: ok so I think the problem here is that we depend on event busses. We can make lambdas proxied in simon by using the
// @SimonRemote fun(input: Stuff): Stuff { ... } syntax
// so we should do that.
@SimonRemote
class ProxyableEventSinkImpl(val eventBus: EventBus): ProxyableEventSink {

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
        foreignEvents -= event
        output.syndicateOn(event)
    }
    fun onForeignEvent(event: Event){
        foreignEvents += event
        localEventBus.post(event)
    }
}


object ClientSide {

    fun run(){
        val lookup = Simon.createNameLookup("127.0.0.1")
        val eventBus = EventBus("global event bus --client")
        EventBusDuder("Client", eventBus)

        val source = lookup.lookup("global event bus-producer") as ProxyableEventSource

        val sink = lookup.lookup("global event bus-consumer") as ProxyableEventSink

        EventSyndicator(eventBus, sink, source)
    }
}

object ServerSide {

    fun run(){

        val registry = Simon.createRegistry().apply { start() }
        val lookup = Simon.createNameLookup("127.0.0.1")

        val eventBus = EventBus("global event bus")
        val source = ProxyableEventSourceImpl(eventBus)
        val sink = ProxyableEventSinkImpl(eventBus)

        registry.bind(eventBus.identifier() + "-producer", source)
        registry.bind(eventBus.identifier() + "-consumer", sink)

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
                println("$name is posting")
                eventBus.post("Blam-from-$name!")

                delay(2.sec)
            }
        }
    }

    @Subscribe
    fun logMessageOn(event: String){
        println("$name saw message '$event'")
    }
}


val Int.sec: Duration get() = Duration.ofSeconds(this.toLong())