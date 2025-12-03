package jsonschema_to_mermaid.diagram

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnglishSingularizerTest {
    @Test
    fun testRegularPlurals() {
        assertEquals("Cat", EnglishSingularizer.toSingular("cats"))
        assertEquals("Box", EnglishSingularizer.toSingular("boxes"))
        assertEquals("Status", EnglishSingularizer.toSingular("statuses"))
        assertEquals("Company", EnglishSingularizer.toSingular("companies"))
    }

    @Test
    fun testIrregularPlurals() {
        assertEquals("Child", EnglishSingularizer.toSingular("children"))
        assertEquals("Mouse", EnglishSingularizer.toSingular("mice"))
        assertEquals("Datum", EnglishSingularizer.toSingular("data"))
        assertEquals("Person", EnglishSingularizer.toSingular("people"))
    }

    @Test
    fun testEdgeCases() {
        assertEquals("Bus", EnglishSingularizer.toSingular("buses"))
        assertEquals("Dress", EnglishSingularizer.toSingular("dresses"))
        assertEquals("Foot", EnglishSingularizer.toSingular("feet"))
        assertEquals("Man", EnglishSingularizer.toSingular("men"))
    }

    @Test
    fun testAlreadySingular() {
        assertEquals("Book", EnglishSingularizer.toSingular("book"))
        assertEquals("Child", EnglishSingularizer.toSingular("child"))
    }
}

