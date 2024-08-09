package com.zegreatrob.tools.digger.core

fun allPaths(
    log: List<CommitRef>,
    firstTagCommit: CommitRef,
): MutableList<List<CommitRef>> {
    val commitMap = log.associateBy { it.id }
    val allPaths = mutableListOf<List<CommitRef>>()

    var currentPath = emptyList<CommitRef>()
    val pendingCommits = mutableListOf<Pair<CommitRef, Int?>>(firstTagCommit to null)
    val pathCache = mutableMapOf<CommitRef, List<CommitRef>>()
    println("")
    while (pendingCommits.isNotEmpty()) {
        val currentEntry = pendingCommits.last()
        val (currentCommit, child) = currentEntry
        pendingCommits.removeLast()

        val toIndex = child?.plus(1) ?: currentPath.size
        if (child != null) {
            val cachableList = currentPath.subList(toIndex, currentPath.size)
            if (cachableList.isNotEmpty()) {
                pathCache[cachableList.first()] = cachableList
            }
        }
        currentPath = currentPath.subList(0, toIndex) + currentCommit
        if (currentCommit.parents.isEmpty()) {
            allPaths += currentPath
            if (allPaths.size % 5 == 0) {
                print("\rFound ${allPaths.size} paths. pending commits ${pendingCommits.size}")
            }
        } else {
            val parentRefs = currentCommit.parents.mapNotNull { commitMap[it] }

            parentRefs.forEach { parentRef ->
                val cachedList = pathCache[parentRef]
                if (cachedList != null) {
                    val newPath = currentPath + cachedList
                    allPaths += newPath
                    print("\rFound ${allPaths.size} paths. pending commits ${pendingCommits.size}")
                } else {
                    pendingCommits += Pair(parentRef, currentPath.size - 1)
                }
            }
        }
    }
    println("")
    return allPaths
}
