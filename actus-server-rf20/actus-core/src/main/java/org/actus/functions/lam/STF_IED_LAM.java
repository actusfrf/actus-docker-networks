/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.lam;

import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.functions.StateTransitionFunction;
import org.actus.types.ContractRole;
import org.actus.types.InterestCalculationBase;
import org.actus.util.CommonUtils;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;

import java.time.LocalDateTime;

public final class STF_IED_LAM implements StateTransitionFunction {
    
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states,
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        // update state space
        states.statusDate = time;
        states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("ContractRole"))* model.<Double>getAs("NotionalPrincipal");
        states.nominalInterestRate = model.<Double>getAs("NominalInterestRate");


        if(InterestCalculationBase.NT.equals(model.<InterestCalculationBase>getAs("InterestCalculationBase"))){
            states.interestCalculationBaseAmount = states.notionalPrincipal;
        }else {
            states.interestCalculationBaseAmount = ContractRoleConvention.roleSign(model.getAs("ContractRole")) * model.<Double>getAs("InterestCalculationBaseAmount");
        }

        if(!CommonUtils.isNull(model.getAs("AccruedInterest"))){
            states.accruedInterest = ContractRoleConvention.roleSign(model.getAs("ContractRole")) * model.<Double>getAs("AccruedInterest");
        }else if(!CommonUtils.isNull(model.getAs("CycleAnchorDateOfInterestPayment")) &&
                model.<LocalDateTime>getAs("CycleAnchorDateOfInterestPayment").isBefore(time)) {
            states.accruedInterest = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(model.<LocalDateTime>getAs("CycleAnchorDateOfInterestPayment")),timeAdjuster.shiftCalcTime(time))
                    * states.notionalPrincipal
                    * states.interestCalculationBaseAmount;
        } else{
            states.accruedInterest = 0.0;
        }

        // return post-event-states
        return StateSpace.copyStateSpace(states);
    }
    
}
