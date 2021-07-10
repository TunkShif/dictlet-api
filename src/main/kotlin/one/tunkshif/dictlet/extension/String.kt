package one.tunkshif.dictlet.extension

fun String.toKebabStyle() =
    this.trim().splitToSequence(" ").map { it.toLowerCase() }.joinToString("-")
