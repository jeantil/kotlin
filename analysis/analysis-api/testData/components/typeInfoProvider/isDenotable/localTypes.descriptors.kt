fun test() {
    class A
    @Denotable("A") A()
    @Denotable("kotlin.collections.List<A>") listOf(A())
    @Undenotable("<no name provided>") object {}
    @Undenotable("kotlin.collections.List<<no name provided>>") listOf(object {})
}
