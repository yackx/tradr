package net.ackx.tradr

import groovy.transform.Immutable
import groovy.transform.ToString

@Immutable
@ToString
class TraderParams {
    /** MACD parameters */
    MacdParams macd

    /**
     * Damper.
     * </p>
     * In order to avoid unwanted operations whenever the lines slightly cross,
     * the signal and MACD lines should be greater than that value to take a
     * {@link TradingAction#BUY buying} or {@link TradingAction#SELL selling} decision
     */
    double damper
}
