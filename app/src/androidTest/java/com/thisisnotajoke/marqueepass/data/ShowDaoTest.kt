package com.thisisnotajoke.marqueepass.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShowDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ShowDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.showDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // --- Insert Tests ---

    @Test
    fun insertShow_returnsId() = runTest {
        val show = Show(id = 1000L, title = "Hamilton", status = ShowStatus.SEEN)
        val returnedId = dao.insertShow(show)
        assertEquals(1000L, returnedId)
    }

    @Test
    fun insertShow_canBeRetrievedById() = runTest {
        val show = Show(id = 2000L, title = "Wicked", theater = "Gershwin Theatre", status = ShowStatus.WANT_TO_SEE)
        dao.insertShow(show)
        val retrieved = dao.getShowById(2000L)
        assertNotNull(retrieved)
        assertEquals("Wicked", retrieved!!.title)
        assertEquals("Gershwin Theatre", retrieved.theater)
        assertEquals(ShowStatus.WANT_TO_SEE, retrieved.status)
    }

    @Test
    fun insertShow_withReplaceStrategy_updatesExisting() = runTest {
        val original = Show(id = 3000L, title = "Original", status = ShowStatus.SEEN)
        dao.insertShow(original)

        val updated = original.copy(title = "Updated")
        dao.insertShow(updated)

        val retrieved = dao.getShowById(3000L)
        assertEquals("Updated", retrieved!!.title)

        val all = dao.getAllShows().first()
        assertEquals(1, all.size)
    }

    @Test
    fun insertShow_multipleShowsWithUniqueIds() = runTest {
        val shows = (1..5).map { i ->
            Show(id = System.currentTimeMillis() + i, title = "Show $i", status = ShowStatus.SEEN)
        }
        shows.forEach { dao.insertShow(it) }

        val all = dao.getAllShows().first()
        assertEquals(5, all.size)
    }

    // --- Query Tests ---

    @Test
    fun getAllShows_returnsOrderedByDateDesc() = runTest {
        dao.insertShow(Show(id = 1L, title = "Oldest", date = 1000L, status = ShowStatus.SEEN))
        dao.insertShow(Show(id = 2L, title = "Newest", date = 3000L, status = ShowStatus.SEEN))
        dao.insertShow(Show(id = 3L, title = "Middle", date = 2000L, status = ShowStatus.SEEN))

        val shows = dao.getAllShows().first()
        assertEquals("Newest", shows[0].title)
        assertEquals("Middle", shows[1].title)
        assertEquals("Oldest", shows[2].title)
    }

    @Test
    fun getShowsByStatus_filtersSeen() = runTest {
        dao.insertShow(Show(id = 1L, title = "Seen1", status = ShowStatus.SEEN))
        dao.insertShow(Show(id = 2L, title = "Want1", status = ShowStatus.WANT_TO_SEE))
        dao.insertShow(Show(id = 3L, title = "Seen2", status = ShowStatus.SEEN))

        val seenShows = dao.getShowsByStatus(ShowStatus.SEEN).first()
        assertEquals(2, seenShows.size)
        assertTrue(seenShows.all { it.status == ShowStatus.SEEN })
    }

    @Test
    fun getShowsByStatus_filtersWantToSee() = runTest {
        dao.insertShow(Show(id = 1L, title = "Seen1", status = ShowStatus.SEEN))
        dao.insertShow(Show(id = 2L, title = "Want1", status = ShowStatus.WANT_TO_SEE))
        dao.insertShow(Show(id = 3L, title = "Want2", status = ShowStatus.WANT_TO_SEE))

        val wantShows = dao.getShowsByStatus(ShowStatus.WANT_TO_SEE).first()
        assertEquals(2, wantShows.size)
        assertTrue(wantShows.all { it.status == ShowStatus.WANT_TO_SEE })
    }

    @Test
    fun getShowsByStatus_returnsEmptyWhenNoneMatch() = runTest {
        dao.insertShow(Show(id = 1L, title = "Seen1", status = ShowStatus.SEEN))

        val wantToSeeShows = dao.getShowsByStatus(ShowStatus.WANT_TO_SEE).first()
        assertTrue(wantToSeeShows.isEmpty())
    }

    @Test
    fun getShowById_returnsNullForNonexistent() = runTest {
        val result = dao.getShowById(999L)
        assertNull(result)
    }

    // --- Update Tests ---

    @Test
    fun updateShow_modifiesExistingShow() = runTest {
        val show = Show(id = 5000L, title = "Before", status = ShowStatus.WANT_TO_SEE)
        dao.insertShow(show)

        dao.updateShow(show.copy(title = "After", status = ShowStatus.SEEN, rating = 4))

        val retrieved = dao.getShowById(5000L)
        assertEquals("After", retrieved!!.title)
        assertEquals(ShowStatus.SEEN, retrieved.status)
        assertEquals(4, retrieved.rating)
    }

    // --- Delete Tests ---

    @Test
    fun deleteShow_removesShow() = runTest {
        val show = Show(id = 6000L, title = "ToDelete", status = ShowStatus.SEEN)
        dao.insertShow(show)
        assertEquals(1, dao.getAllShows().first().size)

        dao.deleteShow(show)
        assertEquals(0, dao.getAllShows().first().size)
        assertNull(dao.getShowById(6000L))
    }

    @Test
    fun deleteAllShows_clearsDatabase() = runTest {
        dao.insertShow(Show(id = 1L, title = "A", status = ShowStatus.SEEN))
        dao.insertShow(Show(id = 2L, title = "B", status = ShowStatus.WANT_TO_SEE))
        assertEquals(2, dao.getAllShows().first().size)

        dao.deleteAllShows()
        assertEquals(0, dao.getAllShows().first().size)
    }

    // --- ID Uniqueness Tests (P0 regression) ---

    @Test
    fun timestampBasedIds_doNotCollide() = runTest {
        val baseTime = System.currentTimeMillis()
        val shows = (0 until 10).map { i ->
            Show(id = baseTime + i, title = "Show $i", status = ShowStatus.SEEN)
        }
        shows.forEach { dao.insertShow(it) }

        val all = dao.getAllShows().first()
        assertEquals(10, all.size)
        assertEquals(10, all.map { it.id }.toSet().size)
    }

    @Test
    fun longIds_exceedIntRange() = runTest {
        val largeId = Int.MAX_VALUE.toLong() + 1
        val show = Show(id = largeId, title = "Large ID Show", status = ShowStatus.SEEN)
        dao.insertShow(show)

        val retrieved = dao.getShowById(largeId)
        assertNotNull(retrieved)
        assertEquals(largeId, retrieved!!.id)
    }

    // --- Merge Safety Tests (P0 regression) ---

    @Test
    fun mergeWorkflow_remoteShowsSurviveLocalClear() = runTest {
        // Simulate: local guest shows exist
        val localShows = listOf(
            Show(id = 100L, title = "Local Show 1", status = ShowStatus.SEEN),
            Show(id = 200L, title = "Local Show 2", status = ShowStatus.WANT_TO_SEE)
        )
        localShows.forEach { dao.insertShow(it) }

        // Simulate: capture local shows before destructive operations
        val capturedLocal = dao.getAllShows().first()
        assertEquals(2, capturedLocal.size)

        // Simulate: remote shows fetched successfully
        val remoteShows = listOf(
            Show(id = 300L, title = "Remote Show 1", status = ShowStatus.SEEN, rating = 5),
            Show(id = 400L, title = "Remote Show 2", status = ShowStatus.WANT_TO_SEE)
        )

        // NOW safe to clear and rebuild
        dao.deleteAllShows()

        // Insert remote shows
        remoteShows.forEach { dao.insertShow(it) }

        // Merge local shows with new unique IDs
        val baseTime = System.currentTimeMillis()
        capturedLocal.forEachIndexed { index, show ->
            dao.insertShow(show.copy(id = baseTime + index))
        }

        // Verify all shows exist
        val allShows = dao.getAllShows().first()
        assertEquals(4, allShows.size)

        // Verify remote shows are intact
        assertNotNull(dao.getShowById(300L))
        assertNotNull(dao.getShowById(400L))

        // Verify local shows were re-inserted with new IDs
        val titles = allShows.map { it.title }.toSet()
        assertTrue(titles.contains("Local Show 1"))
        assertTrue(titles.contains("Local Show 2"))
        assertTrue(titles.contains("Remote Show 1"))
        assertTrue(titles.contains("Remote Show 2"))
    }

    @Test
    fun mergeWorkflow_noDataLossIfFetchSucceeds() = runTest {
        // Insert local data
        dao.insertShow(Show(id = 1L, title = "Local", status = ShowStatus.SEEN))

        // Capture local shows (step 1 of safe merge)
        val local = dao.getAllShows().first()

        // Simulate successful remote fetch
        val remote = listOf(Show(id = 2L, title = "Remote", status = ShowStatus.SEEN))

        // Clear and rebuild (step 4 of safe merge)
        dao.deleteAllShows()
        remote.forEach { dao.insertShow(it) }
        local.forEachIndexed { i, show -> dao.insertShow(show.copy(id = System.currentTimeMillis() + i)) }

        // Nothing lost
        val all = dao.getAllShows().first()
        assertEquals(2, all.size)
    }

    // --- Nullable Fields Tests ---

    @Test
    fun nullableFields_persistCorrectly() = runTest {
        val show = Show(
            id = 7000L,
            title = "Minimal Show",
            theater = null,
            date = null,
            status = ShowStatus.WANT_TO_SEE,
            rating = null,
            notes = null
        )
        dao.insertShow(show)

        val retrieved = dao.getShowById(7000L)!!
        assertNull(retrieved.theater)
        assertNull(retrieved.date)
        assertNull(retrieved.rating)
        assertNull(retrieved.notes)
    }

    @Test
    fun allFields_persistCorrectly() = runTest {
        val show = Show(
            id = 8000L,
            title = "Complete Show",
            theater = "Broadway Theatre",
            date = 1700000000000L,
            status = ShowStatus.SEEN,
            rating = 5,
            notes = "Best show ever!"
        )
        dao.insertShow(show)

        val retrieved = dao.getShowById(8000L)!!
        assertEquals("Complete Show", retrieved.title)
        assertEquals("Broadway Theatre", retrieved.theater)
        assertEquals(1700000000000L, retrieved.date)
        assertEquals(ShowStatus.SEEN, retrieved.status)
        assertEquals(5, retrieved.rating)
        assertEquals("Best show ever!", retrieved.notes)
    }

    // --- Status Conversion Tests ---

    @Test
    fun statusEnum_roundTrips() = runTest {
        ShowStatus.entries.forEachIndexed { index, status ->
            val show = Show(id = 9000L + index, title = "Status Test $status", status = status)
            dao.insertShow(show)
            val retrieved = dao.getShowById(9000L + index)
            assertEquals(status, retrieved!!.status)
        }
    }
}
