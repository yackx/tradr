package net.ackx.tradr

import java.math.RoundingMode

/**
 * A wallet consists of an amount of euros, an amount of another currency or stock,
 * and a list of {@link Operation operations} that were performed on them.
 *
 * @author @YouriAckx
 */
class Wallet {
    private BigDecimal stock
    private BigDecimal euros
    private List<Operation> operations = []

    Wallet(BigDecimal stock, BigDecimal euros) {
        this.stock = stock.setScale(2, RoundingMode.HALF_UP)
        this.euros = euros.setScale(2, RoundingMode.HALF_UP)
    }

    static Wallet of(Wallet w) {
        new Wallet(w.stock, w.euros)
    }

    BigDecimal getStock() {
        return stock
    }

    BigDecimal getEuros() {
        return euros
    }

    List<Operation> getOperations() {
        return operations
    }

    void addOperation(Operation wo) {
        if (wo.stockGross > 0) {
            def eurEquivalent = -wo.stockGross * wo.converstionRate
            euros += eurEquivalent
            stock += wo.stockGross * (1 - wo.commissionPercentage)
        } else {
            def eurEquivalent = -wo.stockGross * wo.converstionRate * (1 - wo.commissionPercentage)
            euros += eurEquivalent
            stock += wo.stockGross
        }

        stock = stock.setScale(2, RoundingMode.HALF_UP)
        euros = euros.setScale(2, RoundingMode.HALF_UP)

        operations << wo
    }

    BigDecimal toEuros(BigDecimal conversionRate) {
        def eur = euros + stock * conversionRate
        eur.setScale(2, RoundingMode.HALF_UP)
    }

    @Override
    String toString() {
        "XYZ $stock EUR $euros (${operations.size()} operations)"
    }
}
