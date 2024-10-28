/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.fxout;

import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.functions.PayOffFunction;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.types.ContractRole;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public final class POF_STD_FXOUT implements PayOffFunction {
    
    @Override
    public double eval(LocalDateTime time, StateSpace states, 
                        ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        double payoff = CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
        * ContractRoleConvention.roleSign(model.getAs("ContractRole"))
        * (model.<Double>getAs("NotionalPrincipal")-riskFactorModel.stateAt(model.getAs("Currency2")+"/"+model.getAs("Currency"), model.getAs("MaturityDate"), states, model,true)
                * model.<Double>getAs("NotionalPrincipal2"));
        return payoff;
    }
}
