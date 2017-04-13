package net.ackx.tradr

import groovy.util.logging.Slf4j

import static net.ackx.tradr.TradingAction.*

/**
 * Trader makes a trading decision based on {@link Data}, according to given {@link TraderParams parameters}.
 *
 * @author @YouriAckx
 */
@Slf4j
class Trader {
    private double damper
    private MacdParams macdParams

    /**
     * Create a new instance
     * @param params Trading parameters
     */
    Trader(TraderParams params) {
        this.macdParams = params.macd
        this.damper = params.damper
    }

    /**
     * Make a trading decision based on {@code data}.
     *
     * @param data Data
     * @return A {@link TradingAction trading action}
     */
    TradingAction trade(List<Data> data) {
        def isPositive = { i -> i > 0 }

        log.trace("Trading based on ${data.size()} stock price")

        if (data.size() < macdParams.b + macdParams.c) {
            return NO_DATA
        }

        // Extract closing prices
        def quotes = data.closing as List<Double>

        // EMA 12 and EMA 26 lines
        def emaA = Ema.ema(quotes, macdParams.a)
        def emaB = Ema.ema(quotes, macdParams.b)
        assert emaA.size() == emaB.size() + macdParams.b - macdParams.a

        // MACD line
        def macd = [] as List<Double>
        for (int i = 0; i < emaB.size(); i++) {
            macd << emaA[i + macdParams.b - macdParams.a] - emaB[i]
        }
        assert macd.size() == emaB.size()

        // Signal line
        def signal = Ema.ema(macd, macdParams.c)
        assert signal.size() == macd.size() - macdParams.c + 1

        // Histogram
        def histo = [] as List<Double>
        for (int i = 0; i < signal.size(); i++) {
            histo << macd[i + macdParams.c - 1] - signal[i]
        }

        // From current and running backward, find the last time the signal sign changed
        int histoChangedIndex = -1
        for (int i = histo.size() - 1; i > 0; i--) {
            if (isPositive(histo[i]) != isPositive(histo[i-1])) {
                histoChangedIndex = i
                break
            }
        }
        if (histoChangedIndex == -1) {
            return WAIT
        }

        // From the last sign change, find the first value greater than the damper,
        // with the right sign
        def becamePositive = isPositive(histo[histoChangedIndex])
        int firstAboveDamperIndex = -1
        for (int i = histoChangedIndex; i < histo.size(); i++) {
            if (Math.abs(histo[i]) >= damper && isPositive(histo[i]) == becamePositive) {
                firstAboveDamperIndex = i
                break
            }
        }
        if (firstAboveDamperIndex == -1 || firstAboveDamperIndex != histo.size() - 1) {
            return WAIT
        }

        // We found a value above damper, and its the current one
        return becamePositive ? BUY : SELL
    }
}
