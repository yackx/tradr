package net.ackx.tradr

/**
 * Aggregate data in smaller chunks.
 *
 * @author @YouriAckx
 */
class Aggregator {

    /**
     * Group a list of {@code data} according to a given {@code factor}.
     * <p/>
     * This methods allows you to transform a fine-grained list of data
     * (for instance, containing one quote per minute) into a coarse-grained list
     * (for instance, with one quote per five minute, or per hour).
     * </p>
     * When grouping, this method recompute new {@link Data#low low} and {@link Data#high high}
     * for the aggregated. The {@link Data#opening opening} and the {@link Data#dateTime dateTime}
     * will be taken from the start of the group,
     * and conversely the {@link Data#closing closing} will be taken from the end of the group.
     * </p>
     * The last frames may be dropped to have aggregates based on the same amount of data.
     * </p>
     * For example, given the following {@link Data}:
     * </p>
     * <pre>
     *     dateTime          open    close   low    high
     *     2017-03-01T12:34  10.00   11.00   9.50   11.50
     *     2017-03-01T12:35  11.00   11.50   9.55   12.10
     *     2017-03-01T12:36  11.50   10.22   8.52   12.52
     *     2017-03-01T12:37  10.20   10.10   7.50   14.50
     *     2017-03-01T12:38  10.10   11.00   9.50   11.50
     * </pre>
     *
     * Invoking:
     * <pre>
     *     def aggregated = Aggregator.aggregate(data, 2)
     *     assert aggregated.size() == 2
     * </pre>
     * The {@code aggregated} size is {@code 2} as
     * {@code data[0]} and {@code data[1]} have been aggregated to {@code aggregated[0]}, while
     * {@code data[1]} and {@code data[2]} have been aggregated to {@code aggregated[2]}.
     * The last original frame {@code data[4]} was dropped to keep a constant aggregation factor of {@code 2}.
     *
     * @param data Data to aggregate
     * @param factor Aggregation factor
     *
     * @return Aggregated data
     */
    static List<Data> aggregate(List<Data> data, int factor) {
        def aggregated = [] as List<Data>
        def size = data.size()
        def upTo = size - size % factor

        def startAggregatedSerie = 0
        def high = data[0].high
        def low = data[0].low

        for (int i = 0; i <= upTo; i++) {
            if (i > 0 && i % factor == 0) {
                if (i > 0) {
                    aggregated << new Data(dateTime: data[startAggregatedSerie].dateTime,
                            low: low, high: high,
                            opening: data[startAggregatedSerie].opening,
                            closing: data[startAggregatedSerie + factor - 1].closing)
                }
                if (i == upTo) break
                startAggregatedSerie = i
                high = data[i].high
                low = data[i].low
            }
            low = low < data[i].low ? low : data[i].low
            high = high > data[i].high ? high : data[i].high
        }

        aggregated
    }
}
