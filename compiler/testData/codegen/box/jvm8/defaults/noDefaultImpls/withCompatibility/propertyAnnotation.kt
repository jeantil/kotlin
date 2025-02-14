// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// CHECK_BYTECODE_LISTING

annotation class MyAnn

@JvmDefaultWithCompatibility
interface Test {
    @MyAnn
    val prop: String
        get() = "OK"
}

class TestClass : Test

fun box(): String {
    val testClass = TestClass()
    return testClass.prop
}
