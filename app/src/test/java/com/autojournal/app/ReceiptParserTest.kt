package com.autojournal.app

import com.autojournal.app.domain.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {
    private val today = LocalDate.of(2026, 10, 28)

    @Test
    fun lukoil() {
        val r = ReceiptParser.parse(
            "AЗC No 123\n1. Al-95 ECTO\n40.00 л x 56.20\n=2248.00\nMTOF =2248.00\n26.10.2026 14:35", today
        )
        assertEquals(40.0, r.liters!!, 0.001)
        assertEquals(56.2, r.price!!, 0.001)
        assertEquals(2248.0, r.total!!, 0.001)
        assertEquals("АИ-95", r.grade)
        assertEquals(LocalDate.of(2026, 10, 26), r.date)
    }

    @Test
    fun thousandsWithSpace() {
        val r = ReceiptParser.parse("G-Drive 95\nКол-во 31,557 n\nЦена 61,40\nСумма 1 937,60\n25/10/26", today)
        assertEquals(31.557, r.liters!!, 0.001)
        assertEquals(1937.6, r.total!!, 0.001)
    }

    @Test
    fun integerLiters() {
        val r = ReceiptParser.parse("AN-92 20 L 48.90 978.00 TOTAL 978.00", today)
        assertEquals(20.0, r.liters!!, 0.001)
        assertEquals("АИ-92", r.grade)
    }

    @Test
    fun onlyTotal() {
        val r = ReceiptParser.parse("ДТ Евро\nИTOГ: 2000.00", today)
        assertEquals(2000.0, r.total!!, 0.001)
        assertEquals("ДТ", r.grade)
    }
}
