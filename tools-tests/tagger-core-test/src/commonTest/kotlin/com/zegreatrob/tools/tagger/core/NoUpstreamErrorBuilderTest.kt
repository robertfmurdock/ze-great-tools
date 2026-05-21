package com.zegreatrob.tools.tagger.core

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test

class NoUpstreamErrorBuilderTest {

    @Test
    fun errorMessageIncludesWarningSymbolAndRiskSection() = setup(object {
    }) exercise {
        buildNoUpstreamError(CIEnvironment.UNKNOWN)
    } verify { result ->
        result.contains("⚠️  CRITICAL CONFIGURATION ERROR")
            .assertIsEqualTo(true)
        result.contains("RISK:")
            .assertIsEqualTo(true)
        result.contains("unintended production releases")
            .assertIsEqualTo(true)
    }

    @Test
    fun githubActionsEnvironmentShowsGitHubSpecificFix() = setup(object {
    }) exercise {
        buildNoUpstreamError(CIEnvironment.GITHUB_ACTIONS)
    } verify { result ->
        result.contains("actions/checkout@v4")
            .assertIsEqualTo(true)
        result.contains("ref: \${{ github.head_ref || github.ref }}")
            .assertIsEqualTo(true)
        result.contains("fetch-depth: 0")
            .assertIsEqualTo(true)
    }

    @Test
    fun gitLabEnvironmentShowsGitLabSpecificFix() = setup(object {
    }) exercise {
        buildNoUpstreamError(CIEnvironment.GITLAB_CI)
    } verify { result ->
        result.contains("GitLab CI configuration")
            .assertIsEqualTo(true)
        result.contains("GIT_STRATEGY: clone")
            .assertIsEqualTo(true)
        result.contains("GIT_DEPTH: 0")
            .assertIsEqualTo(true)
    }

    @Test
    fun azureDevOpsEnvironmentShowsAzureSpecificFix() = setup(object {
    }) exercise {
        buildNoUpstreamError(CIEnvironment.AZURE_DEVOPS)
    } verify { result ->
        result.contains("Azure DevOps pipeline")
            .assertIsEqualTo(true)
        result.contains("checkout: self")
            .assertIsEqualTo(true)
        result.contains("fetchDepth: 0")
            .assertIsEqualTo(true)
    }

    @Test
    fun errorMessageIncludesBypassWarning() = setup(object {
    }) exercise {
        buildNoUpstreamError(CIEnvironment.UNKNOWN)
    } verify { result ->
        result.contains("BYPASS (USE WITH CAUTION):")
            .assertIsEqualTo(true)
        result.contains("allowDetachedHead = true")
            .assertIsEqualTo(true)
        result.contains("⚠️  WARNING: On release branches")
            .assertIsEqualTo(true)
    }

    @Test
    fun errorMessageIncludesDocumentationLink() = setup(object {
    }) exercise {
        buildNoUpstreamError(CIEnvironment.UNKNOWN)
    } verify { result ->
        result.contains("https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/tagger-detached-head.md")
            .assertIsEqualTo(true)
    }
}
