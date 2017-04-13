package net.ackx.tradr

import spock.lang.Specification
import spock.lang.Unroll

import static net.ackx.tradr.TradingAction.*

class TraderSpec extends Specification {

    @Unroll
    void "trade index=#index d=#damper -> #action"() {
        when:
        def trainingData = Samples.SAMPLE_1_TD

        and:
        def shortenedSerie = trainingData.subList(0, index + 1)

        then:
        def trader = new Trader(new TraderParams(macd: new MacdParams(a: 12, b: 26, c: 9), damper: damper))
        trader.trade(shortenedSerie) == action

        where:
        index   | damper    | action
        0       | 0.05d     | NO_DATA
        33      | 0.05d     | NO_DATA
        34      | 0.05d     | WAIT
        35      | 0.05d     | WAIT
        47      | 0.05d     | WAIT
        48      | 0.05d     | BUY
        60      | 0.05d     | WAIT
        61      | 0.05d     | SELL
        48      | 1.5d      | WAIT      // dampened
        49      | 1.5d      | BUY       // next after damper
        52      | 5d        | BUY       // next after severe damper
        61      | 1d        | WAIT      // dampened
        61      | 0.1d      | SELL      // not enough dampened
        49      | 1.5d      | BUY       // next after damper
    }
}
