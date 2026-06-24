package domain.flight_computer

interface FlightComputer {
    fun calculateSafetyCircles(glideRatio: Int): List<SafetyCircle>
}
