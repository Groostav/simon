package d1.root1.simon.test

import de.root1.simon.Simon
import de.root1.simon.annotation.SimonRemote
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import java.util.concurrent.CompletableFuture

interface Service {
    fun executeRun(data: Int): CompletableFuture<Double>
}
@SimonRemote(Service::class)
class ServiceImpl: Service {
    override fun executeRun(data: Int) = future<Double> {
        42.0 + data
    }
}

class ThingyEmpoweropsTest {

    @Test fun `running server and client should work with completable futures nicely!`(){
        runServer()
        runClient()
    }

    private fun runServer(){
        val serviceImpl = ServiceImpl()

        val registry = Simon.createRegistry().apply { start() }

        registry.bind("service", serviceImpl)
    }

    private fun runClient() = runBlocking {
        val lookup = Simon.createNameLookup("127.0.0.1")

        val server = lookup.lookup("service") as Service

        val result = server.executeRun(42).await()

        println("got $result!")
    }
}
