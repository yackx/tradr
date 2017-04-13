package net.ackx.tradr

import com.adessa.unbox.LocalDateHelper

import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * An operation made on a {@link Wallet}
 *
 * @author @YouriAckx
 */
class Operation {
    LocalDateTime dateTime
    BigDecimal stockGross
    double commissionPercentage
    BigDecimal converstionRate

    /**
     * Instantiate an operation
     * @param dateTime Timestamp of the operation
     * @param stockGross Stock gross price (without the trading commission)
     * @param conversionRate Conversion rate
     * @param commissionPercentage Trading commission, expressed as a percentage
     */
    Operation(LocalDateTime dateTime, BigDecimal stockGross, BigDecimal conversionRate, double commissionPercentage) {
        this.dateTime = dateTime
        this.stockGross = stockGross.setScale(2, RoundingMode.HALF_UP)
        this.converstionRate = conversionRate.setScale(4, RoundingMode.HALF_UP)
        this.commissionPercentage = commissionPercentage
    }

    @Override
    String toString() {
        def dt = LocalDateHelper.fromLocalDateTime(dateTime)
        "$dt XYZ $stockGross @ $converstionRate EUR/XYZ"
    }
}
