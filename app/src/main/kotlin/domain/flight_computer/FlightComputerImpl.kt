package domain.flight_computer

import javax.inject.Inject

class FlightComputerImpl @Inject constructor() : FlightComputer {

    override fun calculateSafetyCircles(glideRatio: Int): List<SafetyCircle> =
        SAFETY_ALTITUDES.map { alt ->
            SafetyCircle(alt, (alt - MIN_RETURN_ALT_FT) * FEET_TO_METERS * glideRatio)
        }

    companion object {
        private const val FEET_TO_METERS = 0.3048
        private const val MIN_RETURN_ALT_FT = 1500
        private val SAFETY_ALTITUDES = listOf(2000, 3000, 4000, 5000, 6000, 7000)
    }
}
