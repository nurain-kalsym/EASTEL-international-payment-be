package com.kalsym.ekedai.utility;

import com.kalsym.ekedai.model.enums.Operator;

import java.util.Arrays;
import java.util.function.Predicate;

public class ConditionEvaluator {

    // Method for handling numeric comparisons
    public static boolean applyOperatorForNumber(Operator operator, String[] acceptedValues, double targetValue) {
        return Arrays.stream(acceptedValues).allMatch(value -> {
            try {
                double numericValue = Double.parseDouble(value);
                return compare(numericValue, targetValue, operator);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Non-numeric value provided for comparison operation", e);
            }
        });
    }

    // Method for handling string comparisons
    public static boolean applyOperatorForString(Operator operator, String[] acceptedValues, Predicate<String> matchCondition) {
        switch (operator) {
            case OR:
                return Arrays.stream(acceptedValues).anyMatch(matchCondition);
            case AND:
            case EQUAL:
                return Arrays.stream(acceptedValues).allMatch(matchCondition);
            case NOT_EQUAL:
                return Arrays.stream(acceptedValues).noneMatch(matchCondition);
            default:
                throw new IllegalArgumentException("Unsupported operator for string comparison: " + operator);
        }
    }

    // Helper method for numeric comparisons
    private static boolean compare(double numericValue, double targetValue, Operator operator) {
        switch (operator) {
            case LESS_EQUAL:
                return targetValue <= numericValue;
            case LESS:
                return targetValue < numericValue;
            case GREATER_EQUAL:
                return targetValue >= numericValue;
            case GREATER:
                return targetValue > numericValue;
            case EQUAL:
                return targetValue == numericValue;
            case NOT_EQUAL:
                return targetValue != numericValue;
            default:
                throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
        }
    }
}

