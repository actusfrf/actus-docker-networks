package org.actus.functions.lax;

import java.time.LocalDateTime;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;

public final class STF_RR_LAX implements StateTransitionFunction {
	double scheduledRate=0;
	
	public STF_RR_LAX(double rate) {
		this.scheduledRate=rate;
	}
	
	@Override
	public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model,
			RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {

		// compute new rate
		double rate = (riskFactorModel.stateAt(model.getAs("MarketObjectCodeOfRateReset"), time, states, model,true)
                * model.<Double>getAs("RateMultiplier"))
				+ model.<Double>getAs("RateSpread")
				+ scheduledRate - states.nominalInterestRate;
        double deltaRate = Math.min(
                Math.max(rate, model.<Double>getAs("PeriodFloor"))
				,model.<Double>getAs("PeriodCap"));
		rate = Math.min(
			Math.max(states.nominalInterestRate + deltaRate, model.<Double>getAs("LifeFloor"))
			,model.<Double>getAs("LifeCap"));
		
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

