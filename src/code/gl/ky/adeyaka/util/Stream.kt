package gl.ky.adeyaka.util

class Stream<E>(elements: Iterable<E>) {
    private val source: List<E> = elements.toList()
    var index = 0

    fun hasNext() = index < source.size
    fun next() = source[index++]
    fun peek(offset: Int = 0) = source[index + offset]
    fun skip(count: Int = 1) { index += count }
}

typealias CharStream = Stream<Char>
