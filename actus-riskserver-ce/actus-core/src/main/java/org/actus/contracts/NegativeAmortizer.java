/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.events.ContractEvent;
import org.actus.states.StateSpace;
import org.actus.events.EventFactory;
import org.actus.time.ScheduleFactory;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.types.EventType;
import org.actus.types.InterestCalculationBase;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;
import org.actus.functions.pam.*;
import org.actus.functions.lam.*;
import org.actus.functions.nam.*;
import org.actus.functions.StateTransitionFunction;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the Negative Amortizer payoff algorithm
 * 
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class NegativeAmortizer {

    // compute next n non-contingent events
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        // determine maturity of the contract
        LocalDateTime maturity = maturity(model);

        // initial exchange
        events.add(EventFactory.createEvent(
                model.getAs("InitialExchangeDate"),
                EventType.IED, model.getAs("Currency"),
                new POF_IED_PAM(), new STF_IED_LAM(),
                model.getAs("ContractID"))
        );

        // principal redemption schedule
        Set<LocalDateTime> prSchedule = ScheduleFactory.createSchedule(
                model.getAs("CycleAnchorDateOfPrincipalRedemption"),
                maturity,model.getAs("CycleOfPrincipalRedemption"),
                model.getAs("EndOfMonthConvention"),
                false
        );

        // -> chose right state transition function depending on ipcb attributes
        StateTransitionFunction stf= !(InterestCalculationBase.NT.equals(model.<InterestCalculationBase>getAs("InterestCalculationBase")))? new STF_PR_NAM() : new STF_PR2_NAM();

        // regular principal redemption events
        events.addAll(EventFactory.createEvents(
                prSchedule,
                EventType.PR,
                model.getAs("Currency"),
                new POF_PR_NAM(),
                stf,
                model.getAs("BusinessDayConvention"),
                model.getAs("ContractID"))
        );

        events.add(EventFactory.createEvent(
                maturity,
                EventType.MD,
                model.getAs("Currency"),
                new POF_MD_PAM(),new STF_MD_LAM(),
                model.getAs("BusinessDayConvention"),
                model.getAs("ContractID"))
        );

        // purchase
        if (!CommonUtils.isNull(model.getAs("PurchaseDate"))) {
            events.add(EventFactory.createEvent(
                    model.getAs("PurchaseDate"),
                    EventType.PRD,
                    model.getAs("Currency"),
                    new POF_PRD_LAM(),
                    new STF_PRD_LAM(),
                    model.getAs("ContractID"))
            );
        }

        // -> chose right state transition function for IPCI depending on ipcb attributes
        StateTransitionFunction stf_ipci=(!CommonUtils.isNull(model.getAs("InterestCalculationBase")) && model.getAs("InterestCalculationBase").equals(InterestCalculationBase.NTL))? new STF_IPCI_LAM() : new STF_IPCI2_LAM();
        // interest payment related
        if (!CommonUtils.isNull(model.getAs("CycleOfInterestPayment")) || !CommonUtils.isNull(model.getAs("CycleAnchorDateOfInterestPayment"))) {
            // raw interest payment events
            Set<ContractEvent> interestEvents = EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("CycleAnchorDateOfInterestPayment"),
                            maturity,
                            model.getAs("CycleOfInterestPayment"),
                            model.getAs("EndOfMonthConvention"),
                            true
                    ),
                    EventType.IP,
                    model.getAs("Currency"),
                    new POF_IP_LAM(),
                    new STF_IP_PAM(),
                    model.getAs("BusinessDayConvention"),
                    model.getAs("ContractID")
            );

            // adapt if interest capitalization set
            if (!CommonUtils.isNull(model.getAs("CapitalizationEndDate"))) {
                // remove IP and add capitalization event at IPCED instead
                ContractEvent capitalizationEnd = EventFactory.createEvent(model.getAs("CapitalizationEndDate"), 
                                EventType.IPCI,
                                model.getAs("Currency"),
                                new POF_IPCI_PAM(), stf_ipci, 
                                model.getAs("BusinessDayConvention"), 
                                model.getAs("ContractID"));
                interestEvents.removeIf(e -> e.eventType().equals(EventType.IP) && e.compareTo(capitalizationEnd) == 0);
                interestEvents.add(capitalizationEnd);

                // for all events with time <= IPCED && type == "IP" do
                // change type to IPCI and payoff/state-trans functions
                interestEvents.forEach(e -> {
                    if (e.eventType().equals(EventType.IP) && e.compareTo(capitalizationEnd) != 1) {
                    e.eventType(EventType.IPCI);
                    e.fPayOff(new POF_IPCI_PAM());
                    e.fStateTrans(stf_ipci);
                    }
                });
            }
            events.addAll(interestEvents);
            
        }else if(!CommonUtils.isNull(model.getAs("CapitalizationEndDate"))) {
            // if no extra interest schedule set but capitalization end date, add single IPCI event
            events.add(EventFactory.createEvent(
                    model.getAs("CapitalizationEndDate"),
                    EventType.IPCI,
                    model.getAs("Currency"),
                    new POF_IPCI_PAM(),
                    stf_ipci,
                    model.getAs("BusinessDayConvention"),
                    model.getAs("ContractID"))
            );
        }
        // rate reset
        Set<ContractEvent> rateResetEvents = EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.<LocalDateTime>getAs("CycleAnchorDateOfRateReset"),
                        maturity,
                        model.getAs("CycleOfRateReset"),
                        model.getAs("EndOfMonthConvention"),
                        false
                ),
                EventType.RR,
                model.getAs("Currency"),
                new POF_RR_PAM(),
                new STF_RR_LAM(),
                model.getAs("BusinessDayConvention"),
                model.getAs("ContractID")
        );

        // adapt fixed rate reset event
        if(!CommonUtils.isNull(model.getAs("NextResetRate"))) {
            ContractEvent fixedEvent = rateResetEvents.stream().sorted().filter(e -> e.compareTo(EventFactory.createEvent(model.getAs("StatusDate"), EventType.AD, model.getAs("Currency"), null, null, model.getAs("ContractID"))) == 1).findFirst().get();
            fixedEvent.fStateTrans(new STF_RRF_LAM());
            fixedEvent.eventType(EventType.RRF);
            rateResetEvents.add(fixedEvent);
        }
        events.addAll(rateResetEvents);

        // fees (if specified)
        if (!CommonUtils.isNull(model.getAs("CycleOfFee"))) {
            events.addAll(EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("CycleAnchorDateOfFee"),
                            maturity,
                            model.getAs("CycleOfFee"),
                            model.getAs("EndOfMonthConvention")
                    ),
                    EventType.FP,
                    model.getAs("Currency"),
                    new POF_FP_PAM(),
                    new STF_FP_LAM(),
                    model.getAs("BusinessDayConvention"),
                    model.getAs("ContractID"))
            );
        }
        // scaling (if specified)
        if (!CommonUtils.isNull(model.getAs("ScalingEffect")) && (model.getAs("ScalingEffect").toString().contains("I") || model.getAs("ScalingEffect").toString().contains("N"))) {
            events.addAll(EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("CycleAnchorDateOfScalingIndex"),
                            maturity,
                            model.getAs("CycleOfScalingIndex"),
                            model.getAs("EndOfMonthConvention"),
                            false
                    ),
                    EventType.SC,
                    model.getAs("Currency"),
                    new POF_SC_PAM(),
                    new STF_SC_LAM(),
                    model.getAs("BusinessDayConvention"),
                    model.getAs("ContractID"))
            );
        }
        // interest calculation base (if specified)
        if (!CommonUtils.isNull(model.getAs("InterestCalculationBase")) && model.getAs("InterestCalculationBase").equals(InterestCalculationBase.NTL)) {
            events.addAll(EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("CycleAnchorDateOfInterestCalculationBase"),
                            maturity,
                            model.getAs("CycleOfInterestCalculationBase"),
                            model.getAs("EndOfMonthConvention"),
                            false
                    ),
                    EventType.IPCB,
                    model.getAs("Currency"),
                    new POF_IPCB_LAM(),
                    new STF_IPCB_LAM(),
                    model.getAs("BusinessDayConvention"),
                    model.getAs("ContractID"))
            );
        }
        // termination
        if (!CommonUtils.isNull(model.getAs("TerminationDate"))) {
            ContractEvent termination = EventFactory.createEvent(
                            model.getAs("TerminationDate"),
                            EventType.TD,
                            model.getAs("Currency"),
                            new POF_TD_LAM(),
                            new STF_TD_PAM(),
                            model.getAs("ContractID")
            );
            events.removeIf(e -> e.compareTo(termination) == 1); // remove all post-termination events
            events.add(termination);
        }

        // remove all pre-status date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(model.getAs("StatusDate"), EventType.AD, model.getAs("Currency"), null,null, model.getAs("ContractID"))) == -1);

        // remove all post to-date events
        if(CommonUtils.isNull(to)){
            to = maturity;
        }
        ContractEvent postDate = EventFactory.createEvent(to, EventType.AD, model.getAs("Currency"), null, null, model.getAs("ContractID"));
        events.removeIf(e -> e.compareTo(postDate) == 1);

        // sort the events in the payoff-list according to their time of occurence
        Collections.sort(events);

        return events;
    }

    // apply a set of events to the current state of a contract and return the post events state
    public static ArrayList<ContractEvent> apply(ArrayList<ContractEvent> events,
                                                 ContractModelProvider model,
                                                 RiskFactorModelProvider observer) throws AttributeConversionException {

        // initialize state space per status date
        StateSpace states = initStateSpace(model);

        // sort the events according to their time sequence
        Collections.sort(events);

        // apply events according to their time sequence to current state
        ListIterator eventIterator = events.listIterator();
		while (eventIterator.hasNext()) {
			((ContractEvent) eventIterator.next()).eval(states, model, observer, model.getAs("DayCountConvention"),
					model.getAs("BusinessDayConvention"));
        }
        
        // remove pre-purchase events if purchase date set
        if(!CommonUtils.isNull(model.getAs("PurchaseDate"))) {
            events.removeIf(e -> !e.eventType().equals(EventType.AD) && e.compareTo(EventFactory.createEvent(model.getAs("PurchaseDate"), EventType.PRD, model.getAs("Currency"), null, null, model.getAs("ContractID"))) == -1);
        }

        // return evaluated events
        return events;
    }

    // determine maturity of the contract
    private static LocalDateTime maturity(ContractModelProvider model) {
        LocalDateTime maturity = model.getAs("MaturityDate");
        if (CommonUtils.isNull(maturity)) {
            LocalDateTime t0 = model.getAs("StatusDate");
            LocalDateTime pranx = model.getAs("CycleAnchorDateOfPrincipalRedemption");
            LocalDateTime ied = model.getAs("InitialExchangeDate");
            Period prcl = CycleUtils.parsePeriod(model.getAs("CycleOfPrincipalRedemption"));
            LocalDateTime lastEvent;
            if(!CommonUtils.isNull(pranx) && (pranx.isEqual(t0) || pranx.isAfter(t0))) {
                lastEvent = pranx;
            } else if(ied.plus(prcl).isAfter(t0) || ied.plus(prcl).isEqual(t0)) {
                lastEvent = ied.plus(prcl);
            }else{
                Set<LocalDateTime> previousEvents = ScheduleFactory.createSchedule(
                        model.getAs("CycleAnchorDateOfPrincipalRedemption"),
                        model.getAs("StatusDate"),
                        model.getAs("CycleOfPrincipalRedemption"),
                        model.getAs("EndOfMonthConvention")
                    );
                previousEvents.removeIf( d -> d.isBefore(t0));
                previousEvents.remove(t0);
                List<LocalDateTime> prevEventsList = new ArrayList<>(previousEvents);
                Collections.sort(prevEventsList);
                lastEvent = prevEventsList.get(prevEventsList.size()-1);
            }
            double timeFromLastEventPlusOneCycle = model.<DayCountCalculator>getAs("DayCountConvention").dayCountFraction(lastEvent, lastEvent.plus(prcl));
            double redemptionPerCycle = model.<Double>getAs("NextPrincipalRedemptionPayment") - (timeFromLastEventPlusOneCycle * model.<Double>getAs("NominalInterestRate") * model.<Double>getAs("NotionalPrincipal"));
            int remainingPeriods = (int) Math.ceil(model.<Double>getAs("NotionalPrincipal") / redemptionPerCycle)-1;
            maturity = model.<BusinessDayAdjuster>getAs("BusinessDayConvention").shiftEventTime(lastEvent.plus(prcl.multipliedBy(remainingPeriods)));;
        }
        return maturity;
    }

    private static StateSpace initStateSpace(ContractModelProvider model) throws AttributeConversionException {
        StateSpace states = new StateSpace();

        states.notionalScalingMultiplier = model.getAs("NotionalScalingMultiplier");
        states.interestScalingMultiplier = model.getAs("InterestScalingMultiplier");

        states.contractPerformance = model.getAs("ContractPerformance");
        states.statusDate = model.getAs("StatusDate");
        states.nextPrincipalRedemptionPayment = model.<Double>getAs("NextPrincipalRedemptionPayment");

        if(model.<LocalDateTime>getAs("InitialExchangeDate").isAfter(model.getAs("StatusDate"))){
            states.notionalPrincipal = 0.0;
            states.nominalInterestRate = 0.0;
            states.interestCalculationBaseAmount = 0.0;
        }else{
            states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("ContractRole"))*model.<Double>getAs("NotionalPrincipal");
            states.nominalInterestRate = model.getAs("NominalInterestRate");
            if(InterestCalculationBase.NT.equals(model.getAs("InterestCalculationBase"))){
                states.interestCalculationBaseAmount = states.notionalPrincipal; // contractRole applied at notionalPrincipal initialization
            }else{
                states.interestCalculationBaseAmount = ContractRoleConvention.roleSign(model.getAs("ContractRole")) * model.<Double>getAs("InterestCalculationBaseAmount");
            }

        }

        if(CommonUtils.isNull(model.getAs("NominalInterestRate"))){
            states.accruedInterest = 0.0;
        } else if(!CommonUtils.isNull(model.getAs("AccruedInterest"))){
            states.accruedInterest = ContractRoleConvention.roleSign(model.getAs("ContractRole")) * model.<Double>getAs("AccruedInterest");
        } else{
            DayCountCalculator dayCounter = model.getAs("DayCountConvention");
            BusinessDayAdjuster timeAdjuster = model.getAs("BusinessDayConvention");
            List<LocalDateTime> ipSchedule = new ArrayList<>(ScheduleFactory.createSchedule(
                    model.getAs("CycleAnchorDateOfInterestPayment"),
                    model.getAs("MaturityDate"),
                    model.getAs("CycleOfInterestPayment"),
                    model.getAs("EndOfMonthConvention"),
                    true
            ));
            Collections.sort(ipSchedule);
            List<LocalDateTime> dateEarlierThanT0 = ipSchedule.stream().filter(time -> time.isBefore(states.statusDate)).collect(Collectors.toList());
            LocalDateTime tMinus = dateEarlierThanT0.get(dateEarlierThanT0.size() -1);
            states.accruedInterest = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(tMinus), timeAdjuster.shiftCalcTime(states.statusDate))
                    * states.notionalPrincipal
                    * states.nominalInterestRate;
        }

        if(CommonUtils.isNull(model.getAs("FeeRate"))){
            states.feeAccrued = 0.0;
        } else if(!CommonUtils.isNull(model.getAs("FeeAccrued"))){
            states.feeAccrued = model.getAs("FeeAccrued");
        }//TODO: implement last two possible initialization

        // return the initialized state space
        return states;
    }

}
