package net.ackx.tradr

import groovy.transform.Immutable

/**
 * Parameters for MACD calculation
 *
 * @author @YouriAckx
 */
@Immutable
class MacdParams {
    int a, b, c

    @Override
    String toString() {
        "MACD($a, $b, $c)"
    }
}
