package net.ackx.tradr

/**
 * Exponential Moving Average calculator
 *
 * @author @YouriAckx
 */
class Ema {

    /**
     * Compute EMA
     * @param serie Series of data
     * @param periods EMA periods
     * @return
     */
    static List<Double> ema(List<Double> serie, int periods) {
        def size = serie.size()
        if (size < periods) {
            throw new IllegalArgumentException("serie must have at least an effective of $periods")
        }

        // Multiplier
        double multiplier = 2.0d / (periods + 1) as double

        def emas = [] as List<Double>

        // Oldest effective is the average of the previous.
        // Not all algorithms work that way; some only take the "old" value
        // and don't perform average.
        double avg = serie.subList(0, periods).sum() / periods
        emas << avg

        for (int i = periods; i < size; i++) {
            int index = i - periods
            emas[index + 1] = multiplier * serie[i] + (1d - multiplier) * emas[index]
        }

        emas
    }
}
