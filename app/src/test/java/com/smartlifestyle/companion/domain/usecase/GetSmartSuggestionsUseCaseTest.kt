package com.smartlifestyle.companion.domain.usecase

import com.smartlifestyle.companion.domain.model.RoutineTask
import com.smartlifestyle.companion.domain.model.UserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * GetSmartSuggestionsUseCase is pure Kotlin (no Android framework types), so these
 * run as fast local JVM tests with no emulator/mocking needed - the payoff of
 * keeping the recommendation engine framework-free, as noted in its own doc comment.
 */
class GetSmartSuggestionsUseCaseTest {

    private val useCase = GetSmartSuggestionsUseCase()

    private fun task(
        id: String = "task-1",
        title: String = "Generic task",
        scheduledTimeMillis: Long,
        isCompleted: Boolean = false
    ) = RoutineTask(
        id = id,
        title = title,
        scheduledTimeMillis = scheduledTimeMillis,
        isCompleted = isCompleted,
        priorityScore = 0f
    )

    private fun contextAt(
        currentTimeMillis: Long,
        isRaining: Boolean = false,
        minutesSinceLastMovement: Int = 0,
        ambientLux: Float? = null
    ) = UserContext(
        currentTimeMillis = currentTimeMillis,
        stepsToday = 0,
        isRaining = isRaining,
        minutesSinceLastMovement = minutesSinceLastMovement,
        ambientLux = ambientLux
    )

    /** Builds a timestamp for today at a specific hour, for time-of-day-dependent rules. */
    private fun timeAtHour(hour: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

    @Test
    fun `completed tasks always score below incomplete ones`() {
        val now = System.currentTimeMillis()
        val completed = task(id = "a", scheduledTimeMillis = now - 10_000, isCompleted = true)
        val incomplete = task(id = "b", scheduledTimeMillis = now + 100_000, isCompleted = false)

        val ranked = useCase(listOf(completed, incomplete), contextAt(now))

        assertEquals("b", ranked.first().id)
    }

    @Test
    fun `overdue tasks are ranked above tasks that are not yet due`() {
        val now = System.currentTimeMillis()
        val overdue = task(id = "overdue", scheduledTimeMillis = now - TimeUnit.HOURS.toMillis(2))
        val future = task(id = "future", scheduledTimeMillis = now + TimeUnit.DAYS.toMillis(1))

        val ranked = useCase(listOf(future, overdue), contextAt(now))

        assertEquals("overdue", ranked.first().id)
    }

    @Test
    fun `tasks due within the next hour get an urgency boost`() {
        val now = System.currentTimeMillis()
        val dueSoon = task(id = "soon", scheduledTimeMillis = now + TimeUnit.MINUTES.toMillis(30))
        val dueLater = task(id = "later", scheduledTimeMillis = now + TimeUnit.HOURS.toMillis(5))

        val ranked = useCase(listOf(dueLater, dueSoon), contextAt(now))

        assertEquals("soon", ranked.first().id)
    }

    @Test
    fun `movement tasks are boosted after prolonged inactivity`() {
        val now = System.currentTimeMillis()
        val walk = task(id = "walk", title = "Evening walk", scheduledTimeMillis = now + TimeUnit.HOURS.toMillis(3))

        val sedentaryScore = useCase(
            listOf(walk),
            contextAt(now, minutesSinceLastMovement = 120)
        ).first().priorityScore

        val activeScore = useCase(
            listOf(walk),
            contextAt(now, minutesSinceLastMovement = 10)
        ).first().priorityScore

        assertTrue("expected sedentary score ($sedentaryScore) > active score ($activeScore)", sedentaryScore > activeScore)
    }

    @Test
    fun `outdoor tasks are deprioritised when it is raining`() {
        val now = System.currentTimeMillis()
        val run = task(id = "run", title = "Morning run", scheduledTimeMillis = now + TimeUnit.HOURS.toMillis(1))

        val dryScore = useCase(listOf(run), contextAt(now, isRaining = false)).first().priorityScore
        val rainyScore = useCase(listOf(run), contextAt(now, isRaining = true)).first().priorityScore

        assertTrue("expected rainy score ($rainyScore) < dry score ($dryScore)", rainyScore < dryScore)
    }

    @Test
    fun `wind-down tasks are boosted in a dark room during evening hours`() {
        val eveningTime = timeAtHour(21)
        val relax = task(id = "relax", title = "Wind down with reading", scheduledTimeMillis = eveningTime)

        val darkScore = useCase(
            listOf(relax),
            contextAt(eveningTime, ambientLux = 5f)
        ).first().priorityScore

        val brightScore = useCase(
            listOf(relax),
            contextAt(eveningTime, ambientLux = 300f)
        ).first().priorityScore

        assertTrue("expected dark-room score ($darkScore) > bright-room score ($brightScore)", darkScore > brightScore)
    }

    @Test
    fun `wind-down boost does not apply outside evening hours`() {
        val morningTime = timeAtHour(8)
        val relax = task(id = "relax", title = "Wind down with reading", scheduledTimeMillis = morningTime)

        val score = useCase(
            listOf(relax),
            contextAt(morningTime, ambientLux = 5f)
        ).first().priorityScore

        // No evening-hours match, so only whatever generic rules apply (none, here) should fire.
        assertEquals(0f, score)
    }

    @Test
    fun `results are sorted by priority score descending`() {
        val now = System.currentTimeMillis()
        val low = task(id = "low", scheduledTimeMillis = now + TimeUnit.DAYS.toMillis(2))
        val high = task(id = "high", scheduledTimeMillis = now - TimeUnit.HOURS.toMillis(1)) // overdue

        val ranked = useCase(listOf(low, high), contextAt(now))

        assertTrue(ranked[0].priorityScore >= ranked[1].priorityScore)
    }
}
