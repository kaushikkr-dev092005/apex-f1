package com.example.data.explore

import com.example.data.race.*
import java.util.Locale

enum class EvidenceKind { Observed, Assumed, Estimated, Unavailable }
data class EvidenceLine(val kind: EvidenceKind, val text: String)
data class PitWallAnswer(val question: String, val summary: String, val evidence: List<EvidenceLine>, val source: String, val eventTimeMs: Long?, val arrivalTimeMs: Long?)
data class PitWallContext(val driverCode: String, val timing: Timing?, val status: TrackStatus, val demo: Boolean, val eventTimeMs: Long?, val arrivalTimeMs: Long?, val plan: StrategyPlan)
interface RaceExplainer { fun explain(question: String, context: PitWallContext): PitWallAnswer }

/** A local evidence/rules explainer. Never attributes hidden intent or impersonates a connected LLM. */
object PitWallEngine : RaceExplainer {
    override fun explain(question: String, context: PitWallContext): PitWallAnswer {
        val q=question.trim().take(400)
        require(q.isNotBlank())
        val row=context.timing
        val lines=mutableListOf<EvidenceLine>()
        if(row!=null) {
            lines.add(EvidenceLine(EvidenceKind.Observed,"${context.driverCode}: ${formatLap(row.lastLapMs)} last lap, ${row.tyre} tyres aged ${row.tyreAge} laps, ${row.stops} recorded stop(s)."))
            lines.add(EvidenceLine(EvidenceKind.Observed,"Gap to leader ${formatGap(row.gapMs)}; track status ${context.status.name}."))
        } else lines.add(EvidenceLine(EvidenceKind.Unavailable,"No timing sample is available for this driver."))
        val summary=when {
            q.contains("rain",true)||q.contains("weather",true)->{
                lines.add(EvidenceLine(EvidenceKind.Assumed,"${context.plan.rainPercent}% rain scenario weight is user supplied, not a weather forecast."))
                lines.add(EvidenceLine(EvidenceKind.Unavailable,"No observed weather or team weather model is connected."))
                "Weather can change the tyre trade-off, but this feed cannot establish the chance of rain."
            }
            q.contains("red",true)||q.contains("safety",true)->{
                lines.add(EvidenceLine(EvidenceKind.Assumed,"The simulator uses 60% of the entered pit loss under its hypothetical safety-car condition."))
                lines.add(EvidenceLine(EvidenceKind.Unavailable,"Actual neutralised pit loss and restart conditions are not measured here."))
                "Neutralisation can change the cost of stopping. A red flag does not by itself establish the optimal strategy."
            }
            else->{
                lines.add(EvidenceLine(EvidenceKind.Assumed,"Pit loss ${context.plan.pitLossSeconds}s; degradation multiplier ${context.plan.degradationFactor}; ${context.plan.laps}-lap generic exercise."))
                if(StrategyEngine.validate(context.plan)==null) {
                    val result=StrategyEngine.simulate(context.plan)
                    lines.add(EvidenceLine(EvidenceKind.Estimated,String.format(Locale.US,"This plan differs from the model baseline by %+.1f seconds at the median of 80 seeded scenarios. Negative means faster in this model.",result.medianDeltaSeconds)))
                }
                lines.add(EvidenceLine(EvidenceKind.Unavailable,"Team intent, future traffic, live tyre temperatures and calibrated degradation are unknown."))
                "Pitting trades track position for fresher tyres. These samples support a comparison, not a claim about why a team made its decision."
            }
        }
        return PitWallAnswer(q,summary,lines,if(context.demo)"APEX deterministic exhibition · simulated observations"else"Connected normalized timing provider",context.eventTimeMs,context.arrivalTimeMs)
    }
}
