package org.junit

/**
 * Local-only, minimal, JUnit4-compatible test annotation used by the
 * sandbox verification harness (scripts/run_jvm_tests.sh).
 *
 * The Gradle build compiles the identical test sources against the real
 * junit:junit artifact from Maven Central; this shim exists ONLY because the
 * sandbox cannot reach Maven Central. See docs/TESTING.md.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Test

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Before

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class After

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Ignore
