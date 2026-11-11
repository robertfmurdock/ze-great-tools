package com.zegreatrob.tools.dashboard

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal data class JUnitCounts(
    val reports: Long = 0,
    val passed: Long = 0,
    val failed: Long = 0,
    val skipped: Long = 0,
) {
    operator fun plus(other: JUnitCounts) = JUnitCounts(
        reports + other.reports,
        passed + other.passed,
        failed + other.failed,
        skipped + other.skipped,
    )

    fun addTotals(builder: JsonObjectBuilder) = with(builder) {
        put("total", passed + failed + skipped)
        put("passed", passed)
        put("failed", failed)
        put("skipped", skipped)
    }
}

internal fun readCounts(file: File): JUnitCounts {
    val data = Json.parseToJsonElement(file.readText()).jsonObject
    return JUnitCounts(data.number("reports"), data.number("passed"), data.number("failed"), data.number("skipped"))
}

private fun JsonObject.number(key: String) = getValue(key).jsonPrimitive.long

internal fun countJUnitReport(file: File): JUnitCounts {
    val factory = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }
    val document = try {
        factory.newDocumentBuilder().parse(file)
    } catch (exception: SAXException) {
        throw IllegalArgumentException("Invalid JUnit XML in '$file'.", exception)
    }
    require(document.documentElement.tagName in setOf("testsuite", "testsuites")) { "Expected JUnit XML in '$file'." }
    val cases = document.getElementsByTagName("testcase")
    return (0 until cases.length).fold(JUnitCounts(reports = 1)) { counts, index ->
        counts + (cases.item(index) as Element).outcome()
    }
}

private fun Element.outcome() = when {
    hasElement("failure") || hasElement("error") -> JUnitCounts(failed = 1)
    hasElement("skipped") || hasSkipProperty() -> JUnitCounts(skipped = 1)
    else -> JUnitCounts(passed = 1)
}

private fun Element.hasElement(tag: String) = getElementsByTagName(tag).length > 0

private fun Element.hasSkipProperty(): Boolean {
    val properties = getElementsByTagName("property")
    return (0 until properties.length).any { (properties.item(it) as Element).getAttribute("name") == "skip" }
}
