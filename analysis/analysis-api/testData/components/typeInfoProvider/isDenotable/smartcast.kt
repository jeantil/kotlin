fun test(a: Any?) {
    if (a is String) {
        (@Denotable("kotlin.String") a).length
        if (a is Int) {
            (@Undenotable("(kotlin.String&kotlin.Int)") a).inc()
        }
        if (a is String) {
            (@Denotable("kotlin.String") a).length
        }
    }
    if (a != null) {
        (@Denotable("kotlin.Any") a).hashCode()
    }
    if (a == null) {
        (@Denotable("kotlin.Nothing?") a).isNothing()
    }
    if (a is String || a is Int) {
        (@Undenotable("(kotlin.Comparable<(kotlin.String&kotlin.Int)>&java.io.Serializable)") a).length
        (@Undenotable("(kotlin.Comparable<(kotlin.String&kotlin.Int)>&java.io.Serializable)") a).inc()
    }
    @Undenotable("(kotlin.Comparable<*>&java.io.Serializable)") if (true) {
        ""
    } else {
        1
    }
}

fun Nothing?.isNothing() {}
