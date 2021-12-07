fun test() {
    class A
    @Denotable("A") A()
    @Denotable("kotlin.collections.List<A>") listOf(A())
    @Undenotable("<anonymous>") object {}
    @Undenotable("kotlin.collections.List<<anonymous>>") listOf(object {})
}
