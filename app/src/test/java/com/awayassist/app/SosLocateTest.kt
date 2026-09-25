package com.awayassist.app

import com.awayassist.app.data.computeSha256
import com.awayassist.app.util.SosLocateController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SosLocateTest {

    @Test
    fun testSha256Computation() {
        val input = "AWAYASSIST"
        val hash = computeSha256(input)
        assertNotNull(hash)
        assertEquals(64, hash.length)

        // Known SHA-256 for "AWAYASSIST"
        val expected = "c7d7775ebf5334a1fbeab33ecba27e5dbb8dddb1bf3f18dabc834b8fbb162c62"
        assertEquals(expected, hash.lowercase())

        // Empty input returns empty string
        assertEquals("", computeSha256(""))
    }

    @Test
    fun testCommandParsing() {
        // We test parsing through SosLocateController dummy parser or simulated logic
        val controller = SosLocateControllerMock()

        // Valid commands
        val findCmd = controller.parseMessage("MYSECRET FIND")
        assertNotNull(findCmd)
        assertEquals("MYSECRET", findCmd?.prefixCandidate)
        assertEquals(SosLocateController.SosCommand.FIND, findCmd?.command)

        val trackCmd = controller.parseMessage("SECRET PASSKEY TRACK")
        assertNotNull(trackCmd)
        assertEquals("SECRET PASSKEY", trackCmd?.prefixCandidate)
        assertEquals(SosLocateController.SosCommand.TRACK, trackCmd?.command)

        val traceCmd = controller.parseMessage("PASSKEY123 trace")
        assertNotNull(traceCmd)
        assertEquals("PASSKEY123", traceCmd?.prefixCandidate)
        assertEquals(SosLocateController.SosCommand.TRACE, traceCmd?.command)

        val stopCmd = controller.parseMessage("PASSKEY123   STOP  ")
        assertNotNull(stopCmd)
        assertEquals("PASSKEY123", stopCmd?.prefixCandidate)
        assertEquals(SosLocateController.SosCommand.STOP, stopCmd?.command)

        // Invalid commands or formats
        assertNull(controller.parseMessage("JUSTANORMALTEXT"))
        assertNull(controller.parseMessage("MYSECRET INVALIDCOMMAND"))
        assertNull(controller.parseMessage(""))
        assertNull(controller.parseMessage("   "))
    }

    @Test
    fun testPrefixVerification() {
        val controller = SosLocateControllerMock()
        val storedPrefix = "AWAYASSIST_ALPHA"
        val storedHash = computeSha256(storedPrefix)

        // Matching prefix
        assertTrue(controller.verifyPrefix("AWAYASSIST_ALPHA", storedHash))
        assertTrue(controller.verifyPrefix("  AWAYASSIST_ALPHA  ", storedHash))

        // Mismatched prefix
        assertFalse(controller.verifyPrefix("WRONG_PREFIX", storedHash))
        assertFalse(controller.verifyPrefix("awayassist_alpha", storedHash)) // Case-sensitive passkey
        assertFalse(controller.verifyPrefix("", storedHash))
        assertFalse(controller.verifyPrefix("AWAYASSIST_ALPHA", ""))
    }

    private class SosLocateControllerMock {
        fun parseMessage(body: String): SosLocateController.ParsedCommand? {
            val trimmed = body.trim()
            if (trimmed.isEmpty()) return null

            val lastSpaceIndex = trimmed.lastIndexOf(' ')
            if (lastSpaceIndex <= 0) return null

            val prefixCandidate = trimmed.substring(0, lastSpaceIndex).trim()
            val commandStr = trimmed.substring(lastSpaceIndex + 1).trim().uppercase()

            val command = try {
                SosLocateController.SosCommand.valueOf(commandStr)
            } catch (e: IllegalArgumentException) {
                return null
            }

            return SosLocateController.ParsedCommand(prefixCandidate = prefixCandidate, command = command)
        }

        fun verifyPrefix(candidate: String, storedSha256: String): Boolean {
            if (candidate.isBlank() || storedSha256.isBlank()) return false
            val candidateHash = computeSha256(candidate.trim())
            return java.security.MessageDigest.isEqual(
                candidateHash.toByteArray(Charsets.UTF_8),
                storedSha256.toByteArray(Charsets.UTF_8)
            )
        }
    }
}
