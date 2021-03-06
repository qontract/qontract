package application

import picocli.CommandLine.*
import `in`.specmatic.core.Contract
import `in`.specmatic.core.logException
import `in`.specmatic.core.information
import `in`.specmatic.stub.HttpStub
import java.io.File
import java.util.concurrent.Callable

@Command(name = "samples",
        mixinStandardHelpOptions = true,
        description = ["Generate samples of the API requests and responses for all scenarios"])
class SamplesCommand : Callable<Unit> {
    @Parameters(index = "0", description = ["Contract file path"])
    lateinit var contractFile: File

    override fun call() {
        logException {
            if(!contractFile.exists())
                throw Exception("Could not find file ${contractFile.path}")

            val gherkin = contractFile.readText().trim()

            try {
                HttpStub(gherkin, emptyList(), "127.0.0.1", 56789).use { fake ->
                    Contract(gherkin).samples(fake)
                }
            } catch(e: StackOverflowError) {
                information.forTheUser("Got a stack overflow error. You probably have a recursive data structure definition in the contract.")
            }
        }
    }
}

