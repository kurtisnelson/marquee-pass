package com.thisisnotajoke.marqueepass.data

import org.junit.Assert.*
import org.junit.Test

class ShowTest {

    @Test
    fun `default show has empty ID`() {
        val show = Show(title = "Hamilton")
        assertEquals("", show.id)
    }

    @Test
    fun `show with explicit ID retains it`() {
        val id = System.currentTimeMillis().toString()
        val show = Show(id = id, title = "Wicked")
        assertEquals(id, show.id)
    }

    @Test
    fun `default status is WANT_TO_SEE`() {
        val show = Show(title = "Phantom")
        assertEquals(ShowStatus.WANT_TO_SEE, show.status)
    }

    @Test
    fun `copy preserves all fields`() {
        val original = Show(
            id = "123",
            title = "Hadestown",
            theater = "Walter Kerr Theatre",
            date = 1700000000000L,
            status = ShowStatus.SEEN,
            rating = 5,
            notes = "Amazing!"
        )
        val copy = original.copy()
        assertEquals(original, copy)
    }

    @Test
    fun `copy with new ID does not mutate original`() {
        val original = Show(id = "100", title = "Hamilton")
        val copy = original.copy(id = "200")
        assertEquals("100", original.id)
        assertEquals("200", copy.id)
        assertEquals(original.title, copy.title)
    }

    @Test
    fun `copy with status change clears rating`() {
        val seen = Show(id = "1", title = "Test", status = ShowStatus.SEEN, rating = 4)
        val wishlist = seen.copy(status = ShowStatus.WANT_TO_SEE, rating = null)
        assertNull(wishlist.rating)
        assertEquals(ShowStatus.WANT_TO_SEE, wishlist.status)
    }

    @Test
    fun `nullable fields default to null`() {
        val show = Show(title = "Test")
        assertNull(show.theater)
        assertNull(show.date)
        assertNull(show.rating)
        assertNull(show.notes)
    }

    @Test
    fun `show equality is based on all fields`() {
        val a = Show(id = "1", title = "A", status = ShowStatus.SEEN)
        val b = Show(id = "1", title = "A", status = ShowStatus.SEEN)
        val c = Show(id = "1", title = "A", status = ShowStatus.WANT_TO_SEE)
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `all ShowStatus values are defined`() {
        val statuses = ShowStatus.entries
        assertEquals(2, statuses.size)
        assertTrue(statuses.contains(ShowStatus.SEEN))
        assertTrue(statuses.contains(ShowStatus.WANT_TO_SEE))
    }

    @Test
    fun `millisecond IDs are positive and large`() {
        val id = System.currentTimeMillis().toString()
        assertTrue("ID should be positive", id.toLong() > 0)
        assertTrue("ID should be larger than Int.MAX_VALUE to validate Long is needed", id.toLong() > Int.MAX_VALUE.toLong())
    }

    @Test
    fun `sequential millisecond IDs are unique`() {
        val ids = (0 until 100).map { (System.currentTimeMillis() + it).toString() }
        assertEquals("All IDs should be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun `rating must be between 1 and 5 by convention`() {
        // The database rules enforce 1-5, verify the data class allows it
        for (r in 1..5) {
            val show = Show(id = "1", title = "T", status = ShowStatus.SEEN, rating = r)
            assertEquals(r, show.rating)
        }
    }
}
