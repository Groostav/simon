package d1.root1.simon.test

import de.root1.simon.Lookup
import de.root1.simon.Registry
import de.root1.simon.Simon
import de.root1.simon.annotation.SimonRemote
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import kotlinx.coroutines.experimental.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.CompletableFuture

interface Service {

    fun executeRun(data: Int): CompletableFuture<Double>
    fun executeExceptionRun(data: Int): CompletableFuture<Double>
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
}

class ThingyEmpoweropsTest {

    companion object {

        private lateinit var registry: Registry
        private lateinit var service: Service

        @BeforeClass @JvmStatic fun setupServer(){
            val serviceImpl = ServiceImpl()

            registry = Simon.createRegistry().apply { start() }

            registry.bind("service", serviceImpl)

            val lookup = Simon.createNameLookup("127.0.0.1")

            service = lookup.lookup("service") as Service
        }

        @AfterClass @JvmStatic fun teardownServer(){
            registry.stop()
        }
    }

    @Test fun `when using simple happy path completable future should properly send and recieve values`() = runBlocking {
        val futureResult = service.executeRun(42)

        val result: Double = futureResult.await()

        assert(result == 84.0)
    }

    @Test fun `when using exceptional future should properly send and recieve values`() = runBlocking {

        val rawExceptionResult = service.executeExceptionRun(43);

        val exception =
                try { rawExceptionResult.await().also { TODO("result is $it") } }
                catch (ex: BlamException) { ex }
    }
}
