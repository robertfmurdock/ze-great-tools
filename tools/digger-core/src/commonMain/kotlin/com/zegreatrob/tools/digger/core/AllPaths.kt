package com.zegreatrob.tools.digger.core

fun allPaths(
    log: List<CommitRef>,
    firstTagCommit: CommitRef,
    preferredCommitIds: Set<String> = emptySet(),
): MutableList<List<CommitRef>> {
    val commitMap = log.associateBy { it.id }
    val rootCommit = log.last { it.parents.isEmpty() }
    val allPaths = mutableListOf<List<CommitRef>>()

    var currentPath = emptyList<CommitRef>()
    val pendingCommits = mutableListOf<Pair<CommitRef, Int?>>(firstTagCommit to null)
    val pathCache = mutableMapOf<CommitRef, List<List<CommitRef>>>()
    println("")
    while (pendingCommits.isNotEmpty() && allPaths.size < 20000 && (preferredCommitIds.isEmpty() || allPaths.size == 0)) {
        val currentEntry = pendingCommits.last()
        val (currentCommit, child) = currentEntry
        pendingCommits.removeLast()

        val toIndex = child?.plus(1) ?: currentPath.size
        if (child != null) {
            if (currentPath.lastOrNull() == rootCommit) {
                pathCache.updateCache(currentPath[toIndex], allPaths)
            }
        }
        currentPath = currentPath.subList(0, toIndex) + currentCommit
        if (currentCommit.parents.isEmpty()) {
            if (currentCommit != rootCommit) {
                throw Exception("${currentCommit.id} was not ${rootCommit.id}")
            }
            allPaths += currentPath
        } else {
            val parentRefs = currentCommit.parents.mapNotNull { commitMap[it] }
                .let {
                    it.filter { ref -> preferredCommitIds.contains(ref.id) }
                        .takeIf(List<CommitRef>::isNotEmpty)
                        ?: it
                }

            val parentRefsWithCacheEntries = pathCache.filterKeys { parentRefs.contains(it) }

            allPaths += parentRefsWithCacheEntries.flatMap { (_, cache) ->
                cache.map { currentPath + it }
            }
            pendingCommits += (parentRefs - parentRefsWithCacheEntries.keys)
                .map { parentRef -> Pair(parentRef, currentPath.size - 1) }
        }
        allPaths.removeAll { !it.map(CommitRef::id).containsAll(preferredCommitIds) }
        reportState(allPaths, pendingCommits, pathCache)
    }
    println("")
    return allPaths
}

private fun MutableMap<CommitRef, List<List<CommitRef>>>.updateCache(
    cacheKey: CommitRef,
    allPaths: MutableList<List<CommitRef>>,
) {
    this[cacheKey] = allPaths.filter { it.contains(cacheKey) }
        .map { it.subList(it.indexOf(cacheKey), it.size) }
}

private fun reportState(
    allPaths: MutableList<List<CommitRef>>,
    pendingCommits: MutableList<Pair<CommitRef, Int?>>,
    pathCache: MutableMap<CommitRef, List<List<CommitRef>>>,
) {
    print("\rFound ${allPaths.size} paths. pending commits ${pendingCommits.size} cache size ${pathCache.size}")
}
