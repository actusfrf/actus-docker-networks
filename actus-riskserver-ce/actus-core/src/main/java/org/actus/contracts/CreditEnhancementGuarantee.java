/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.events.ContractEvent;
import org.actus.events.EventFactory;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.ceg.*;
import org.actus.functions.optns.*;
import org.actus.states.StateSpace;
import org.actus.time.ScheduleFactory;
import org.actus.types.ContractReference;
import org.actus.types.CreditEventTypeCovered;
import org.actus.types.EventType;
import org.actus.types.GuaranteedExposure;
import org.actus.types.ReferenceRole;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the Credit Enhancement Guarantee contract algorithm
 *
 * @see <a https://www.actusfrf.org"></a>
 */
public class CreditEnhancementGuarantee {
    // forward projection of the entire lifecycle of the contract
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<>();

        // determine maturity date
        LocalDateTime maturity = maturity(model);
        
        // purchase
        if (!CommonUtils.isNull(model.getAs("PurchaseDate"))) {
            events.add(EventFactory.createEvent(model.getAs("PurchaseDate"), EventType.PRD, model.getAs("Currency"), new POF_PRD_OPTNS(), new STF_PRD_CEG(), model.getAs("ContractID")));
        }

        // fees (if specified)
        if(!(CommonUtils.isNull(model.getAs("FeeRate")) || model.<Double>getAs("FeeRate") == 0.0)){
            LocalDateTime startDate;
            LocalDateTime endDate;
            if(CommonUtils.isNull(model.getAs("CycleAnchorDateOfFee")) && CommonUtils.isNull(model.getAs("CycleOfFee"))){
                startDate = null;
            }else if(CommonUtils.isNull(model.getAs("CycleAnchorDateOfFee"))){
                startDate = model.<LocalDateTime>getAs("PurchaseDate").plus(Period.parse(model.getAs("CycleOfFee")));
            }else{
                startDate = model.getAs("CycleAnchorDateOfFee");
            }
            if(CommonUtils.isNull(model.getAs("ExerciseDate"))){
                endDate = maturity;
            }else {
                endDate = model.getAs("ExerciseDate");
            }
            events.addAll(EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            startDate,
                            endDate,
                            model.getAs("CycleOfFee"),
                            model.getAs("EndOfMonthConvention"),
                            false),
                    EventType.FP,
                    model.getAs("Currency"),
                    new POF_FP_CEG(),
                    new STF_FP_CEG(),
                    model.getAs("BusinessDayConvention"),
                    model.getAs("ContractID"))
            );
        }

        // maturity
        if(CommonUtils.isNull(model.getAs("ExerciseDate"))){
            events.add(EventFactory.createEvent(maturity, EventType.MD, model.getAs("Currency"), new POF_MD_CEG(), new STF_MD_CEG(), model.getAs("ContractID")));
        }

        //exercise
        if(!CommonUtils.isNull(model.getAs("ExerciseDate"))){
            events.add(EventFactory.createEvent(model.getAs("ExerciseDate"), EventType.XD, model.getAs("Currency"), new POF_XD_OPTNS(), new STF_XD_CEG(), model.getAs("ContractID")));
            events.add(EventFactory.createEvent(model.<LocalDateTime>getAs("ExerciseDate").plus(CycleUtils.parsePeriod(model.getAs("SettlementPeriod"))), EventType.STD, model.getAs("Currency"), new POF_STD_CEG(), new STF_STD_CEG(), model.getAs("BusinessDayConvention"), model.getAs("ContractID")));
        }
        return events;
    }

    // apply a set of events to the current state of a contract and return the post events state
    public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events,
                                                 ContractModelProvider model,
                                                 RiskFactorModelProvider observer) throws AttributeConversionException {
        // determine maturity date
        LocalDateTime maturity = maturity(model);
        events = addExternalXDEvent(model, events, observer, maturity);
        // initialize state space per status date
        StateSpace states = initStateSpace(model, observer, maturity);
        // sort the events according to their time sequence
        Collections.sort(events);

        // apply events according to their time sequence to current state
        events.forEach(e -> {
            e.eval(states, model, observer, model.getAs("DayCountConvention"), model.getAs("BusinessDayConvention"));
        });

        // return post events states
        return events;
    }

    // determine maturity of the contract
    private static LocalDateTime maturity(ContractModelProvider model) {
        LocalDateTime maturityDate = null;
        if(!CommonUtils.isNull(model.getAs("MaturityDate"))){
            maturityDate = model.getAs("MaturityDate");
        } else{
            List<ContractReference> coveredContractRefs = model.<List<ContractReference>>getAs("ContractStructure").stream().filter(ref -> ref.referenceRole.equals(ReferenceRole.COVE)).collect(Collectors.toList());
            List<LocalDateTime> maturityDates = new ArrayList<>();
            coveredContractRefs.forEach(c -> {
                maturityDates.add(LocalDateTime.parse(c.getContractAttribute("MaturityDate")));
            });
            Collections.sort(maturityDates);
            maturityDate = maturityDates.get(maturityDates.size()-1);
        }
        return maturityDate;
    }

    public static StateSpace initStateSpace(ContractModelProvider model, RiskFactorModelProvider observer, LocalDateTime maturity) throws AttributeConversionException {
        StateSpace states = new StateSpace();
        states.maturityDate = maturity;
        states.statusDate = model.getAs("StatusDate");

        if(states.statusDate.isAfter(states.maturityDate)){
            states.notionalPrincipal = 0.0;
        }else if(!CommonUtils.isNull(model.<Double>getAs("NotionalPrincipal"))){
            states.notionalPrincipal = model.<Double>getAs("CoverageOfCreditEnhancement")
                    * ContractRoleConvention.roleSign(model.getAs("ContractRole"))
                    * model.<Double>getAs("NotionalPrincipal");
        } else{
            states.notionalPrincipal = CreditEnhancementGuarantee.calculateNotionalPrincipal(states,model,observer,states.statusDate);
        }

        if(CommonUtils.isNull(model.getAs("FeeRate"))){
            states.feeAccrued = 0.0;
        } else if(!CommonUtils.isNull(model.getAs("FeeAccrued"))){
            states.feeAccrued = model.getAs("FeeAccrued");
        }//TODO: implement last two possible initialization

        states.exerciseAmount = model.getAs("ExerciseAmount");
        states.exerciseDate = model.getAs("ExerciseDate");
        // return the initialized state space
        return states;
    }

    public static Double calculateNotionalPrincipal(StateSpace states, ContractModelProvider model, RiskFactorModelProvider observer, LocalDateTime time){
        List<ContractReference> coveredContractRefs = model.<List<ContractReference>>getAs("ContractStructure").stream().filter(ref -> ref.referenceRole.equals(ReferenceRole.COVE)).collect(Collectors.toList());
        List<StateSpace> statesAtTimePoint = coveredContractRefs.stream().map(c -> c.getStateSpaceAtTimepoint(time,observer)).collect(Collectors.toList());

        if(GuaranteedExposure.NO.equals(model.getAs("GuaranteedExposure"))){
            states.notionalPrincipal =
                    model.<Double>getAs("CoverageOfCreditEnhancement")
                            * ContractRoleConvention.roleSign(model.getAs("ContractRole"))
                            * statesAtTimePoint.stream().map(s -> s.notionalPrincipal).reduce(0.0, Double::sum);
        }else if(GuaranteedExposure.NI.equals(model.getAs("GuaranteedExposure"))){
            states.notionalPrincipal =
                    model.<Double>getAs("CoverageOfCreditEnhancement")
                            * ContractRoleConvention.roleSign(model.getAs("ContractRole"))
                            * (statesAtTimePoint.stream().map(s -> s.notionalPrincipal).reduce(0.0, Double::sum)
                            + statesAtTimePoint.stream().map(s -> s.accruedInterest).reduce(0.0, Double::sum))
            ;
        }else{
            List<String> marketObjectCodesOfUnderlying = coveredContractRefs.stream().map(c -> c.getContractAttribute("MarketObjectCode")).collect(Collectors.toList());
            states.notionalPrincipal =
                    model.<Double>getAs("CoverageOfCreditEnhancement")
                            * ContractRoleConvention.roleSign(model.getAs("ContractRole"))
                            * marketObjectCodesOfUnderlying.stream().map(s -> observer.stateAt(s,time,states,model,true)).reduce(0.0, Double::sum);
        }
        return states.notionalPrincipal;
    }

    private static ArrayList<ContractEvent> addExternalXDEvent(ContractModelProvider model, ArrayList<ContractEvent> events, RiskFactorModelProvider observer, LocalDateTime maturity){
        List<String> contractIdentifiers = model.<List<ContractReference>>getAs("ContractStructure").stream().map(c -> c.getContractAttribute("ContractID")).collect(Collectors.toList());
        CreditEventTypeCovered creditEventTypeCovered = model.<CreditEventTypeCovered[]>getAs("CreditEventTypeCovered")[0];
        // fetch observed events from external data observer
        Set<ContractEvent> observedEvents = observer.events(model);
        // filter relevant credit events:
        // - emitted by any of the covered contracts
        // - emitted before maturity of the guarantee
        // - credit event type that is actually covered under the guarantee
        List<ContractEvent> ceEvents = observedEvents.stream().filter(e -> contractIdentifiers.contains(e.getContractID()) && 
                                                                            !maturity.isBefore(e.eventTime()) &&
                                                                            e.states().contractPerformance.toString().equals(creditEventTypeCovered.toString())).collect(Collectors.toList());
        if(ceEvents.size() > 0 ){
            ContractEvent ceEvent = ceEvents.get(0);
            events = events.stream().filter(e -> e.eventType() != EventType.MD).collect(Collectors.toCollection(ArrayList::new));
            events.add(EventFactory.createEvent(ceEvent.eventTime(), EventType.XD, model.getAs("Currency"), new POF_XD_OPTNS(), new STF_XD_CEG(), model.getAs("ContractID")));
            ContractEvent std = EventFactory.createEvent(ceEvent.eventTime().plus(CycleUtils.parsePeriod(model.getAs("SettlementPeriod"))), EventType.STD, model.getAs("Currency"), new POF_STD_CEG(), new STF_STD_CEG(), model.getAs("BusinessDayConvention"), model.getAs("ContractID"));
            events.add(std);
        }
        return events;
    }
}