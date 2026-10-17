package com.rr.numio.calendar.kerala

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/**
 * Just enough astronomy for the Malayalam calendar, all offline:
 * where the Sun and Moon are (sidereal, Lahiri), and sunrise/sunset in Kerala.
 * Formulas from Jean Meeus, "Astronomical Algorithms" (good to a few minutes of time).
 */
object Sky {
    // Kochi: Kerala calendars are reckoned for Kerala, wherever the phone is.
    private const val LAT = 9.93
    private const val LON = 76.27

    private fun rad(d: Double) = Math.toRadians(d)
    private fun norm(d: Double) = ((d % 360.0) + 360.0) % 360.0

    fun julianDay(t: Instant): Double = t.epochSecond / 86400.0 + t.nano / 8.64e13 + 2440587.5

    /** Julian centuries since J2000 (Terrestrial Time, ΔT ≈ 69 s). */
    private fun centuries(jd: Double) = (jd + 69.0 / 86400.0 - 2451545.0) / 36525.0

    /** Apparent tropical longitude of the Sun, degrees. */
    fun sunTropical(jd: Double): Double {
        val t = centuries(jd)
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(rad(m)) +
                (0.019993 - 0.000101 * t) * sin(rad(2 * m)) + 0.000289 * sin(rad(3 * m))
        val omega = 125.04 - 1934.136 * t
        return norm(l0 + c - 0.00569 - 0.00478 * sin(rad(omega)))
    }

    // Main periodic terms of the Moon's longitude (Meeus table 47.A): D, M, M', F, coefficient (1e-6°).
    private val moonTerms = arrayOf(
        intArrayOf(0, 0, 1, 0, 6288774), intArrayOf(2, 0, -1, 0, 1274027), intArrayOf(2, 0, 0, 0, 658314),
        intArrayOf(0, 0, 2, 0, 213618), intArrayOf(0, 1, 0, 0, -185116), intArrayOf(0, 0, 0, 2, -114332),
        intArrayOf(2, 0, -2, 0, 58793), intArrayOf(2, -1, -1, 0, 57066), intArrayOf(2, 0, 1, 0, 53322),
        intArrayOf(2, -1, 0, 0, 45758), intArrayOf(0, 1, -1, 0, -40923), intArrayOf(1, 0, 0, 0, -34720),
        intArrayOf(0, 1, 1, 0, -30383), intArrayOf(2, 0, 0, -2, 15327), intArrayOf(0, 0, 1, 2, -12528),
        intArrayOf(0, 0, 1, -2, 10980), intArrayOf(4, 0, -1, 0, 10675), intArrayOf(0, 0, 3, 0, 10034),
        intArrayOf(4, 0, -2, 0, 8548), intArrayOf(2, 1, -1, 0, -7888), intArrayOf(2, 1, 0, 0, -6766),
        intArrayOf(1, 0, -1, 0, -5163), intArrayOf(1, 1, 0, 0, 4987), intArrayOf(2, -1, 1, 0, 4036),
        intArrayOf(2, 0, 2, 0, 3994), intArrayOf(4, 0, 0, 0, 3861), intArrayOf(2, 0, -3, 0, 3665),
        intArrayOf(0, 1, -2, 0, -2689), intArrayOf(2, 0, -1, 2, -2602), intArrayOf(2, -1, -2, 0, 2390),
        intArrayOf(1, 0, 1, 0, -2348), intArrayOf(2, -2, 0, 0, 2236), intArrayOf(0, 1, 2, 0, -2120),
        intArrayOf(0, 2, 0, 0, -2069),
    )

    /** Apparent tropical longitude of the Moon, degrees. */
    fun moonTropical(jd: Double): Double {
        val t = centuries(jd)
        val lp = 218.3164477 + 481267.88123421 * t
        val d = 297.8501921 + 445267.1114034 * t
        val m = 357.5291092 + 35999.0502909 * t
        val mp = 134.9633964 + 477198.8675055 * t
        val f = 93.2720950 + 483202.0175233 * t
        val e = 1 - 0.002516 * t
        var sum = 0.0
        for (k in moonTerms) {
            val arg = k[0] * d + k[1] * m + k[2] * mp + k[3] * f
            sum += k[4] * e.pow(abs(k[1])) * sin(rad(arg))
        }
        sum += 3958 * sin(rad(119.75 + 131.849 * t)) + 1962 * sin(rad(lp - f)) + 318 * sin(rad(53.09 + 479264.290 * t))
        val omega = 125.04452 - 1934.136261 * t
        return norm(lp + sum / 1e6 - 0.004778 * sin(rad(omega)))
    }

