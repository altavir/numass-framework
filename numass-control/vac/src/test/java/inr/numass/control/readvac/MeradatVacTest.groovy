package inr.numass.control.readvac

import spock.lang.Specification

/**
 * Created by darksnake on 22-May-17.
 */
class MeradatVacTest extends Specification {
    def "CalculateLRC"() {
        given:
        def str = "020300000002";
        when:
        def lrc = MeradatVacDevice.calculateLRC(str).toUpperCase();
        then:
        lrc == "F9"
    }
}
