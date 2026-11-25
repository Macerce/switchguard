package com.macerce.switchguard.core

/**
 * Düzenleyicide iç içe adımlara yol ile erişim. Yol: üst listedeki sıra, sonra her blok için dal ve sıra;
 * metin hali "2", "2.t.0", "2.e.1.t.3" ('t' = ise/tekrar gövdesi, 'e' = değilse). Saf.
 */
object StepTree {
    /** Derinlik sınırı (blok içinde blok içinde blok). */
    const val MAX_DEPTH = 3

    data class Path(val top: Int, val rest: List<Pair<Char, Int>> = emptyList()) {
        val depth get() = rest.size
        override fun toString(): String = (listOf(top.toString()) + rest.flatMap { listOf(it.first.toString(), it.second.toString()) }).joinToString(".")
        /** Bir üst adımın yolu (üst düzeyde null). */
        fun parent(): Path? = if (rest.isEmpty()) null else Path(top, rest.dropLast(1))
        fun child(branch: Char, index: Int) = Path(top, rest + (branch to index))

        companion object {
            fun parse(s: String): Path? = runCatching {
                val p = s.split('.')
                val rest = p.drop(1).chunked(2).map { (b, i) -> b.single().also { require(it == 't' || it == 'e') } to i.toInt() }
                Path(p[0].toInt(), rest)
            }.getOrNull()
        }
    }

    fun get(list: List<AutoAction>, path: Path): AutoAction? {
        var cur = list.getOrNull(path.top) ?: return null
        for ((b, i) in path.rest) cur = branch(cur, b).getOrNull(i) ?: return null
        return cur
    }

    fun branch(a: AutoAction, b: Char): List<AutoAction> = if (b == 'e') a.otherwise else a.then

    private fun withBranch(a: AutoAction, b: Char, list: List<AutoAction>): AutoAction = if (b == 'e') a.copy(otherwise = list) else a.copy(then = list)

    /** Yoldaki adımı [f] ile değiştirir; yol yoksa liste aynen döner. */
    fun update(list: List<AutoAction>, path: Path, f: (AutoAction) -> AutoAction): List<AutoAction> {
        fun go(a: AutoAction, rest: List<Pair<Char, Int>>): AutoAction {
            if (rest.isEmpty()) return f(a)
            val (b, i) = rest.first()
            val children = branch(a, b)
            if (i !in children.indices) return a
            return withBranch(a, b, children.toMutableList().also { it[i] = go(it[i], rest.drop(1)) })
        }
        if (path.top !in list.indices) return list
        return list.toMutableList().also { it[path.top] = go(it[path.top], path.rest) }
    }

    /** Yoldaki adımı siler. */
    fun remove(list: List<AutoAction>, path: Path): List<AutoAction> {
        val parent = path.parent() ?: return list.filterIndexed { i, _ -> i != path.top }
        val (b, i) = path.rest.last()
        return update(list, parent) { p -> withBranch(p, b, branch(p, b).filterIndexed { j, _ -> j != i }) }
    }

    /** [parent] bloğunun [b] dalının sonuna ekler; eklenenin yolu döner. */
    fun append(list: List<AutoAction>, parent: Path, b: Char, step: AutoAction): Pair<List<AutoAction>, Path> {
        val size = get(list, parent)?.let { branch(it, b).size } ?: 0
        return update(list, parent) { p -> withBranch(p, b, branch(p, b) + step) } to parent.child(b, size)
    }

    /** Aynı listede bir yukarı (-1) / aşağı (+1) taşır; yeni yol döner. */
    fun move(list: List<AutoAction>, path: Path, delta: Int): Pair<List<AutoAction>, Path> {
        fun swap(l: List<AutoAction>, i: Int): List<AutoAction>? {
            val j = i + delta
            if (i !in l.indices || j !in l.indices) return null
            return l.toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }
        }
        val parent = path.parent()
        if (parent == null) {
            val l = swap(list, path.top) ?: return list to path
            return l to path.copy(top = path.top + delta)
        }
        val (b, i) = path.rest.last()
        var moved = false
        val out = update(list, parent) { p -> swap(branch(p, b), i)?.let { moved = true; withBranch(p, b, it) } ?: p }
        return if (!moved) list to path else out to parent.child(b, i + delta)
    }
}
