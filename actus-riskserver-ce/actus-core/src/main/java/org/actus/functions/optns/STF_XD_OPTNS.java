package org.actus.functions.optns;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.types.ContractReference;
import org.actus.types.OptionType;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class STF_XD_OPTNS implements StateTransitionFunction {
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        OptionType option = model.getAs("OptionType");
        double st = riskFactorModel.stateAt(model.<ArrayList<ContractReference>>getAs("ContractStructure").get(0).getContractAttribute("MarketObjectCode"), time, states, model,true);
        if(option.equals(OptionType.C)){
            states.exerciseAmount = Math.max(st - model.<Double>getAs("OptionStrike1"), 0.0);
        } else if(option.equals(OptionType.P)){
            states.exerciseAmount = Math.max(model.<Double>getAs("OptionStrike1") - st, 0.0);
        } else{
            states.exerciseAmount = Math.max(st - model.<Double>getAs("OptionStrike1"), 0.0) + Math.max(model.<Double>getAs("OptionStrike2") - st, 0.0);
        }
        states.statusDate = time;

        return StateSpace.copyStateSpace(states);
    }
}
