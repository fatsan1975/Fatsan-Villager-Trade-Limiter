package com.fatsan.villagertrade.util;

import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;

public final class DiscountMath {

    private static final int MAJOR_NEGATIVE_WEIGHT = -5;
    private static final int MINOR_NEGATIVE_WEIGHT = -1;
    private static final int MINOR_POSITIVE_WEIGHT = 1;
    private static final int MAJOR_POSITIVE_WEIGHT = 5;
    private static final int TRADING_WEIGHT = 1;

    private DiscountMath() {}

    public static int weightedNegative(Reputation reputation) {
        return (value(reputation, ReputationType.MAJOR_NEGATIVE) * MAJOR_NEGATIVE_WEIGHT)
                + (value(reputation, ReputationType.MINOR_NEGATIVE) * MINOR_NEGATIVE_WEIGHT);
    }

    public static int weightedTrading(Reputation reputation) {
        return value(reputation, ReputationType.TRADING) * TRADING_WEIGHT;
    }

    public static int weightedCure(Reputation reputation) {
        return (value(reputation, ReputationType.MAJOR_POSITIVE) * MAJOR_POSITIVE_WEIGHT)
                + (value(reputation, ReputationType.MINOR_POSITIVE) * MINOR_POSITIVE_WEIGHT);
    }

    private static int value(Reputation reputation, ReputationType type) {
        return reputation == null ? 0 : reputation.getReputation(type);
    }
}