    /** Lahiri ayanamsa, degrees. */
    fun ayanamsa(jd: Double): Double {
        val t = centuries(jd)
        return 23.85306 + 1.39722 * t + 0.00018 * t * t
    }

    fun sunSidereal(t: Instant): Double = julianDay(t).let { norm(sunTropical(it) - ayanamsa(it)) }
    fun moonSidereal(t: Instant): Double = julianDay(t).let { norm(moonTropical(it) - ayanamsa(it)) }

    /** Sunrise and sunset in Kochi on [date]. */
    fun sunriseSunset(date: LocalDate): Pair<Instant, Instant> {
        val midnight = date.atStartOfDay().toInstant(ZoneOffset.UTC)
        fun event(sign: Int): Instant {
            var hours = 12 - LON / 15 + sign * 6
            repeat(4) {
                val jd = julianDay(midnight.plusMillis((hours * 3_600_000).toLong()))
                val t = centuries(jd)
                val lambda = sunTropical(jd)
                val eps = 23.439 - 0.013 * t
                val dec = Math.toDegrees(asin(sin(rad(eps)) * sin(rad(lambda))))
                val ra = Math.toDegrees(atan2(cos(rad(eps)) * sin(rad(lambda)), cos(rad(lambda))))
                val l0 = norm(280.46646 + 36000.76983 * t)
                val eot = (((l0 - ra + 180) % 360 + 360) % 360 - 180) * 4 // minutes
                val cosH = (sin(rad(-0.833)) - sin(rad(LAT)) * sin(rad(dec))) / (cos(rad(LAT)) * cos(rad(dec)))
                val h = Math.toDegrees(acos(cosH.coerceIn(-1.0, 1.0)))
                hours = 12 - LON / 15 - eot / 60 + sign * h / 15
            }
            return midnight.plusMillis((hours * 3_600_000).toLong())
        }
        return event(-1) to event(1)
    }

    /** Which sidereal sign (0 = Mesha/Medam … 11 = Meena/Meenam) the Sun is in at [t]. */
    fun sunSign(t: Instant): Int = floor(sunSidereal(t) / 30).toInt()

    /** Which nakshatra (0 = Ashwathi … 26 = Revathi) the Moon is in at [t]. */
    fun star(t: Instant): Int = floor(moonSidereal(t) / (360.0 / 27)).toInt().coerceIn(0, 26)

    /** How far the Moon is ahead of the Sun, degrees (0 = new moon, 180 = full moon). */
    fun elongation(t: Instant): Double = julianDay(t).let { norm(moonTropical(it) - sunTropical(it)) }

    private const val STEP = 6L * 3600

    /** The new moon just before [t]. */
    fun newMoonBefore(t: Instant): Instant {
        var hi = t
        var lo = t.minusSeconds(STEP)
        while (elongation(lo) <= elongation(hi)) { hi = lo; lo = lo.minusSeconds(STEP) }
        return refineNewMoon(lo, hi)
    }

    /** The new moon just after [t]. */
    fun newMoonAfter(t: Instant): Instant {
        var lo = t
        var hi = t.plusSeconds(STEP)
        while (elongation(hi) >= elongation(lo)) { lo = hi; hi = hi.plusSeconds(STEP) }
        return refineNewMoon(lo, hi)
    }

    private fun refineNewMoon(before: Instant, after: Instant): Instant {
        var lo = before; var hi = after
        repeat(24) {
            val mid = lo.plusSeconds(java.time.Duration.between(lo, hi).seconds / 2)
            if (elongation(mid) > 180) lo = mid else hi = mid
        }
        return hi
    }

    /** Lunar day 1–30 (1–15 waxing, 16–30 waning) at [t]. */
    fun tithi(t: Instant): Int {
        val jd = julianDay(t)
        return floor(norm(moonTropical(jd) - sunTropical(jd)) / 12).toInt() + 1
    }
}
