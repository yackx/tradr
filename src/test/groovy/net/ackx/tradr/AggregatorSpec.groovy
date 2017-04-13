package net.ackx.tradr

import spock.lang.Specification

import static com.adessa.unbox.LocalDateHelper.toLocalDateTime

class AggregatorSpec extends Specification {

    void "aggregate"() {
        given:
        def json = [
                [dt: '2017-03-01T12:34', o: '10.00', c: '11.00', l: '9.50', h: '11.50'],
                [dt: '2017-03-01T12:35', o: '11.00', c: '11.50', l: '9.55', h: '12.10'],
                [dt: '2017-03-01T12:36', o: '11.50', c: '10.22', l: '8.52', h: '12.52'],
                [dt: '2017-03-01T12:37', o: '10.20', c: '10.10', l: '7.50', h: '14.50'],
                [dt: '2017-03-01T12:38', o: '10.10', c: '11.00', l: '9.50', h: '11.50'],
        ]
        def data = json.collect { new Data(dateTime: toLocalDateTime(it.dt),
                opening: it.o as BigDecimal, closing: it.c as BigDecimal,
                low: it.l as BigDecimal, high: it.h as BigDecimal)
        }

        when:
        def aggregated = Aggregator.aggregate(data, 2)

        then:
        aggregated.size() == 2

        and:
        aggregated[0].dateTime == toLocalDateTime('2017-03-01T12:34')
        aggregated[0].opening == '10.00' as BigDecimal
        aggregated[0].closing == '11.50' as BigDecimal
        aggregated[0].low == '9.50' as BigDecimal
        aggregated[0].high == '12.10' as BigDecimal

        and:
        aggregated[1].dateTime == toLocalDateTime('2017-03-01T12:36')
        aggregated[1].opening == '11.50' as BigDecimal
        aggregated[1].closing == '10.10' as BigDecimal
        aggregated[1].low == '7.50' as BigDecimal
        aggregated[1].high == '14.50' as BigDecimal
    }
}
