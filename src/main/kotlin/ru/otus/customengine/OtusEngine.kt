package ru.otus.customengine

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.platform.commons.logging.LoggerFactory
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.FileSelector
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.lang.reflect.Method
import java.util.*


class OtusHttpDescriptor : AbstractTestDescriptor {
    var content: List<String>

    var filename: String

    constructor(engineId: UniqueId, displayName: String, fileContent: List<String>, name: String) : super(
        engineId.append(
            "otus-test",
            displayName
        ), displayName
    ) {
        content = fileContent
        filename = name
    }

    override fun getType() = TestDescriptor.Type.TEST
}

class OtusMethodDescriptor : AbstractTestDescriptor {
    var method: Method

    var obj: Any

    constructor(engineId: UniqueId, segment: String, displayName: String, m: Method, o: Any) : super(
        engineId.append(
            segment,
            displayName
        ), displayName
    ) {
        method = m
        obj = o
    }

    override fun getType() = TestDescriptor.Type.TEST
}

annotation class OtusTest

class OtusEngine : TestEngine {
    val logger = LoggerFactory.getLogger(OtusEngine::class.java)

    override fun getId() = "otus"

    override fun discover(request: EngineDiscoveryRequest?, uniqueId: UniqueId?): TestDescriptor {
        logger.info { "Custom test engine is started" }
        val descriptor = EngineDescriptor(uniqueId, "Otus Test Engine")
        request?.getSelectorsByType(ClassSelector::class.java)?.forEach { clazz ->
            val obj = ReflectionSupport.newInstance(clazz.javaClass)
            ReflectionSupport.findMethods(
                clazz.javaClass,
                { AnnotationSupport.isAnnotated(it, OtusTest::class.java) },
                HierarchyTraversalMode.TOP_DOWN
            )?.forEach { method ->
                descriptor.addChild(
                    OtusMethodDescriptor(
                        descriptor.uniqueId,
                        method.name,
                        descriptor.displayName,
                        method,
                        obj
                    )
                )
                logger.info { "Method is $method" }
            }
        }
        request?.getSelectorsByType(FileSelector::class.java)?.forEach {
            logger.info { "File: ${it.file.absolutePath}" }
            descriptor.addChild(
                OtusHttpDescriptor(
                    descriptor.uniqueId,
                    descriptor.displayName,
                    it.file.readLines(),
                    it.file.name
                )
            )
        }
        return descriptor
    }

    override fun execute(request: ExecutionRequest?) {
        val rootDescriptor = request?.rootTestDescriptor
        rootDescriptor?.children?.forEach {
            var badUrl: String? = null
            if (it is OtusMethodDescriptor) {
                logger.info { "Calling method ${it.method}" }
                request.engineExecutionListener?.executionStarted(it)
                val time = Calendar.getInstance().toInstant().toEpochMilli()
                try {
                    it.method.invoke(it.obj)
                    logger.info { "Execution time: ${(Calendar.getInstance()).toInstant().toEpochMilli() - time}ms" }
                    request.engineExecutionListener?.executionFinished(it, TestExecutionResult.successful())
                    logger.info { "Executed" }
                } catch (e: Exception) {
                    request.engineExecutionListener?.executionFinished(it, TestExecutionResult.failed(e))
                }
            }
            if (it is OtusHttpDescriptor) {
                request.engineExecutionListener?.executionStarted(it)
                var ok = true
                for (file in it.content) {
                    val line = file.split(" ")
                    val url = line[0]
                    val status = line[1].toInt()
                    val client = HttpClient(CIO)
                    runBlocking {
                        val result = client.get(url)
                        if (result.status.value != status) {
                            ok = false
                            badUrl = url
                        }
                    }
                    if (!ok) break
                }
                request.engineExecutionListener?.executionFinished(
                    it,
                    if (ok) TestExecutionResult.successful() else TestExecutionResult.failed(Exception("Status is not matched at ${badUrl}"))
                )
                logger.info { "Executing for file ${it.filename}" }
            }
        }
    }
}