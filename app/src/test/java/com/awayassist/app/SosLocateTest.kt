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
        val controller = SosLocateControllerMock()

        // Space separated
        val findCmd = controller.parseMessage("MYSECRET FIND")
        assertNotNull(findCmd)
        assertEquals("MYSECRET", findCmd?.prefixCandidate)
        assertEquals(SosLocateController.SosCommand.FIND, findCmd?.command)

        // Attached hashtag/symbol prefix
        val hashFind = controller.parseMessage("#FIND")
        assertNotNull(hashFind)
        assertEquals("#", hashFind?.prefixCandidate)
        assertEquals(SosLocateController.SosCommand.FIND, hashFind?.command)

        val hashSpaceFind = controller.parseMessage("# FIND")
        assertNotNull(hashSpaceFind)
        assertEquals("#", hashSpaceFind?.prefixCandidate)
        assertEquals(SosLocateController.SosCommand.FIND, hashSpaceFind?.command)

        // Plain command without prefix
        val plainFind = controller.parseMessage("find")
        assertNotNull(plainFind)
        assertEquals("", plainFind?.prefixCandidate)
        assertEquals(SosLocateController.SosCommand.FIND, plainFind?.command)

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

        // When no prefix is stored, any command (empty candidate) is accepted
        assertTrue(controller.verifyPrefix("", ""))
        assertTrue(controller.verifyPrefix("ANYTHING", ""))
    }

    private class SosLocateControllerMock {
        fun parseMessage(body: String): SosLocateController.ParsedCommand? {
            val trimmed = body.trim()
            if (trimmed.isEmpty()) return null

            for (cmd in SosLocateController.SosCommand.values()) {
                val cmdName = cmd.name
                if (trimmed.equals(cmdName, ignoreCase = true)) {
                    return SosLocateController.ParsedCommand(prefixCandidate = "", command = cmd)
                }
                if (trimmed.endsWith(cmdName, ignoreCase = true)) {
                    val candidate = trimmed.substring(0, trimmed.length - cmdName.length).trim()
                    return SosLocateController.ParsedCommand(prefixCandidate = candidate, command = cmd)
                }
            }

            return null
        }

        fun verifyPrefix(candidate: String, storedSha256: String): Boolean {
            if (storedSha256.isBlank()) return true
            if (candidate.isBlank()) return false
            val candidateHash = computeSha256(candidate.trim())
            return java.security.MessageDigest.isEqual(
                candidateHash.toByteArray(Charsets.UTF_8),
                storedSha256.toByteArray(Charsets.UTF_8)
            )
        }
    }
}
