/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.attributes;

import org.actus.contracts.ContractType;
import org.actus.time.calendar.BusinessDayCalendarProvider;
import org.actus.time.calendar.NoHolidaysCalendar;
import org.actus.time.calendar.MondayToFridayCalendar;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.AttributeConversionException;
import org.actus.ContractTypeUnknownException;
import org.actus.types.*;
import org.actus.util.CommonUtils;
import org.actus.types.ContractReference;


import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * A data structure representing the set of ACTUS contract attributes
 * <p>
 * This is a simple implementation of the {@link ContractModelProvider} interface representing
 * a generic data structure for the various ACTUS attributes parametrizing a {@link ContractType}.
 * Method {@code parse} allows parsing the attributes from an input {@code String} representation
 * to the internal data types.
 * <p>
 * Note, an ACTUS {@link ContractType} can deal with any data structure implementing the
 * {@link ContractModelProvider} interface. Thus, depending on the system ACTUS is embedded in,
 * more efficient data structures and parsing methods are possible.
 *
 * @see <a href="https://www.actusfrf.org/data-dictionary">ACTUS Data Dictionary</a>
 */
public class ContractModel implements ContractModelProvider {
    private Map<String, Object> attributes;

    /**
     * Constructor
     * <p>
     * The map provided as the constructor argument is expected to contain <key,value> pairs
     * of attributes using ACTUS attribute names (in long form) and data types of values
     * as per official ACTUS data dictionary.
     */
    public ContractModel(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * Create a new contract model from a java map
     * <p>
     * The map provided as the method argument is expected to contain <key,value> pairs
     * of attributes using ACTUS attribute names (in long form) and data types of values
     * as per official ACTUS data dictionary.
     *
     * @param attributes a java map of attributes as per ACTUS data dictionary
     * @return an instance of ContractModel containing the attributes provided with the method argument
     */
    public static ContractModel of(Map<String, Object> attributes) {
        return new ContractModel(attributes);
    }


    /**
     * Parse the attributes from external String-representation to internal, attribute-specific data types
     * <p>
     * For the {@link ContractType} indicated in attribute "ContractType" the method goes through the list
     * of supported attributes and tries to parse these to their respective data type as indicated in the
     * ACTUS data dictionary ({@linktourl https://www.actusfrf.org/data-dictionary}).
     * <p>
     * For all attributes mandatory to a certain "ContractType" the method expects a not-{@code null} return value
     * of method {@code get} of the {@code Map<String,String>} method parameter. For non-mandatory attributes, a
     * {@code null} return value is allowed and treated as that the attribute is not specified. Some attributes may
     * be mandatory conditional to the value of other attributes. Be referred to the ACTUS data dictionary
     * for details.
     *
     * @param contractAttributes an external, raw (String) data representation of the set of attributes
     * @return an instance of ContractModel containing the attributes provided with the method argument
     * @throws AttributeConversionException if an attribute cannot be parsed to its data type
     */
    public static ContractModel parse(Map<String, Object> contractAttributes) {
        HashMap<String, Object> map = new HashMap<>();

        if(contractAttributes.get("contractStructure")!=null){
            Map<String, Object> attributes = contractAttributes;
            List<ContractReference> contractStructure = new ArrayList<>();
            // parse all attributes known to the respective contract type
            try {
                switch (ContractTypeEnum.valueOf((String)attributes.get("contractType"))) {

                    case SWAPS:
                        // parse attributes (Swap) attributes
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("Currency", attributes.get("currency"));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse((String)attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtTerminationDate")));
                        map.put("DeliverySettlement", DeliverySettlement.valueOf((String)attributes.get("deliverySettlement")));
                        map.put("ContractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        // parse child attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("ContractRole"))));
                        map.put("ContractStructure", contractStructure);

                        break;

                    case CAPFL:
                        // parse attributes (CapFloor) attributes
                        map.put("ContractType", ContractTypeEnum.CAPFL);
                        map.put("StatusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("Currency", attributes.get("currency"));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse((String)attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtTerminationDate")));
                        map.put("LifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble((String)attributes.get("lifeCap")));
                        map.put("LifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble((String)attributes.get("lifeFloor")));

                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("ContractRole"))));
                        map.put("ContractStructure", contractStructure);
                        break;

                    case OPTNS:
                        map.put("MaturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse((String)attributes.get("maturityDate"))));
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf((String)attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf((String)attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        map.put("StatusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("CreatorID", attributes.get("creatorID"));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        //TODO: Only needed for underlying MarketObjectCode values in Exercise-Date/Amount calc ?
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("ContractPerformance", (CommonUtils.isNull(attributes.get("contractPerformance")) ? ContractPerformance.PF : ContractPerformance.valueOf((String)attributes.get("contractPerformance"))));
                        map.put("Seniority", !CommonUtils.isNull(attributes.get("seniority")) ? Seniority.valueOf((String)attributes.get("seniority")): null);
                        map.put("NonPerformingDate", !CommonUtils.isNull(attributes.get("nonPerformingDates")) ? LocalDateTime.parse((String)attributes.get("nonPerformingDates")): null);
                        map.put("PrepaymentPeriod", attributes.get("prepaymentPeriod"));
                        map.put("GracePeriod", attributes.get("gracePeriod"));
                        map.put("DelinquencyPeriod", attributes.get("delinquencyPeriod"));
                        map.put("DelinquencyRate", !CommonUtils.isNull(attributes.get("delinquencyRate")) ? Double.parseDouble((String)attributes.get("delinquencyRate")) : 0.0);
                        map.put("GuaranteedExposure", (CommonUtils.isNull(attributes.get("guaranteedExposure")) ? null : GuaranteedExposure.valueOf((String)attributes.get("guaranteedExposure"))));
                        map.put("CoverageOfCreditEnhancement", (CommonUtils.isNull(attributes.get("coverageOfCreditEnhancement")) ? 1.0 : Double.parseDouble((String)attributes.get("coverageOfCreditEnhancement"))));
                        map.put("CreditEventTypeCovered", (CommonUtils.isNull(attributes.get("creditEventTypeCovered")) ? new ArrayList<CreditEventTypeCovered>().add(CreditEventTypeCovered.DF) : Arrays.stream(((String)attributes.get("creditEventTypeCovered")).replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(CreditEventTypeCovered::valueOf).toArray(CreditEventTypeCovered[]::new)));
                        map.put("CycleAnchorDateOfDividend", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfDividend")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfDividend"))));
                        map.put("CycleOfDividend", attributes.get("cycleOfDividend"));
                        map.put("NextDividendPaymentAmount", !CommonUtils.isNull(attributes.get("nextDividendPaymentAmount")) ? Double.parseDouble((String)attributes.get("nextDividendPaymentAmount")) : 0.0);
                        map.put("ExDividendDate", (CommonUtils.isNull(attributes.get("exDividendDate")) ? null : LocalDateTime.parse((String)attributes.get("exDividendDate"))));
                        map.put("CycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfFee"))));
                        map.put("CycleOfFee", attributes.get("cycleOfFee"));
                        map.put("FeeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf((String)attributes.get("feeBasis")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeRate")));
                        map.put("FeeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeAccrued")));
                        map.put("CycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse((String)attributes.get("initialExchangeDate"))) : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("ArrayCycleAnchorDateOfInterestPayment", attributes.get("arrayCycleAnchorDateOfInterestPayment"));
                        map.put("CycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("ArrayCycleOfInterestPayment", attributes.get("arrayCycleOfInterestPayment"));
                        map.put("NominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("nominalInterestRate")));
                        map.put("ExerciseAmount", !(CommonUtils.isNull(attributes.get("exerciseAmount"))) ? Double.parseDouble((String)attributes.get("exerciseAmount")): 0.0);
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("SettlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
                        map.put("ExerciseDate", (CommonUtils.isNull(attributes.get("exerciseDate")) ? null : LocalDateTime.parse((String)attributes.get("exerciseDate"))));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("OptionType", OptionType.valueOf((String)attributes.get("optionType")));
                        map.put("OptionStrike1", Double.parseDouble((String)attributes.get("optionStrike1")));
                        map.put("Currency", attributes.get("currency"));

                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("ContractRole"))));
                        map.put("ContractStructure", contractStructure);
                        break;
                        
                    case BCS:
                        map.put("MaturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse((String)attributes.get("maturityDate"))));
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf((String)attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf((String)attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        map.put("StatusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("CreatorID", attributes.get("creatorID"));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        //TODO: Only needed for underlying MarketObjectCode values in Exercise-Date/Amount calc ?
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("ContractPerformance", (CommonUtils.isNull(attributes.get("contractPerformance")) ? ContractPerformance.PF : ContractPerformance.valueOf((String)attributes.get("contractPerformance"))));
                        map.put("Seniority", !CommonUtils.isNull(attributes.get("seniority")) ? Seniority.valueOf((String)attributes.get("seniority")): null);
                        map.put("NonPerformingDate", !CommonUtils.isNull(attributes.get("nonPerformingDates")) ? LocalDateTime.parse((String)attributes.get("nonPerformingDates")): null);
                        map.put("PrepaymentPeriod", attributes.get("prepaymentPeriod"));
                        map.put("GracePeriod", attributes.get("gracePeriod"));
                        map.put("DelinquencyPeriod", attributes.get("delinquencyPeriod"));
                        map.put("DelinquencyRate", !CommonUtils.isNull(attributes.get("delinquencyRate")) ? Double.parseDouble((String)attributes.get("delinquencyRate")) : 0.0);
                        map.put("GuaranteedExposure", (CommonUtils.isNull(attributes.get("guaranteedExposure")) ? null : GuaranteedExposure.valueOf((String)attributes.get("guaranteedExposure"))));
                        map.put("CoverageOfCreditEnhancement", (CommonUtils.isNull(attributes.get("coverageOfCreditEnhancement")) ? 1.0 : Double.parseDouble((String)attributes.get("coverageOfCreditEnhancement"))));
                        map.put("CreditEventTypeCovered", (CommonUtils.isNull(attributes.get("creditEventTypeCovered")) ? new ArrayList<CreditEventTypeCovered>().add(CreditEventTypeCovered.DF) : Arrays.stream(((String)attributes.get("creditEventTypeCovered")).replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(CreditEventTypeCovered::valueOf).toArray(CreditEventTypeCovered[]::new)));
                        map.put("CycleAnchorDateOfDividend", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfDividend")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfDividend"))));
                        map.put("CycleOfDividend", attributes.get("cycleOfDividend"));
                        map.put("NextDividendPaymentAmount", !CommonUtils.isNull(attributes.get("nextDividendPaymentAmount")) ? Double.parseDouble((String)attributes.get("nextDividendPaymentAmount")) : 0.0);
                        map.put("ExDividendDate", (CommonUtils.isNull(attributes.get("exDividendDate")) ? null : LocalDateTime.parse((String)attributes.get("exDividendDate"))));
                        map.put("CycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfFee"))));
                        map.put("CycleOfFee", attributes.get("cycleOfFee"));
                        map.put("FeeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf((String)attributes.get("feeBasis")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeRate")));
                        map.put("FeeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeAccrued")));
                        map.put("CycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse((String)attributes.get("initialExchangeDate"))) : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("ArrayCycleAnchorDateOfInterestPayment", attributes.get("arrayCycleAnchorDateOfInterestPayment"));
                        map.put("CycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("ArrayCycleOfInterestPayment", attributes.get("arrayCycleOfInterestPayment"));
                        map.put("NominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("nominalInterestRate")));
                        map.put("ExerciseAmount", !(CommonUtils.isNull(attributes.get("exerciseAmount"))) ? Double.parseDouble((String)attributes.get("exerciseAmount")): 0.0);
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("SettlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
                        map.put("ExerciseDate", (CommonUtils.isNull(attributes.get("exerciseDate")) ? null : LocalDateTime.parse((String)attributes.get("exerciseDate"))));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("Currency", attributes.get("currency"));
                        map.put("BoundaryValue", (CommonUtils.isNull(attributes.get("boundaryValue"))) ? 0.0 : Double.parseDouble((String)attributes.get("boundaryValue")));
                        map.put("BoundaryDirection", attributes.get("boundaryDirection"));
                        map.put("BoundaryEffect", attributes.get("boundaryEffect"));
                        map.put("BoundaryLegInitiallyActive", (CommonUtils.isNull(attributes.get("boundaryLegInitiallyActive"))) ? null : ReferenceRole.valueOf((String)attributes.get("boundaryLegInitiallyActive")));
                        map.put("BoundaryMonitoringAnchorDate", (CommonUtils.isNull(attributes.get("boundaryMonitoringAnchorDate"))) ? map.get("PurchaseDate") : LocalDateTime.parse((String)attributes.get("boundaryMonitoringAnchorDate")));
                        map.put("BoundaryMonitoringEndDate", (CommonUtils.isNull(attributes.get("boundaryMonitoringEndDate"))) ? map.get("MaturityDate") : LocalDateTime.parse((String)attributes.get("boundaryMonitoringEndDate")));
                        map.put("BoundaryMonitoringCycle", attributes.get("boundaryMonitoringCycle"));
                        map.put("BoundaryCrossedFlag", attributes.get("boundaryCrossedFlag"));


                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("ContractRole"))));
                        map.put("ContractStructure", contractStructure);
                        break;
                        
                    case FUTUR:
                        map.put("MaturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse((String)attributes.get("maturityDate"))));
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf((String)attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf((String)attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        map.put("StatusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("CreatorID", attributes.get("creatorID"));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("ContractPerformance", (CommonUtils.isNull(attributes.get("contractPerformance")) ? ContractPerformance.PF : ContractPerformance.valueOf((String)attributes.get("contractPerformance"))));
                        map.put("Seniority", !CommonUtils.isNull(attributes.get("seniority")) ? Seniority.valueOf((String)attributes.get("seniority")): null);
                        map.put("NonPerformingDate", !CommonUtils.isNull(attributes.get("nonPerformingDates")) ? LocalDateTime.parse((String)attributes.get("nonPerformingDates")): null);
                        map.put("PrepaymentPeriod", attributes.get("prepaymentPeriod"));
                        map.put("GracePeriod", attributes.get("gracePeriod"));
                        map.put("DelinquencyPeriod", attributes.get("delinquencyPeriod"));
                        map.put("DelinquencyRate", !CommonUtils.isNull(attributes.get("delinquencyRate")) ? Double.parseDouble((String)attributes.get("delinquencyRate")) : 0.0);
                        map.put("GuaranteedExposure", (CommonUtils.isNull(attributes.get("guaranteedExposure")) ? null : GuaranteedExposure.valueOf((String)attributes.get("guaranteedExposure"))));
                        map.put("CoverageOfCreditEnhancement", (CommonUtils.isNull(attributes.get("coverageOfCreditEnhancement")) ? 1.0 : Double.parseDouble((String)attributes.get("coverageOfCreditEnhancement"))));
                        map.put("CreditEventTypeCovered", (CommonUtils.isNull(attributes.get("creditEventTypeCovered")) ? new ArrayList<CreditEventTypeCovered>().add(CreditEventTypeCovered.DF) : Arrays.stream(((String)attributes.get("creditEventTypeCovered")).replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(CreditEventTypeCovered::valueOf).toArray(CreditEventTypeCovered[]::new)));
                        map.put("CycleAnchorDateOfDividend", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfDividend")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfDividend"))));
                        map.put("CycleOfDividend", attributes.get("cycleOfDividend"));
                        map.put("NextDividendPaymentAmount", !CommonUtils.isNull(attributes.get("nextDividendPaymentAmount")) ? Double.parseDouble((String)attributes.get("nextDividendPaymentAmount")) : 0.0);
                        map.put("ExDividendDate", (CommonUtils.isNull(attributes.get("exDividendDate")) ? null : LocalDateTime.parse((String)attributes.get("exDividendDate"))));
                        map.put("CycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfFee"))));
                        map.put("CycleOfFee", attributes.get("cycleOfFee"));
                        map.put("FeeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf((String)attributes.get("feeBasis")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeRate")));
                        map.put("FeeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeAccrued")));
                        map.put("CycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse((String)attributes.get("initialExchangeDate"))) : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("ArrayCycleAnchorDateOfInterestPayment", attributes.get("arrayCycleAnchorDateOfInterestPayment"));
                        map.put("CycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("ArrayCycleOfInterestPayment", attributes.get("arrayCycleOfInterestPayment"));
                        map.put("NominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("nominalInterestRate")));
                        map.put("AccruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble((String)attributes.get("accruedInterest")));
                        map.put("FuturesPrice", Double.parseDouble((String)attributes.get("futuresPrice")));
                        map.put("ExerciseAmount", !(CommonUtils.isNull(attributes.get("exerciseAmount"))) ? Double.parseDouble((String)attributes.get("exerciseAmount")): 0.0);
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("SettlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
                        map.put("ExerciseDate", (CommonUtils.isNull(attributes.get("exerciseDate")) ? null : LocalDateTime.parse((String)attributes.get("exerciseDate"))));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("Currency", attributes.get("currency"));

                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("ContractRole"))));
                        map.put("ContractStructure", contractStructure);
                        break;
                    case CEG:
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf((String)attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf((String)attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        map.put("StatusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("CreatorID", attributes.get("creatorID"));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("ContractPerformance", (CommonUtils.isNull(attributes.get("contractPerformance")) ? ContractPerformance.PF : ContractPerformance.valueOf((String)attributes.get("contractPerformance"))));
                        map.put("NonPerformingDate", !CommonUtils.isNull(attributes.get("nonPerformingDates")) ? LocalDateTime.parse((String)attributes.get("nonPerformingDates")): null);
                        map.put("GracePeriod", attributes.get("gracePeriod"));
                        map.put("DelinquencyPeriod", attributes.get("delinquencyPeriod"));
                        map.put("DelinquencyRate", !CommonUtils.isNull(attributes.get("delinquencyRate")) ? Double.parseDouble((String)attributes.get("delinquencyRate")) : 0.0);
                        map.put("GuaranteedExposure", (CommonUtils.isNull(attributes.get("guaranteedExposure")) ? GuaranteedExposure.NO : GuaranteedExposure.valueOf((String)attributes.get("guaranteedExposure"))));
                        map.put("CoverageOfCreditEnhancement", (CommonUtils.isNull(attributes.get("coverageOfCreditEnhancement")) ? 1.0 : Double.parseDouble((String)attributes.get("coverageOfCreditEnhancement"))));
                        map.put("CreditEventTypeCovered", (CommonUtils.isNull(attributes.get("creditEventTypeCovered")) ? new ArrayList<CreditEventTypeCovered>().add(CreditEventTypeCovered.DF) : Arrays.stream(((String)attributes.get("creditEventTypeCovered")).replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(CreditEventTypeCovered::valueOf).toArray(CreditEventTypeCovered[]::new)));
                        map.put("CycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee")) ? null : LocalDateTime.parse((String)attributes.get("cycleAnchorDateOfFee"))));
                        map.put("CycleOfFee", attributes.get("cycleOfFee"));
                        map.put("FeeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf((String)attributes.get("feeBasis")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeRate")));
                        map.put("FeeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble((String)attributes.get("feeAccrued")));
                        map.put("DayCountConvention", (CommonUtils.isNull(attributes.get("dayCountConvention"))) ? null : new DayCountCalculator(attributes.get("dayCountConvention").toString(), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("Currency", attributes.get("currency"));
                        map.put("MaturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse((String)attributes.get("maturityDate"))));
                        map.put("NotionalPrincipal", CommonUtils.isNull(attributes.get("notionalPrincipal")) ? null : Double.parseDouble((String)attributes.get("notionalPrincipal")));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse((String)attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse((String)attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble((String)attributes.get("priceAtTerminationDate")));
                        map.put("ExerciseDate", (CommonUtils.isNull(attributes.get("exerciseDate")) ? null : LocalDateTime.parse((String)attributes.get("exerciseDate"))));
                        map.put("ExerciseAmount", !(CommonUtils.isNull(attributes.get("exerciseAmount"))) ? Double.parseDouble((String)attributes.get("exerciseAmount")): 0.0);
                        map.put("SettlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
                         
                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("ContractRole"))));
                        map.put("ContractStructure", contractStructure);
                        break;

                    case CEC:
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf((String)attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf((String)attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf((String)attributes.get("contractType")));
                        map.put("StatusDate", LocalDateTime.parse((String)attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf((String)attributes.get("contractRole")));
                        map.put("CreatorID", attributes.get("creatorID"));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("GuaranteedExposure", (CommonUtils.isNull(attributes.get("guaranteedExposure")) ? GuaranteedExposure.NO : GuaranteedExposure.valueOf((String)attributes.get("guaranteedExposure"))));
                        map.put("CoverageOfCreditEnhancement", (CommonUtils.isNull(attributes.get("coverageOfCreditEnhancement")) ? 1.0 : Double.parseDouble((String)attributes.get("coverageOfCreditEnhancement"))));
                        map.put("CreditEventTypeCovered", (CommonUtils.isNull(attributes.get("creditEventTypeCovered")) ? new ArrayList<CreditEventTypeCovered>().add(CreditEventTypeCovered.DF) : Arrays.stream(((String)attributes.get("creditEventTypeCovered")).replaceAll("\\[", "").replaceAll("]", "").trim().split(",")).map(CreditEventTypeCovered::valueOf).toArray(CreditEventTypeCovered[]::new)));
                        map.put("Currency", attributes.get("currency"));
                        map.put("ExerciseDate", (CommonUtils.isNull(attributes.get("exerciseDate")) ? null : LocalDateTime.parse((String)attributes.get("exerciseDate"))));
                        map.put("ExerciseAmount", !(CommonUtils.isNull(attributes.get("exerciseAmount"))) ? Double.parseDouble((String)attributes.get("exerciseAmount")): 0.0);
                        map.put("SettlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
                         
                        // parse underlying attributes
                        ((List<Map<String,Object>>)attributes.get("contractStructure")).forEach(e->contractStructure.add(new ContractReference(e, (ContractRole)map.get("ContractRole"))));
                        map.put("ContractStructure", contractStructure);
                        break;

                    default:
                        throw new ContractTypeUnknownException();
                }
            } catch (Exception e) {
                throw new AttributeConversionException();
            }
        } else{
            Map<String,String> attributes = contractAttributes.entrySet().stream().collect(Collectors.toMap(e->e.getKey(),e->e.getValue().toString()));
            // parse all attributes known to the respective contract type
            try {
                map.put("ContractID", attributes.get("contractID"));
                switch (ContractTypeEnum.valueOf(attributes.get("contractType"))) {
                    case PAM:
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", (!CommonUtils.isNull(attributes.get("contractRole"))) ? ContractRole.valueOf(attributes.get("contractRole")) : null);
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("CycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("CycleOfFee", attributes.get("cycleOfFee"));
                        map.put("FeeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("FeeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("CycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("CycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("NominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("DayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("AccruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("CapitalizationEndDate", (CommonUtils.isNull(attributes.get("capitalizationEndDate"))) ? null : LocalDateTime.parse(attributes.get("capitalizationEndDate")));
                        map.put("CyclePointOfInterestPayment", CommonUtils.isNull(attributes.get("cyclePointOfInterestPayment")) ? null : CyclePointOfInterestPayment.valueOf(attributes.get("cyclePointOfInterestPayment")));
                        map.put("Currency", attributes.get("currency"));
                        map.put("InitialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("PremiumDiscountAtIED", (CommonUtils.isNull(attributes.get("premiumDiscountAtIED"))) ? 0.0 : Double.parseDouble(attributes.get("premiumDiscountAtIED")));
                        map.put("NotionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("MarketObjectCodeOfScalingIndex", attributes.get("marketObjectCodeOfScalingIndex"));
                        map.put("ScalingIndexAtContractDealDate", (CommonUtils.isNull(attributes.get("scalingIndexAtContractDealDate"))) ? 0.0 : Double.parseDouble(attributes.get("scalingIndexAtContractDealDate")));
                        map.put("NotionalScalingMultiplier", (CommonUtils.isNull(attributes.get("notionalScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("notionalScalingMultiplier")));
                        map.put("InterestScalingMultiplier", (CommonUtils.isNull(attributes.get("interestScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("interestScalingMultiplier")));
                        map.put("CycleAnchorDateOfScalingIndex", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfScalingIndex"))) ? ((CommonUtils.isNull(attributes.get("cycleOfScalingIndex"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfScalingIndex")));
                        map.put("CycleOfScalingIndex", attributes.get("cycleOfScalingIndex"));
                        map.put("ScalingEffect", CommonUtils.isNull(attributes.get("scalingEffect")) ? ScalingEffect.OOO : ScalingEffect.valueOf(attributes.get("scalingEffect")));
                        // TODO: review prepayment mechanism and attributes
                        map.put("CycleAnchorDateOfOptionality", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfOptionality"))) ? ((CommonUtils.isNull(attributes.get("cycleOfOptionality"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfOptionality")));
                        map.put("CycleOfOptionality", attributes.get("cycleOfOptionality"));
                        map.put("PenaltyType", (CommonUtils.isNull(attributes.get("penaltyType"))) ? PenaltyType.valueOf("N") : PenaltyType.valueOf(attributes.get("penaltyType")));
                        map.put("PenaltyRate", (CommonUtils.isNull(attributes.get("penaltyRate"))) ? 0.0 : Double.parseDouble(attributes.get("penaltyRate")));
                        map.put("ObjectCodeOfPrepaymentModel", attributes.get("objectCodeOfPrepaymentModel"));
                        map.put("CycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("CycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("RateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("MarketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("LifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("LifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("PeriodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("PeriodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        map.put("CyclePointOfRateReset", CommonUtils.isNull(attributes.get("cyclePointOfRateReset")) ? null : CyclePointOfRateReset.valueOf(attributes.get("cyclePointOfRateReset")));
                        map.put("FixingPeriod", attributes.get("fixingPeriod"));
                        map.put("NextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("RateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
                        map.put("MaturityDate", LocalDateTime.parse(attributes.get("maturityDate")));

                        break; // nothing else to do for PAM
                    case LAM:
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("CycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("CycleOfFee", attributes.get("cycleOfFee"));
                        map.put("FeeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("FeeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("CycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("CycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("NominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("DayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("AccruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("CapitalizationEndDate", (CommonUtils.isNull(attributes.get("capitalizationEndDate"))) ? null : LocalDateTime.parse(attributes.get("capitalizationEndDate")));
                        map.put("CyclePointOfRateReset", CommonUtils.isNull(attributes.get("cyclePointOfRateReset")) ? null : map.get("CyclePointOfInterestPayment") == CyclePointOfInterestPayment.B ? CyclePointOfRateReset.E : CyclePointOfRateReset.valueOf(attributes.get("cyclePointOfRateReset")));
                        map.put("Currency", attributes.get("currency"));
                        map.put("InitialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("PremiumDiscountAtIED", (CommonUtils.isNull(attributes.get("premiumDiscountAtIED"))) ? 0.0 : Double.parseDouble(attributes.get("premiumDiscountAtIED")));
                        map.put("NotionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("MarketObjectCodeOfScalingIndex", attributes.get("marketObjectCodeOfScalingIndex"));
                        map.put("ScalingIndexAtContractDealDate", (CommonUtils.isNull(attributes.get("scalingIndexAtContractDealDate"))) ? 0.0 : Double.parseDouble(attributes.get("scalingIndexAtContractDealDate")));
                        map.put("NotionalScalingMultiplier", (CommonUtils.isNull(attributes.get("notionalScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("notionalScalingMultiplier")));
                        map.put("InterestScalingMultiplier", (CommonUtils.isNull(attributes.get("interestScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("interestScalingMultiplier")));
                        map.put("CycleAnchorDateOfScalingIndex", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfScalingIndex"))) ? ((CommonUtils.isNull(attributes.get("cycleOfScalingIndex"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfScalingIndex")));
                        map.put("CycleOfScalingIndex", attributes.get("cycleOfScalingIndex"));
                        map.put("ScalingEffect", CommonUtils.isNull(attributes.get("scalingEffect")) ? ScalingEffect.OOO : ScalingEffect.valueOf(attributes.get("scalingEffect")));
                        // TODO: review prepayment mechanism and attributes
                        map.put("CycleAnchorDateOfOptionality", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfOptionality"))) ? ((CommonUtils.isNull(attributes.get("cycleOfOptionality"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfOptionality")));
                        map.put("CycleOfOptionality", attributes.get("cycleOfOptionality"));
                        map.put("PenaltyType", (CommonUtils.isNull(attributes.get("penaltyType"))) ? PenaltyType.valueOf("N") : PenaltyType.valueOf(attributes.get("penaltyType")));
                        map.put("PenaltyRate", (CommonUtils.isNull(attributes.get("penaltyRate"))) ? 0.0 : Double.parseDouble(attributes.get("penaltyRate")));
                        map.put("ObjectCodeOfPrepaymentModel", attributes.get("objectCodeOfPrepaymentModel"));
                        map.put("CycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("CycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("RateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("MarketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("LifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("LifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("PeriodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("PeriodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        map.put("FixingPeriod", attributes.get("fixingPeriod"));
                        map.put("NextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("RateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
                        map.put("MaturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse(attributes.get("maturityDate"))));
                        map.put("CycleAnchorDateOfInterestCalculationBase", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestCalculationBase"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestCalculationBase"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestCalculationBase")));
                        map.put("CycleOfInterestCalculationBase", attributes.get("cycleOfInterestCalculationBase"));
                        map.put("InterestCalculationBase", CommonUtils.isNull(attributes.get("interestCalculationBase")) ? null : InterestCalculationBase.valueOf(attributes.get("interestCalculationBase")));
                        map.put("InterestCalculationBaseAmount", (CommonUtils.isNull(attributes.get("interestCalculationBaseAmount"))) ? 0.0 : Double.parseDouble(attributes.get("interestCalculationBaseAmount")));
                        map.put("CycleAnchorDateOfPrincipalRedemption", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfPrincipalRedemption"))) ? LocalDateTime.parse(attributes.get("initialExchangeDate")) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfPrincipalRedemption")));
                        map.put("CycleOfPrincipalRedemption", attributes.get("cycleOfPrincipalRedemption"));
                        map.put("NextPrincipalRedemptionPayment", (CommonUtils.isNull(attributes.get("nextPrincipalRedemptionPayment"))) ? null : Double.parseDouble(attributes.get("nextPrincipalRedemptionPayment")));

                    case NAM:
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("CycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("CycleOfFee", attributes.get("cycleOfFee"));
                        map.put("FeeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("FeeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("CycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("CycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("NominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("DayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("AccruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("CapitalizationEndDate", (CommonUtils.isNull(attributes.get("capitalizationEndDate"))) ? null : LocalDateTime.parse(attributes.get("capitalizationEndDate")));
                        map.put("CyclePointOfRateReset", CommonUtils.isNull(attributes.get("cyclePointOfRateReset")) ? null : map.get("CyclePointOfInterestPayment") == CyclePointOfInterestPayment.B ? CyclePointOfRateReset.E : CyclePointOfRateReset.valueOf(attributes.get("cyclePointOfRateReset")));
                        map.put("Currency", attributes.get("currency"));
                        map.put("InitialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("PremiumDiscountAtIED", (CommonUtils.isNull(attributes.get("premiumDiscountAtIED"))) ? 0.0 : Double.parseDouble(attributes.get("premiumDiscountAtIED")));
                        map.put("NotionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("MarketObjectCodeOfScalingIndex", attributes.get("marketObjectCodeOfScalingIndex"));
                        map.put("ScalingIndexAtContractDealDate", (CommonUtils.isNull(attributes.get("scalingIndexAtContractDealDate"))) ? 0.0 : Double.parseDouble(attributes.get("scalingIndexAtContractDealDate")));
                        map.put("NotionalScalingMultiplier", (CommonUtils.isNull(attributes.get("notionalScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("notionalScalingMultiplier")));
                        map.put("InterestScalingMultiplier", (CommonUtils.isNull(attributes.get("interestScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("interestScalingMultiplier")));
                        map.put("CycleAnchorDateOfScalingIndex", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfScalingIndex"))) ? ((CommonUtils.isNull(attributes.get("cycleOfScalingIndex"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfScalingIndex")));
                        map.put("CycleOfScalingIndex", attributes.get("cycleOfScalingIndex"));
                        map.put("ScalingEffect", CommonUtils.isNull(attributes.get("scalingEffect")) ? ScalingEffect.OOO : ScalingEffect.valueOf(attributes.get("scalingEffect")));
                        // TODO: review prepayment mechanism and attributes
                        map.put("CycleAnchorDateOfOptionality", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfOptionality"))) ? ((CommonUtils.isNull(attributes.get("cycleOfOptionality"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfOptionality")));
                        map.put("CycleOfOptionality", attributes.get("cycleOfOptionality"));
                        map.put("PenaltyType", (CommonUtils.isNull(attributes.get("penaltyType"))) ? PenaltyType.valueOf("N") : PenaltyType.valueOf(attributes.get("penaltyType")));
                        map.put("PenaltyRate", (CommonUtils.isNull(attributes.get("penaltyRate"))) ? 0.0 : Double.parseDouble(attributes.get("penaltyRate")));
                        map.put("ObjectCodeOfPrepaymentModel", attributes.get("objectCodeOfPrepaymentModel"));
                        map.put("CycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("CycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("RateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("MarketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("LifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("LifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("PeriodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("PeriodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        map.put("FixingPeriod", attributes.get("fixingPeriod"));
                        map.put("NextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("RateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));

                        // present for LAM, NAM, ANN but not PAM
                        map.put("MaturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse(attributes.get("maturityDate"))));
                        map.put("CycleAnchorDateOfInterestCalculationBase", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestCalculationBase"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestCalculationBase"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestCalculationBase")));
                        map.put("CycleOfInterestCalculationBase", attributes.get("cycleOfInterestCalculationBase"));
                        map.put("InterestCalculationBase", CommonUtils.isNull(attributes.get("interestCalculationBase")) ? InterestCalculationBase.NT : InterestCalculationBase.valueOf(attributes.get("interestCalculationBase")));
                        map.put("InterestCalculationBaseAmount", (CommonUtils.isNull(attributes.get("interestCalculationBaseAmount"))) ? 0.0 : Double.parseDouble(attributes.get("interestCalculationBaseAmount")));
                        map.put("CycleAnchorDateOfPrincipalRedemption", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfPrincipalRedemption"))) ? LocalDateTime.parse(attributes.get("initialExchangeDate")) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfPrincipalRedemption")));
                        map.put("CycleOfPrincipalRedemption", attributes.get("cycleOfPrincipalRedemption"));
                        map.put("NextPrincipalRedemptionPayment", (CommonUtils.isNull(attributes.get("nextPrincipalRedemptionPayment"))) ? null : Double.parseDouble(attributes.get("nextPrincipalRedemptionPayment")));

                    case ANN: // almost identical with LAM, NAM, ANN
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("CycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("CycleOfFee", attributes.get("cycleOfFee"));
                        map.put("FeeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("FeeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("CycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("CycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("NominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("DayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("AccruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("CapitalizationEndDate", (CommonUtils.isNull(attributes.get("capitalizationEndDate"))) ? null : LocalDateTime.parse(attributes.get("capitalizationEndDate")));
                        map.put("CyclePointOfRateReset", CommonUtils.isNull(attributes.get("cyclePointOfRateReset")) ? null : map.get("CyclePointOfInterestPayment") == CyclePointOfInterestPayment.B ? CyclePointOfRateReset.E : CyclePointOfRateReset.valueOf(attributes.get("cyclePointOfRateReset")));
                        map.put("Currency", attributes.get("currency"));
                        map.put("InitialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("PremiumDiscountAtIED", (CommonUtils.isNull(attributes.get("premiumDiscountAtIED"))) ? 0.0 : Double.parseDouble(attributes.get("premiumDiscountAtIED")));
                        map.put("NotionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("MarketObjectCodeOfScalingIndex", attributes.get("marketObjectCodeOfScalingIndex"));
                        map.put("ScalingIndexAtContractDealDate", (CommonUtils.isNull(attributes.get("scalingIndexAtContractDealDate"))) ? 0.0 : Double.parseDouble(attributes.get("scalingIndexAtContractDealDate")));
                        map.put("NotionalScalingMultiplier", (CommonUtils.isNull(attributes.get("notionalScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("notionalScalingMultiplier")));
                        map.put("InterestScalingMultiplier", (CommonUtils.isNull(attributes.get("interestScalingMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("interestScalingMultiplier")));
                        map.put("CycleAnchorDateOfScalingIndex", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfScalingIndex"))) ? ((CommonUtils.isNull(attributes.get("cycleOfScalingIndex"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfScalingIndex")));
                        map.put("CycleOfScalingIndex", attributes.get("cycleOfScalingIndex"));
                        map.put("ScalingEffect", CommonUtils.isNull(attributes.get("scalingEffect")) ? ScalingEffect.OOO : ScalingEffect.valueOf(attributes.get("scalingEffect")));
                        // TODO: review prepayment mechanism and attributes
                        map.put("CycleAnchorDateOfOptionality", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfOptionality"))) ? ((CommonUtils.isNull(attributes.get("cycleOfOptionality"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfOptionality")));
                        map.put("CycleOfOptionality", attributes.get("cycleOfOptionality"));
                        map.put("PenaltyType", (CommonUtils.isNull(attributes.get("penaltyType"))) ? PenaltyType.valueOf("N") : PenaltyType.valueOf(attributes.get("penaltyType")));
                        map.put("PenaltyRate", (CommonUtils.isNull(attributes.get("penaltyRate"))) ? 0.0 : Double.parseDouble(attributes.get("penaltyRate")));
                        map.put("ObjectCodeOfPrepaymentModel", attributes.get("objectCodeOfPrepaymentModel"));
                        map.put("CycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("CycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("RateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("MarketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("LifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("LifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("PeriodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("PeriodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        map.put("FixingPeriod", attributes.get("fixingPeriod"));
                        map.put("NextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("RateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));

                        // present for LAM, NAM, ANN but not PAM
                        map.put("MaturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse(attributes.get("maturityDate"))));
                        map.put("CycleAnchorDateOfInterestCalculationBase", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestCalculationBase"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestCalculationBase"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestCalculationBase")));
                        map.put("CycleOfInterestCalculationBase", attributes.get("cycleOfInterestCalculationBase"));
                        map.put("InterestCalculationBase", CommonUtils.isNull(attributes.get("interestCalculationBase")) ? InterestCalculationBase.NT : InterestCalculationBase.valueOf(attributes.get("interestCalculationBase")));
                        map.put("InterestCalculationBaseAmount", (CommonUtils.isNull(attributes.get("interestCalculationBaseAmount"))) ? 0.0 : Double.parseDouble(attributes.get("interestCalculationBaseAmount")));
                        map.put("CycleAnchorDateOfPrincipalRedemption", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfPrincipalRedemption"))) ? LocalDateTime.parse(attributes.get("initialExchangeDate")) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfPrincipalRedemption")));
                        map.put("CycleOfPrincipalRedemption", attributes.get("cycleOfPrincipalRedemption"));
                        map.put("NextPrincipalRedemptionPayment", (CommonUtils.isNull(attributes.get("nextPrincipalRedemptionPayment"))) ? null : Double.parseDouble(attributes.get("nextPrincipalRedemptionPayment")));

                        // present for ANN but not for LAM, NAM
                        map.put("AmortizationDate", (CommonUtils.isNull(attributes.get("amortizationDate")) ? null : LocalDateTime.parse(attributes.get("amortizationDate"))));

                        break;
                    case CLM:
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("CycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("CycleOfFee", attributes.get("cycleOfFee"));
                        map.put("FeeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("FeeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("CycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("CycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("NominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("DayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("AccruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("Currency", attributes.get("currency"));
                        map.put("InitialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("NotionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("MaturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse(attributes.get("maturityDate"))));
                        map.put("XDayNotice", attributes.get("xDayNotice"));
                        map.put("CycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("CycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("RateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("MarketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("FixingPeriod", attributes.get("fixingPeriod"));
                        map.put("NextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("RateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
                        map.put("LifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("LifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("PeriodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("PeriodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        break;
                    case UMP:

                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("CycleAnchorDateOfFee", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfFee"))) ? ((CommonUtils.isNull(attributes.get("cycleOfFee"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfFee")));
                        map.put("CycleOfFee", attributes.get("cycleOfFee"));
                        map.put("FeeBasis", (CommonUtils.isNull(attributes.get("feeBasis"))) ? null : FeeBasis.valueOf(attributes.get("feeBasis")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("FeeAccrued", (CommonUtils.isNull(attributes.get("feeAccrued"))) ? 0.0 : Double.parseDouble(attributes.get("feeAccrued")));
                        map.put("CycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("CycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("NominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("DayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("AccruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("Currency", attributes.get("currency"));
                        map.put("InitialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("NotionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("XDayNotice", attributes.get("xDayNotice"));
                        map.put("MaximumPenaltyFreeDisbursement", (CommonUtils.isNull(attributes.get("maximumPenaltyFreeDisbursement"))) ? attributes.get("notionalPrincipal") : attributes.get("maximumPenaltyFreeDisbursement"));
                        map.put("CycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("CycleOfRateReset", attributes.get("cycleOfRateReset"));
                        map.put("RateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("MarketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("FixingPeriod", attributes.get("fixingPeriod"));
                        map.put("NextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("RateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
			map.put("LifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap"))) ;
			map.put("LifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor"))) ;
                        map.put("PeriodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap"))) ;
                     	map.put("PeriodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor"))) ;
                        break;
                    case CSH:

                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("Currency", attributes.get("currency"));
                        map.put("NotionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));

                        break;
                    case COM: // almost identical with STK

                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("Currency", attributes.get("currency"));
                        map.put("Quantity", (CommonUtils.isNull(attributes.get("quantity"))) ? 1 : Double.parseDouble(attributes.get("quantity")));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("MarketValueObserved", (CommonUtils.isNull(attributes.get("marketValueObserved"))) ? 0.0 : Double.parseDouble(attributes.get("marketValueObserved")));

                        break;
                    case STK:

                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("Currency", attributes.get("currency"));
                        map.put("Quantity", (CommonUtils.isNull(attributes.get("quantity"))) ? 1 : Double.parseDouble(attributes.get("quantity")));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("MarketValueObserved", (CommonUtils.isNull(attributes.get("marketValueObserved"))) ? 0.0 : Double.parseDouble(attributes.get("marketValueObserved")));

                        // present for STK but not COM
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("CycleAnchorDateOfDividendPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfDividendPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfDividendPayment"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfDividendPayment")));
                        map.put("CycleOfDividendPayment", attributes.get("cycleOfDividendPayment"));
                        map.put("MarketObjectCodeOfDividends", attributes.get("marketObjectCodeOfDividends"));

                        break;
                    case FXOUT:

                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("Currency", attributes.get("currency"));
                        map.put("Currency2", attributes.get("currency2"));
                        map.put("MaturityDate", LocalDateTime.parse(attributes.get("maturityDate")));
                        map.put("NotionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("NotionalPrincipal2", Double.parseDouble(attributes.get("notionalPrincipal2")));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("DeliverySettlement", CommonUtils.isNull(attributes.get("deliverySettlement")) ? null : DeliverySettlement.valueOf(attributes.get("deliverySettlement")));
                        map.put("SettlementPeriod", (CommonUtils.isNull(attributes.get("settlementPeriod"))) ? "P0D" : attributes.get("settlementPeriod"));
    
                        break;
                    case SWPPV:
                        map.put("AccruedInterest", (CommonUtils.isNull(attributes.get("accruedInterest"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest")));
                        map.put("AccruedInterest2", (CommonUtils.isNull(attributes.get("accruedInterest2"))) ? 0.0 : Double.parseDouble(attributes.get("accruedInterest2")));
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("CreatorID", attributes.get("creatorID"));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("CycleAnchorDateOfInterestPayment", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestPayment"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestPayment"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestPayment")));
                        map.put("CycleOfInterestPayment", attributes.get("cycleOfInterestPayment"));
                        map.put("NominalInterestRate", Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("NominalInterestRate2", Double.parseDouble(attributes.get("nominalInterestRate2")));
                        map.put("DayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("Currency", attributes.get("currency"));
                        map.put("InitialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("MaturityDate", LocalDateTime.parse(attributes.get("maturityDate")));
                        map.put("NotionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("PurchaseDate", (CommonUtils.isNull(attributes.get("purchaseDate"))) ? null : LocalDateTime.parse(attributes.get("purchaseDate")));
                        map.put("PriceAtPurchaseDate", (CommonUtils.isNull(attributes.get("priceAtPurchaseDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtPurchaseDate")));
                        map.put("TerminationDate", (CommonUtils.isNull(attributes.get("terminationDate"))) ? null : LocalDateTime.parse(attributes.get("terminationDate")));
                        map.put("PriceAtTerminationDate", (CommonUtils.isNull(attributes.get("priceAtTerminationDate"))) ? 0.0 : Double.parseDouble(attributes.get("priceAtTerminationDate")));
                        map.put("CycleAnchorDateOfRateReset", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfRateReset"))) ? ((CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfRateReset")));
                        map.put("CycleOfRateReset", (CommonUtils.isNull(attributes.get("cycleOfRateReset"))) ? null : attributes.get("cycleOfRateReset"));
                        map.put("RateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("MarketObjectCodeOfRateReset", (CommonUtils.isNull(attributes.get("marketObjectCodeOfRateReset"))) ? null :attributes.get("marketObjectCodeOfRateReset"));
                        map.put("CyclePointOfRateReset", CommonUtils.isNull(attributes.get("cyclePointOfRateReset")) ? null : map.get("CyclePointOfInterestPayment") == CyclePointOfInterestPayment.B ? CyclePointOfRateReset.E : CyclePointOfRateReset.valueOf(attributes.get("cyclePointOfRateReset")));
                        map.put("FixingPeriod", (CommonUtils.isNull(attributes.get("fixingPeriod"))) ? null : attributes.get("fixingPeriod"));
                        map.put("NextResetRate", (CommonUtils.isNull(attributes.get("nextResetRate"))) ? null : Double.parseDouble(attributes.get("nextResetRate")));
                        map.put("RateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
                        map.put("DeliverySettlement", CommonUtils.isNull(attributes.get("deliverySettlement")) ? null : DeliverySettlement.valueOf(attributes.get("deliverySettlement")));
                        break;

                    case LAX:
                        map.put("Calendar", (!CommonUtils.isNull(attributes.get("calendar")) && attributes.get("calendar").equals("MF")) ? new MondayToFridayCalendar() : new NoHolidaysCalendar());
                        map.put("BusinessDayConvention", new BusinessDayAdjuster(CommonUtils.isNull(attributes.get("businessDayConvention")) ? null : BusinessDayConventionEnum.valueOf(attributes.get("businessDayConvention")), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("ContractID", attributes.get("contractID"));
                        map.put("StatusDate", LocalDateTime.parse(attributes.get("statusDate")));
                        map.put("ContractRole", ContractRole.valueOf(attributes.get("contractRole")));
                        map.put("CounterpartyID", attributes.get("counterpartyID"));
                        map.put("MarketObjectCode", attributes.get("marketObjectCode"));
                        map.put("LegalEntityIDRecordCreator", attributes.get("legalEntityIDRecordCreator"));
                        map.put("Currency", attributes.get("currency"));
                        map.put("InitialExchangeDate", LocalDateTime.parse(attributes.get("initialExchangeDate")));
                        map.put("PremiumDiscountAtIED", (CommonUtils.isNull(attributes.get("premiumDiscountAtIED"))) ? 0.0 : Double.parseDouble(attributes.get("premiumDiscountAtIED")));
                        map.put("MaturityDate", (CommonUtils.isNull(attributes.get("maturityDate")) ? null : LocalDateTime.parse(attributes.get("maturityDate"))));
                        map.put("NotionalPrincipal", Double.parseDouble(attributes.get("notionalPrincipal")));
                        map.put("ArrayCycleAnchorDateOfPrincipalRedemption", attributes.get("arrayCycleAnchorDateOfPrincipalRedemption"));
                        map.put("ArrayCycleOfPrincipalRedemption", attributes.get("arrayCycleOfPrincipalRedemption"));
                        map.put("ArrayNextPrincipalRedemptionPayment", (CommonUtils.isNull(attributes.get("arrayNextPrincipalRedemptionPayment"))) ? 0 : attributes.get("arrayNextPrincipalRedemptionPayment"));
                        map.put("ArrayIncreaseDecrease", attributes.get("arrayIncreaseDecrease"));
                        map.put("ArrayCycleAnchorDateOfInterestPayment", attributes.get("arrayCycleAnchorDateOfInterestPayment"));
                        map.put("ArrayCycleOfInterestPayment", attributes.get("arrayCycleOfInterestPayment"));
                        map.put("NominalInterestRate", (CommonUtils.isNull(attributes.get("nominalInterestRate"))) ? 0.0 : Double.parseDouble(attributes.get("nominalInterestRate")));
                        map.put("DayCountConvention", new DayCountCalculator(attributes.get("dayCountConvention"), (BusinessDayCalendarProvider) map.get("Calendar")));
                        map.put("ArrayCycleAnchorDateOfRateReset", attributes.get("arrayCycleAnchorDateOfRateReset"));
                        map.put("ArrayCycleOfRateReset", attributes.get("arrayCycleOfRateReset"));
                        map.put("ArrayRate", attributes.get("arrayRate"));
                        map.put("ArrayFixedVariable", attributes.get("arrayFixedVariable"));
                        map.put("MarketObjectCodeOfRateReset", attributes.get("marketObjectCodeOfRateReset"));
                        map.put("ContractType", ContractTypeEnum.valueOf(attributes.get("contractType")));
                        map.put("FeeRate", (CommonUtils.isNull(attributes.get("feeRate"))) ? 0.0 : Double.parseDouble(attributes.get("feeRate")));
                        map.put("EndOfMonthConvention", (CommonUtils.isNull(attributes.get("endOfMonthConvention"))) ? EndOfMonthConventionEnum.SD : EndOfMonthConventionEnum.valueOf(attributes.get("endOfMonthConvention")));
                        map.put("RateMultiplier", (CommonUtils.isNull(attributes.get("rateMultiplier"))) ? 1.0 : Double.parseDouble(attributes.get("rateMultiplier")));
                        map.put("RateSpread", (CommonUtils.isNull(attributes.get("rateSpread"))) ? 0.0 : Double.parseDouble(attributes.get("rateSpread")));
                        map.put("PeriodCap", (CommonUtils.isNull(attributes.get("periodCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("periodCap")));
                        map.put("PeriodFloor", (CommonUtils.isNull(attributes.get("periodFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("periodFloor")));
                        map.put("LifeCap", (CommonUtils.isNull(attributes.get("lifeCap"))) ? Double.POSITIVE_INFINITY : Double.parseDouble(attributes.get("lifeCap")));
                        map.put("LifeFloor", (CommonUtils.isNull(attributes.get("lifeFloor"))) ? Double.NEGATIVE_INFINITY : Double.parseDouble(attributes.get("lifeFloor")));
                        map.put("CycleAnchorDateOfInterestCalculationBase", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfInterestCalculationBase"))) ? ((CommonUtils.isNull(attributes.get("cycleOfInterestCalculationBase"))) ? null : LocalDateTime.parse(attributes.get("initialExchangeDate"))) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfInterestCalculationBase")));
                        map.put("CycleOfInterestCalculationBase", attributes.get("cycleOfInterestCalculationBase"));
                        map.put("InterestCalculationBase", CommonUtils.isNull(attributes.get("interestCalculationBase")) ? InterestCalculationBase.NT : InterestCalculationBase.valueOf(attributes.get("interestCalculationBase")));
                        map.put("InterestCalculationBaseAmount", (CommonUtils.isNull(attributes.get("interestCalculationBaseAmount"))) ? 0.0 : Double.parseDouble(attributes.get("interestCalculationBaseAmount")));
                        map.put("CycleAnchorDateOfPrincipalRedemption", (CommonUtils.isNull(attributes.get("cycleAnchorDateOfPrincipalRedemption"))) ? LocalDateTime.parse(attributes.get("initialExchangeDate")) : LocalDateTime.parse(attributes.get("cycleAnchorDateOfPrincipalRedemption")));
                        break;
                    default:
                        throw new ContractTypeUnknownException();
                }
            } catch (Exception e) {
                throw new AttributeConversionException();
            }
        }
        return new ContractModel(map);
    }

    @Override
    public <T> T getAs(String name) {
        return (T) attributes.get(name);
    }

    public void addAttribute(String Key, Object value){
        attributes.put(Key,value);
    }
}
