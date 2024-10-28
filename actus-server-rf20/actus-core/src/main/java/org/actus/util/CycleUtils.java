/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.util;

import org.actus.AttributeConversionException;

import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.DayOfWeek;
import static java.util.Locale.forLanguageTag;

/**
 * Utilities for handling ACTUS cycles
 * <p>
 * 
 */
public final class CycleUtils {
    
    /**
     * A period-based cycle starts with character 'P'
     */
    public static boolean isPeriod(String cycle) {
        return cycle.charAt(0)=='P';
    }

    /**
     * See next method
     */
    public static Period parsePeriod(String cycle, boolean stub) {
        // parse and return period
        return parsePeriod(cycle);
    }
    
    /**
     * Period is the character sequence starting with 'P' and ending before the 'L'-character of the stub
     */
    public static Period parsePeriod(String cycle) {
        Period period;
        // parse period from cycle
        try {
            period = Period.parse(cycle.split("L")[0]);
        } catch (Exception e) {
          throw(new AttributeConversionException());
        }
        // return period
        return period;
    }
    
    /*
     * Position is the integer at the position of the first character in the cycle
     */
    public static int parsePosition(String cycle) {
        int position;
        
        // parse position from cycle
        try {
          position = Integer.parseInt(""+cycle.charAt(0));
        } catch (Exception e) {
          throw(new AttributeConversionException());
        }
        // return weekday position
        return position; 
    }

    /*
     * Weekday is the character sequence following a single integer (first character) and up to 
     * the stub information (starting with an 'L'-character)
     */
    public static DayOfWeek parseWeekday(String cycle) {
        DayOfWeek weekday;
        // parse weekday from cycle
        try {
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E", forLanguageTag("en"));
          TemporalAccessor accessor = formatter.parse(cycle.split("L")[0].substring(1));
          weekday= DayOfWeek.from(accessor);
        } catch (Exception e) {
          throw(new AttributeConversionException());
        }
        // return weekday
        return weekday; 
    }

    /**
     * Stub is the character sequence following an 'L' in a cycle
     */
    public static char parseStub(String cycle) throws AttributeConversionException {
        char stub;
        try {
          stub = cycle.split("L")[1].charAt(0);
          if(!(stub==StringUtils.LongStub || stub==StringUtils.ShortStub)) throw(new AttributeConversionException());
        } catch (Exception e) {
          throw(new AttributeConversionException());
        }
        // return stub
        return stub;
    }
    
}
