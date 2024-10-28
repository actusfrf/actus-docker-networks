/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.contracts;

import org.actus.AttributeConversionException;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.events.ContractEvent;
import org.actus.functions.pam.*;
import org.actus.states.StateSpace;
import org.actus.events.EventFactory;
import org.actus.time.ScheduleFactory;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.types.EventType;
import org.actus.util.CommonUtils;
import org.actus.functions.clm.POF_IED_CLM;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents the Undefined Maturity Profile payoff algorithm
 *
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class UndefinedMaturityProfile {

    // forward projection of the entire lifecycle of the contract
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        // initial exchange
        events.add(EventFactory.createEvent(
                model.getAs("InitialExchangeDate"),
                EventType.IED,
                model.getAs("Currency"),
                new POF_IED_CLM(),
                new STF_IED_PAM(),
                model.getAs("ContractID"))
        );
        // interest payment capitalization
        events.addAll(EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.getAs("CycleAnchorDateOfInterestPayment"),
                        to,
                        model.getAs("CycleOfInterestPayment"),
                        model.getAs("EndOfMonthConvention"),
                        false
                ),
                EventType.IPCI,
                model.getAs("Currency"),
                new POF_IPCI_PAM(),
                new STF_IPCI_PAM(),
                model.getAs("BusinessDayConvention"),
                model.getAs("ContractID"))
        );
        // rate reset
        Set<ContractEvent> rateResetEvents = EventFactory.createEvents(
                ScheduleFactory.createSchedule(
                        model.<LocalDateTime>getAs("CycleAnchorDateOfRateReset"),
                        to,
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
        events.addAll(rateResetEvents);

        // fees (if specified)
        if (!CommonUtils.isNull(model.getAs("CycleOfFee"))) {
            events.addAll(EventFactory.createEvents(
                    ScheduleFactory.createSchedule(
                            model.getAs("CycleAnchorDateOfFee"),
                            to,
                            model.getAs("CycleOfFee"),
                            model.getAs("EndOfMonthConvention"),
                            false
                    ),
                    EventType.FP,
                    model.getAs("Currency"),
                    new POF_FP_PAM(),
                    new STF_FP_PAM(),
                    model.getAs("BusinessDayConvention"),
                    model.getAs("ContractID"))
            );
        }

        // termination
        if (!CommonUtils.isNull(model.getAs("TerminationDate"))) {
            ContractEvent termination =
                    EventFactory.createEvent(
                            model.getAs("TerminationDate"),
                            EventType.TD, model.getAs("Currency"),
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

        // return evaluated events
        return events;
    }

    private static StateSpace initStateSpace(ContractModelProvider model) throws AttributeConversionException {
        StateSpace states = new StateSpace();
        states.notionalScalingMultiplier = 1;
        states.interestScalingMultiplier = 1;
        
        states.statusDate = model.getAs("StatusDate");
        if (!model.<LocalDateTime>getAs("InitialExchangeDate").isAfter(model.getAs("StatusDate"))) {
            states.notionalPrincipal = ContractRoleConvention.roleSign(model.getAs("ContractRole"))*model.<Double>getAs("NotionalPrincipal");
            states.nominalInterestRate = model.getAs("NominalInterestRate");
            states.accruedInterest = ContractRoleConvention.roleSign(model.getAs("ContractRole"))*model.<Double>getAs("AccruedInterest");
            states.feeAccrued = model.getAs("FeeAccrued");
        }

        // return the initialized state space
        return states;
    }

}
