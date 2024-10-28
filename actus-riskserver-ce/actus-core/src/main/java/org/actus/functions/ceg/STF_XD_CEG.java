package org.actus.functions.ceg;

import org.actus.attributes.ContractModelProvider;
import org.actus.contracts.CreditEnhancementGuarantee;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;
import org.actus.types.FeeBasis;
import org.actus.conventions.contractrole.ContractRoleConvention;

import java.time.LocalDateTime;

public class STF_XD_CEG implements StateTransitionFunction {
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        
        if(CommonUtils.isNull(model.getAs("NotionalPrincipal"))){
            states.notionalPrincipal = CreditEnhancementGuarantee.calculateNotionalPrincipal(states, model, riskFactorModel, time);
        }
        states.exerciseAmount = states.notionalPrincipal;
        states.exerciseDate = time;

        if((CommonUtils.isNull(model.getAs("FeeRate")) || model.<Double>getAs("FeeRate")==0.0)) {
            states.feeAccrued = states.feeAccrued;
        } else if(FeeBasis.A.equals(model.getAs("FeeBasis")) && !CommonUtils.isNull(model.getAs("CycleOfFee"))){
            double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time));
            double timeFullFeeCycle = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(states.statusDate.plus(CycleUtils.parsePeriod(model.getAs("CycleOfFee")))));
            states.feeAccrued += ContractRoleConvention.roleSign(model.getAs("ContractRole"))
                                    * timeFromLastEvent / timeFullFeeCycle * model.<Double>getAs("FeeRate");
        }else{
            double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time));
            states.feeAccrued += states.notionalPrincipal * timeFromLastEvent * model.<Double>getAs("FeeRate");
        }
      
        states.statusDate = time;
        return StateSpace.copyStateSpace(states);
    }
}
