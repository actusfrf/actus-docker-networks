package org.actus.functions.lax;

import java.time.LocalDateTime;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;

public class STF_RRF_LAX implements StateTransitionFunction {
	double scheduledRate=0;
	
	public STF_RRF_LAX(double rate) {
		this.scheduledRate=rate;
	}

	@Override
	public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model,
			RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
		// compute new rate
		double rate = scheduledRate
			* model.<Double>getAs("RateMultiplier")
			+ model.<Double>getAs("RateSpread") - states.nominalInterestRate;
		double deltaRate = Math.min(
			Math.max(rate, model.<Double>getAs("PeriodFloor"))
			,model.<Double>getAs("PeriodCap"));
		rate = Math.min(
                Math.max(states.nominalInterestRate + deltaRate, model.<Double>getAs("LifeFloor")), model.<Double>getAs("LifeCap"));
		
		// update state space
		double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate),
				timeAdjuster.shiftCalcTime(time));
		states.accruedInterest += states.nominalInterestRate * states.interestCalculationBaseAmount * timeFromLastEvent;
		states.feeAccrued += model.<Double>getAs("FeeRate") * states.notionalPrincipal * timeFromLastEvent;
		states.nominalInterestRate = rate;
		states.statusDate = time;

		// return post-event-states
		return StateSpace.copyStateSpace(states);
	}

}
