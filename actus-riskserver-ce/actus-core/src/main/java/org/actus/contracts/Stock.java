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
import org.actus.states.StateSpace;
import org.actus.events.EventFactory;
import org.actus.time.ScheduleFactory;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.types.EventType;
import org.actus.util.CommonUtils;
import org.actus.util.Constants;
import org.actus.functions.stk.POF_PRD_STK;
import org.actus.functions.stk.STF_PRD_STK;
import org.actus.functions.stk.POF_TD_STK;
import org.actus.functions.stk.STF_TD_STK;
import org.actus.functions.stk.POF_DV_STK;
import org.actus.functions.stk.STF_DV_STK;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents the Stock payoff algorithm
 * 
 * @see <a href="https://www.actusfrf.org"></a>
 */
public final class Stock {

    // compute next n non-contingent events
    public static ArrayList<ContractEvent> schedule(LocalDateTime to,
                                                    ContractModelProvider model) throws AttributeConversionException {
        ArrayList<ContractEvent> events = new ArrayList<ContractEvent>();

        // purchase
        if (!CommonUtils.isNull(model.getAs("PurchaseDate"))) {
            events.add(EventFactory.createEvent(model.getAs("PurchaseDate"), EventType.PRD, model.getAs("Currency"), new POF_PRD_STK(), new STF_PRD_STK(), model.getAs("ContractID")));
        }
        // dividend payment related
        if (!CommonUtils.isNull(model.getAs("CycleOfDividendPayment"))) {
            if(CommonUtils.isNull(model.getAs("TerminationDate"))) {
                events.addAll(EventFactory.createEvents(
                        ScheduleFactory.createSchedule(
                                model.getAs("CycleAnchorDateOfDividendPayment"),
                                model.<LocalDateTime>getAs("CycleAnchorDateOfDividendPayment").plus(Constants.MAX_LIFETIME_STK),
                                model.getAs("CycleOfDividendPayment"),
                                model.getAs("EndOfMonthConvention")
                        ),
                        EventType.DV,
                        model.getAs("Currency"),
                        new POF_DV_STK(),
                        new STF_DV_STK(),
                        model.getAs("BusinessDayConvention"),
                        model.getAs("ContractID"))
                );
            } else {
                events.addAll(EventFactory.createEvents(ScheduleFactory.createSchedule(model.getAs("CycleAnchorDateOfDividendPayment"),
                        model.getAs("TerminationDate"),
                        model.getAs("CycleOfDividendPayment"),
                        model.getAs("EndOfMonthConvention")),
                        EventType.DV, model.getAs("Currency"), new POF_DV_STK(), new STF_DV_STK(), model.getAs("BusinessDayConvention"), model.getAs("ContractID")));

            }
        }
        // termination
        if (!CommonUtils.isNull(model.getAs("TerminationDate"))) {
            ContractEvent termination =
                    EventFactory.createEvent(model.getAs("TerminationDate"), EventType.TD, model.getAs("Currency"), new POF_TD_STK(), new STF_TD_STK(), model.getAs("ContractID"));
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
        events.forEach(e -> e.eval(states, model, observer, new DayCountCalculator("30E360", null), model.getAs("BusinessDayConvention")));

        // return post events states
        return events;
    }

    private static StateSpace initStateSpace(ContractModelProvider model) throws AttributeConversionException {
        StateSpace states = new StateSpace();

        // initialize state variables
        states.statusDate = model.getAs("StatusDate");
        
        // return the initialized state space
        return states;
    }

}
