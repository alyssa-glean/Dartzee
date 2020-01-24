package dartzee.ai

import java.awt.Point
import java.util.*

data class SimulationWrapper(val averageDart: Double,
                             val missPercent: Double,
                             val finishPercent: Double,
                             val treblePercent: Double,
                             val hmPointToCount: HashMap<Point, Int>)
