/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.events.ContractEvent;
import org.actus.states.StateSpace;
import org.actus.events.EventFactory;
import org.actus.time.ScheduleFactory;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.types.EventType;
import org.actus.util.CommonUtils;
import org.actus.functions.pam.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the Principal At Maturity payoff algorithm
 * 
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class PrincipalAtMaturity {

    // compute next events within period
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        // initial exchange
        events.add(EventFactory.createEvent(model.getAs("InitialExchangeDate"), EventType.IED, model.getAs("Currency"), new POF_IED_PAM(), new STF_IED_PAM(), model.getAs("ContractID")));
        // principal redemption
        events.add(EventFactory.createEvent(model.getAs("MaturityDate"), EventType.MD, model.getAs("Currency"), new POF_MD_PAM(), new STF_MD_PAM(), model.getAs("ContractID")));
        // purchase
        if (!CommonUtils.isNull(model.getAs("PurchaseDate"))) {
            events.add(EventFactory.createEvent(model.getAs("PurchaseDate"), EventType.PRD, model.getAs("Currency"), new POF_PRD_PAM(), new STF_PRD_PAM(), model.getAs("ContractID")));
        }
        // interest payment related
        if (!CommonUtils.isNull(model.getAs("NominalInterestRate")) && (!CommonUtils.isNull(model.getAs("CycleOfInterestPayment")) || !CommonUtils.isNull(model.getAs("CycleAnchorDateOfInterestPayment")))) {
            // raw interest payment events
            Set<ContractEvent> interestEvents = EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("CycleAnchorDateOfInterestPayment"),
                            model.getAs("MaturityDate"),
                            model.getAs("CycleOfInterestPayment"),
                            model.getAs("EndOfMonthConvention"),
                            true
                    ),
                    EventType.IP,
                    model.getAs("Currency"),
                    new POF_IP_PAM(),
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
                                                        new POF_IPCI_PAM(), new STF_IPCI_PAM(), 
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
                        e.fStateTrans(new STF_IPCI_PAM());
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
                    new STF_IPCI_PAM(),
                    model.getAs("BusinessDayConvention"),
                    model.getAs("ContractID")
            ));
        }
        // rate reset
        Set<ContractEvent> rateResetEvents = EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.<LocalDateTime>getAs("CycleAnchorDateOfRateReset"),
                        model.getAs("MaturityDate"),
                        model.getAs("CycleOfRateReset"),
                        model.getAs("EndOfMonthConvention"),
                        false
                ),
                EventType.RR,
                model.getAs("Currency"),
                new POF_RR_PAM(),
                new STF_RR_PAM(),
                model.getAs("BusinessDayConvention"),
                model.getAs("ContractID")
        );

        // adapt fixed rate reset event
        if(!CommonUtils.isNull(model.getAs("NextResetRate"))) {
            ContractEvent fixedEvent = rateResetEvents.stream().sorted().filter(e -> e.compareTo(EventFactory.createEvent(model.getAs("StatusDate"), EventType.AD, model.getAs("Currency"), null, null, model.getAs("ContractID"))) == 1).findFirst().get();
            fixedEvent.fStateTrans(new STF_RRF_PAM());
            fixedEvent.eventType(EventType.RRF);
            rateResetEvents.add(fixedEvent);
        }

        // add all rate reset events
        events.addAll(rateResetEvents);

        // fees (if specified)
        if (!CommonUtils.isNull(model.getAs("CycleOfFee"))) { 
        events.addAll(EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.getAs("CycleAnchorDateOfFee"),
                        model.getAs("MaturityDate"),
                        model.getAs("CycleOfFee"),
                        model.getAs("EndOfMonthConvention"),
                        true
                ),
                EventType.FP,
                model.getAs("Currency"),
                new POF_FP_PAM(),
                new STF_FP_PAM(),
                model.getAs("BusinessDayConvention"),
                model.getAs("ContractID")
        ));
        }
        // scaling (if specified)
        String scalingEffect = model.getAs("ScalingEffect").toString();
        if (!CommonUtils.isNull(scalingEffect) && (scalingEffect.contains("I") || scalingEffect.contains("N"))) { 
        events.addAll(EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.getAs("CycleAnchorDateOfScalingIndex"),
                        model.getAs("MaturityDate"),
                        model.getAs("CycleOfScalingIndex"),
                        model.getAs("EndOfMonthConvention"),
                        false
                ),
                EventType.SC,
                model.getAs("Currency"),
                new POF_SC_PAM(),
                new STF_SC_PAM(),
                model.getAs("BusinessDayConvention"),
                model.getAs("ContractID")
        ));
        }
        // termination
        if (!CommonUtils.isNull(model.getAs("TerminationDate"))) {
            ContractEvent termination = EventFactory.createEvent(
                    model.getAs("TerminationDate"),
                    EventType.TD,
                    model.getAs("Currency"),
                    new POF_TD_PAM(),
                    new STF_TD_PAM(),
                    model.getAs("ContractID")
            );
            events.removeIf(e -> e.compareTo(termination) == 1); // remove all post-termination events
            events.add(termination);
        }
        // remove all pre-status date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(model.getAs("StatusDate"), EventType.AD, model.getAs("Currency"), null,null, model.getAs("ContractID"))) == -1);

        // remove all post to-date events
        events.removeIf(e -> e.compareTo(EventFactory.createEvent(to, EventType.AD, model.getAs("Currency"), null,null, model.getAs("ContractID"))) == 1);

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
        events.forEach(e -> e.eval(states, model, observer, model.getAs("DayCountConvention"), model.getAs("BusinessDayConvention")));
        // remove pre-purchase events if purchase date set
        if(!CommonUtils.isNull(model.getAs("PurchaseDate"))) {
            events.removeIf(e -> !e.eventType().equals(EventType.AD) && e.compareTo(EventFactory.createEvent(model.getAs("PurchaseDate"), EventType.PRD, model.getAs("Currency"), null, null, model.getAs("ContractID"))) == -1);
        }
        // return evaluated events
        return events;
    }

    private static StateSpace initStateSpace(ContractModelProvider model) throws AttributeConversionException {
        StateSpace states = new StateSpace();
        states.notionalScalingMultiplier = model.getAs("NotionalScalingMultiplier");
        states.interestScalingMultiplier = model.getAs("InterestScalingMultiplier");

        states.contractPerformance = model.getAs("ContractPerformance");
        states.statusDate = model.getAs("StatusDate");

        if(model.<LocalDateTime>getAs("InitialExchangeDate").isAfter(model.getAs("StatusDate"))){
            states.notionalPrincipal = 0.0;
            states.nominalInterestRate = 0.0;
        }else{
            states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("ContractRole"))*model.<Double>getAs("NotionalPrincipal");
            states.nominalInterestRate = model.getAs("NominalInterestRate");
        }

        if(CommonUtils.isNull(model.getAs("NominalInterestRate"))){
            states.accruedInterest = 0.0;
        } else if(!CommonUtils.isNull(model.getAs("AccruedInterest"))){
            states.accruedInterest = model.getAs("AccruedInterest");
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
