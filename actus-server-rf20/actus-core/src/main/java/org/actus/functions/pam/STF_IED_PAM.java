/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.pam;

import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.types.ContractRole;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public final class STF_IED_PAM implements StateTransitionFunction {
    
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states,
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        // update state space
        states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("ContractRole")) * model.<Double>getAs("NotionalPrincipal");
        states.nominalInterestRate = model.getAs("NominalInterestRate");
        states.statusDate = time;

        // if cycle anchor date of interest payment prior to IED, then update nominal accrued accordingly
        if(!CommonUtils.isNull(model.getAs("CycleAnchorDateOfInterestPayment")) &&
                model.<LocalDateTime>getAs("CycleAnchorDateOfInterestPayment").isBefore(model.getAs("InitialExchangeDate"))) {
            states.accruedInterest += states.notionalPrincipal *states.nominalInterestRate *dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(model.<LocalDateTime>getAs("CycleAnchorDateOfInterestPayment")),timeAdjuster.shiftCalcTime(time));
        }

        // return post-event-states
        return StateSpace.copyStateSpace(states);
    }
    
}
