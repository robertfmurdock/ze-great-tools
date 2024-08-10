package com.zegreatrob.tools.digger.core

fun allPaths(
    log: List<CommitRef>,
    firstTagCommit: CommitRef,
): MutableList<List<CommitRef>> {
    val commitMap = log.associateBy { it.id }
    val rootCommit = log.last { it.parents.isEmpty() }
    val allPaths = mutableListOf<List<CommitRef>>()

    var currentPath = emptyList<CommitRef>()
    val pendingCommits = mutableListOf<Pair<CommitRef, Int?>>(firstTagCommit to null)
    val pathCache = mutableMapOf<CommitRef, List<List<CommitRef>>>()
    println("")
    while (pendingCommits.isNotEmpty() && allPaths.size < 10000) {
        val currentEntry = pendingCommits.last()
        val (currentCommit, child) = currentEntry
        pendingCommits.removeLast()

        val toIndex = child?.plus(1) ?: currentPath.size
        if (child != null) {
            val cachableList = currentPath.subList(toIndex, currentPath.size)
            if (cachableList.lastOrNull() == rootCommit) {
                val cacheKey = cachableList.first()
                pathCache[cacheKey] =
                    pathCache[cacheKey]?.plus(element = cachableList) ?: allPaths.filter { it.contains(cacheKey) }
                        .map { it.subList(it.indexOf(cacheKey), it.size) }
            } else if (cachableList.isNotEmpty()) {
                println("Cache Error: went backward without finishing path")
                println("last path ${allPaths.last().joinToString(", ") { it.id }}")
                throw Exception("Halt")
            }
        }
        currentPath = currentPath.subList(0, toIndex) + currentCommit
        if (currentCommit.parents.isEmpty()) {
            if (currentCommit != rootCommit) {
                throw Exception("${currentCommit.id} was not ${rootCommit.id}")
            }
            allPaths += currentPath
            reportState(allPaths, pendingCommits, pathCache)
        } else {
            val parentRefs = currentCommit.parents.mapNotNull { commitMap[it] }

            val parentRefsWithCacheEntries = pathCache.filterKeys { parentRefs.contains(it) }

            allPaths += parentRefsWithCacheEntries.flatMap { (_, cache) ->
                cache.map { currentPath + it }
            }

            val parentsRemainingToProcess = (parentRefs - parentRefsWithCacheEntries.keys)
                .map { parentRef -> Pair(parentRef, currentPath.size - 1) }
            if (parentsRemainingToProcess.isEmpty()) {
                parentRefsWithCacheEntries[parentRefs.last()]?.let {
                    currentPath = currentPath + it.last()
                }
            }

            pendingCommits += parentsRemainingToProcess
        }
    }
    println("")
    return allPaths
}

private fun reportState(
    allPaths: MutableList<List<CommitRef>>,
    pendingCommits: MutableList<Pair<CommitRef, Int?>>,
    pathCache: MutableMap<CommitRef, List<List<CommitRef>>>,
) {
    print("\rFound ${allPaths.size} paths. pending commits ${pendingCommits.size} cache size ${pathCache.size}")
}
