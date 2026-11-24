package com.notes.net

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.net.URI

class ProtocolContractTest {

    companion object {
        private lateinit var protocolDir: File
        private lateinit var validFixturesDir: File
        private lateinit var invalidFixturesDir: File
        private val mapper = ObjectMapper()

        @BeforeClass
        @JvmStatic
        fun setUp() {
            // Locate /protocol directory relative to working directory or project root
            var dir = File("../protocol").canonicalFile
            if (!dir.exists()) {
                dir = File("../../protocol").canonicalFile
            }
            if (!dir.exists()) {
                dir = File("protocol").canonicalFile
            }
            protocolDir = dir
            validFixturesDir = File(protocolDir, "fixtures/valid")
            invalidFixturesDir = File(protocolDir, "fixtures/invalid")

            assertTrue("Protocol dir must exist: ${protocolDir.absolutePath}", protocolDir.exists())
            assertTrue("Valid fixtures must exist: ${validFixturesDir.absolutePath}", validFixturesDir.exists())
            assertTrue("Invalid fixtures must exist: ${invalidFixturesDir.absolutePath}", invalidFixturesDir.exists())
        }
    }

    private fun loadMessageSchema(): com.networknt.schema.JsonSchema {
        val config = SchemaValidatorsConfig()
        val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
        val schemaFile = File(protocolDir, "message.schema.json")
        return factory.getSchema(com.networknt.schema.SchemaLocation.of(schemaFile.toURI().toString()), config)
    }

    @Test
    fun testAllValidFixturesPassJsonSchema() {
        val schema = loadMessageSchema()
        val validFiles = validFixturesDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        assertTrue("Expected valid fixtures", validFiles.isNotEmpty())

        for (file in validFiles) {
            val jsonNode: JsonNode = mapper.readTree(file)
            val validationMessages = schema.validate(jsonNode)
            assertTrue(
                "Valid fixture ${file.name} failed schema validation: $validationMessages",
                validationMessages.isEmpty()
            )
        }
    }

    @Test
    fun testAllValidFixturesDecodeWithKotlinxSerialization() {
        val validFiles = validFixturesDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        assertTrue("Expected valid fixtures", validFiles.isNotEmpty())

        for (file in validFiles) {
            val text = file.readText()
            val envelope = TolerantProtocolJson.decodeFromString<MessageEnvelope>(text)
            assertEquals("Version must be 1 in ${file.name}", 1, envelope.v)
            assertTrue("Type must not be blank in ${file.name}", envelope.type.isNotBlank())
            assertTrue("ID must not be blank in ${file.name}", envelope.id.isNotBlank())
            assertTrue("Session ID must not be blank in ${file.name}", envelope.session_id.isNotBlank())

            when (envelope.type) {
                "region.submit" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<RegionSubmitPayload>(envelope.payload)
                    assertTrue(p.lines.isNotEmpty())
                    assertNotNull(p.bounding_box)
                }
                "lasso.submit" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<LassoSubmitPayload>(envelope.payload)
                    assertTrue(p.line_ids.isNotEmpty())
                }
                "cancel" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<CancelPayload>(envelope.payload)
                    assertTrue(p.target_id.isNotBlank())
                }
                "ping" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<PingPayload>(envelope.payload)
                    assertNotNull(p)
                }
                "status" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<StatusPayload>(envelope.payload)
                    assertTrue(p.code.isNotBlank())
                }
                "action.underline" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<ActionUnderlinePayload>(envelope.payload)
                    assertTrue(p.line_id.isNotBlank())
                    assertNotNull(p.fallback_bbox)
                    assertEquals(envelope.page_rev, p.page_rev)
                }
                "action.sticky_note" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<ActionStickyNotePayload>(envelope.payload)
                    assertTrue(p.line_id.isNotBlank())
                    assertTrue(p.content.isNotBlank())
                    assertEquals(envelope.page_rev, p.page_rev)
                }
                "action.dismiss" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<ActionDismissPayload>(envelope.payload)
                    assertTrue(p.action_id.isNotBlank())
                }
                "error" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<ErrorPayload>(envelope.payload)
                    assertTrue(p.code.isNotBlank())
                }
                "pong" -> {
                    val p = TolerantProtocolJson.decodeFromJsonElement<PongPayload>(envelope.payload)
                    assertNotNull(p)
                }
            }
        }
    }

    @Test
    fun testAllInvalidFixturesRejectedByJsonSchema() {
        val schema = loadMessageSchema()
        val invalidFiles = invalidFixturesDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        assertTrue("Expected invalid fixtures", invalidFiles.isNotEmpty())

        for (file in invalidFiles) {
            val jsonNode: JsonNode = mapper.readTree(file)
            val validationMessages = schema.validate(jsonNode)
            assertFalse(
                "Invalid fixture ${file.name} was unexpectedly accepted by schema!",
                validationMessages.isEmpty()
            )
        }
    }
}
