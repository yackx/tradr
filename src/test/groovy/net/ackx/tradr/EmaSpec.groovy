package net.ackx.tradr

import spock.lang.Specification

class EmaSpec extends Specification {

    void "EMA"() {
        given:
        def data = Samples.SAMPLE_1
        assert data.size() == 66

        when:
        def ema12 = Ema.ema(data, 12)

        and: "offset in table (see below)"
        def offset12 = 11

        then:
        assert equal(ema12[11 - offset12], 440.8975d) : ema12
        assert equal(ema12[12 - offset12], 439.3101923077d) : ema12
        assert equal(ema12[50 - offset12], 422.1818445776d) : ema12
        assert equal(ema12[65 - offset12], 441.0857715833d) : ema12

        and: "offset in table (see below)"
        def offset26 = 25

        when:
        def ema26 = Ema.ema(data, 26)

        then:
        assert equal(ema26[25 - offset26], 443.2896153846d) : ema26
        assert equal(ema26[35 - offset26], 436.9735732278d) : ema26
        assert equal(ema26[50 - offset26], 423.9019929384d) : ema26
        assert equal(ema26[65 - offset26], 438.3232103674d) : ema26
    }

    private static boolean equal(double a, double b) {
        Math.abs(a - b) < 0.0001d
    }
}
