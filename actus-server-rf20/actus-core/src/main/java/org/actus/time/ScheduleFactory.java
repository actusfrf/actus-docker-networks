/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.time;

import org.actus.AttributeConversionException;
import org.actus.types.EndOfMonthConventionEnum;
import org.actus.util.CommonUtils;
import org.actus.util.StringUtils;
import org.actus.util.CycleUtils;
import org.actus.conventions.endofmonth.EndOfMonthAdjuster;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.Set;
import java.util.HashSet;

/**
 * A factory for date-schedules
 * <p>
 * This component allows to create a schedule of dates from a [Start-Date]/[Cycle]/[End-Date]/
 * [EndOfMonthConvention] specification. The schedule consists of a series of dates starting 
 * at [Start-Date] and continuing with cyclical dates with periodicity according to [Cycle] 
 * up to [End-Date].
 * <p>
 * Cyclical dates (and cyclical dates only) are adjusted for end-of-month effects if [Cycle]
 * is based on a monthly (or multiple thereof) period, [Start-Date] represents the end of a
 * month, and [EndOfMonthConvention] indicates that such effects should be corrected for.
 * <p>
 * [Start-Date], [Cycle], [End-Date], and [EndOfMonthConvention] are expected as Strings 
 * representing dates, cycle, and end-of-month-convention, respectively, in the ACTUS format.
 * <p>
 * The class is a utility class with only static methods and no constructor, i.e. instances
 * of this class cannot be created but its methods only be accessed in a static way.
 */
public final class ScheduleFactory {
    
    // this is a utility class so no need for instantiation
	private ScheduleFactory() {
	}

	/**
	 * Create a schedule of dates
	 *
	 * Depending on which schedule parameters are provided, a set of
	 * dates is generated from (including) startTime to (including)
	 * endTime with a periodic {@code cycle} (if provided).
	 *
	 * Note, a time at {@code endTime} is always added.
	 *
	 * @param startTime the start time of the schedule
	 * @param endTime the end time of the schedule
	 * @param cycle the schedule cycle
	 * @param endOfMonthConvention the convention to be applied
	 * @return an unordered set of schedule times
	 * 
	 */
	public static Set<LocalDateTime> createSchedule(LocalDateTime startTime, LocalDateTime endTime, String cycle, EndOfMonthConventionEnum endOfMonthConvention) throws AttributeConversionException {
		return ScheduleFactory.createSchedule(startTime,endTime,cycle,endOfMonthConvention,true);
	}


	/**
	 * Create a schedule of dates including or not the schedule end time
	 *
	 * Depending on which schedule parameters are provided, a set of
	 * dates is generated from (including) startTime to (including)
	 * endTime with a periodic {@code cycle} (if provided).
	 *
	 * Parameter {@code addEndTime} allows for specifying whether or not
	 * an additional time should be added to the schedule at the schedule
	 * {@code endtime}.
	 *
	 * @param startTime the start time of the schedule
	 * @param endTime the end time of the schedule
	 * @param cycle the schedule cycle
	 * @param endOfMonthConvention the convention to be applied
	 * @param addEndTime should an additional time be generated at {@code endTime}
	 * @return an unordered set of schedule times
	 *
	 */
	public static Set<LocalDateTime> createSchedule(LocalDateTime startTime, LocalDateTime endTime, String cycle, EndOfMonthConventionEnum endOfMonthConvention, boolean addEndTime) throws AttributeConversionException {
		EndOfMonthAdjuster shifter;
		Set<LocalDateTime> timesSet = new HashSet<LocalDateTime>();
        char stub;

		// if no cycle then only start (if specified) and end dates
		if (CommonUtils.isNull(cycle)) {
		    if (!CommonUtils.isNull(startTime)) {
		      timesSet.add(startTime);
		    }
		    // add or not additional time at endTime
			if(addEndTime) {
				timesSet.add(endTime);
			} else {
				if(endTime.equals(startTime)) timesSet.remove(startTime);
			}
			return timesSet;
		}

		Period period;
		Period increment;
        // parse stub
        stub = CycleUtils.parseStub(cycle);
        
        // parse end of month convention
        shifter = new EndOfMonthAdjuster(endOfMonthConvention, startTime, cycle);
		
		// parse cycle
		period = CycleUtils.parsePeriod(cycle);

		// init helpers for schedule creation
		LocalDateTime newTime = LocalDateTime.from(startTime);

		// create schedule based on end-of-month-convention
		int counter = 1;
		while (newTime.isBefore(endTime)) {
			timesSet.add(newTime);
			increment = period.multipliedBy(counter);
			newTime = shifter.shift(startTime.plus(increment));
			counter++;
		}

		// add (or not) additional time at endTime
		if(addEndTime) {
			timesSet.add(endTime);
		} else {
			if(endTime.equals(startTime)) timesSet.remove(startTime);
		}

        // now adjust for the last stub
		if (stub == StringUtils.LongStub && timesSet.size() > 2 && !endTime.equals(newTime)) {
			timesSet.remove(shifter.shift(startTime.plus(period.multipliedBy((counter - 2)))));
		}
		
		// return schedule
		return timesSet;
	}


	/**
	 * Create a schedule of dates comprised by sub-schedules for each startDate/cycle pair
	 * <p>
	 * This method expects {@code startDates} and {@code cycles} to represent a set of 
	 * comma-separated dates [date1,date2,...] and cycles [cycle1,cycle2,...], 
	 * respectively, of equal length. For each [date1/cycle1/date2], [date2/cycle2/date3], 
	 * etc. -pair, a schedule is created. {@code endDate} is used as the end date of the
	 * schedule based on the very last [dateXX/cycleXX]-pair. The overall set of dates is
	 * returned as the schedule.  
	 * 
	 * @param startTimes an array of start times
	 * @param endTime the end time of the schedule
	 * @param cycles an array of cycles
	 * @param endOfMonthConvention the convention to be applied
	 * @return an unordered set of schedule times
	 */
	public static Set<LocalDateTime> createArraySchedule(LocalDateTime[] startTimes,
			LocalDateTime endTime, String[] cycles, EndOfMonthConventionEnum endOfMonthConvention) {
        Set<LocalDateTime> timesSet = new HashSet<LocalDateTime>();
        
        // add schedules 1 to N-1
		for (int i = 0; i < startTimes.length - 1; i++) {
		    timesSet.addAll(
					createSchedule(startTimes[i], startTimes[i + 1], (cycles==null)? null : cycles[i], endOfMonthConvention));
		}
		
		// add last schedule
		timesSet.addAll(
				createSchedule(startTimes[startTimes.length - 1], endTime,
						(cycles==null)? null : cycles[startTimes.length - 1], endOfMonthConvention));
		
		// return schedule
		return timesSet;
	}
}
