package net.ackx.tradr

import java.time.LocalDateTime

/**
 * Stock info (prices and volumes) for a given time.
 *
 * @author @YouriAckx
 */
class Data {
    LocalDateTime dateTime
    BigDecimal opening
    BigDecimal closing
    BigDecimal low
    BigDecimal high
    BigDecimal volumeFrom
    BigDecimal volumeTo

    @Override
    String toString() {
        "[$dateTime o=$opening, c=$closing, l=$low, h=$high, vf=$volumeFrom, vt=$volumeTo]"
    }
}
