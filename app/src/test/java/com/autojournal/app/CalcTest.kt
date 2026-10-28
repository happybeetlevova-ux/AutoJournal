package com.autojournal.app

import com.autojournal.app.data.Refuel
import com.autojournal.app.data.TireSet
import com.autojournal.app.data.mileage
import com.autojournal.app.domain.Calc
import org.junit.Assert.assertEquals
import org.junit.Test

class CalcTest {
    @Test
    fun fullToFullConsumption() {
        val d = 86_400_000L
        val list = listOf(
            Refuel(1, 1, 0, 50000, 40.0, 55.0, 2200.0, true),
            Refuel(2, 1, d, 50500, 38.0, 55.0, 2090.0, true),
            Refuel(3, 1, 2 * d, 50800, 20.0, 56.0, 1120.0, false),
            Refuel(4, 1, 3 * d, 51100, 25.0, 56.0, 1400.0, true),
        )
        val s = Calc.fuelStats(list)
        assertEquals(7.6, s.perRefuel[2]!!, 0.001)
        assertEquals(7.5, s.perRefuel[4]!!, 0.001)
    }

    @Test
    fun tireMileage() {
        val t = TireSet(carId = 1, name = "Зима", installed = true, installedAtKm = 10_000, accumulatedKm = 5_000)
        assertEquals(8_000, t.mileage(13_000))
        assertEquals(5_000, t.copy(installed = false, installedAtKm = null).mileage(13_000))
    }
}
