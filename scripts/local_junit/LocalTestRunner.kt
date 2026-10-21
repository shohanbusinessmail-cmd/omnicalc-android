package localtest

import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.system.exitProcess

/**
 * Tiny reflective JUnit4-compatible runner for the sandbox harness.
 *
 * Usage: java -cp classes LocalTestRunnerKt fully.qualified.TestClass ...
 *
 * Mirrors JUnit4 semantics: fresh instance per test method, @Before/@After
 * around each, @Ignore skipped, non-zero exit code on any failure.
 */
fun main(args: Array<String>) {
    var passed = 0
    var failed = 0
    var skipped = 0
    val failures = mutableListOf<String>()

    for (className in args) {
        val cls = Class.forName(className)
        if (cls.getAnnotation(Ignore::class.java) != null) {
            println("IGNORED class $className")
            continue
        }
        val testMethods = cls.declaredMethods.filter { it.getAnnotation(Test::class.java) != null }
        val beforeMethods = cls.declaredMethods.filter { it.getAnnotation(Before::class.java) != null }
        val afterMethods = cls.declaredMethods.filter { it.getAnnotation(After::class.java) != null }

        for (method in testMethods.sortedBy { it.name }) {
            if (method.getAnnotation(Ignore::class.java) != null) {
                skipped++
                println("IGNORED ${cls.simpleName}.${method.name}")
                continue
            }
            val instance = cls.getDeclaredConstructor().newInstance()
            try {
                beforeMethods.forEach { it.invoke(instance) }
                method.invoke(instance)
                afterMethods.forEach { it.invoke(instance) }
                passed++
                println("PASS ${cls.simpleName}.${method.name}")
            } catch (t: Throwable) {
                afterMethods.forEach {
                    runCatching { it.invoke(instance) }
                }
                failed++
                val cause = t.cause ?: t
                val detail = "${cause.javaClass.simpleName}: ${cause.message}"
                failures += "${cls.simpleName}.${method.name} → $detail"
                println("FAIL ${cls.simpleName}.${method.name} → $detail")
                cause.stackTrace.take(5).forEach { println("      at $it") }
            }
        }
    }

    println()
    println("==== JVM test summary: $passed passed, $failed failed, $skipped skipped ====")
    if (failures.isNotEmpty()) {
        println("Failures:")
        failures.forEach { println("  - $it") }
    }
    if (failed > 0) exitProcess(1)
}
