package com.anotherchrisberry.spock.extensions.retry

import spock.lang.Specification

 @RetryOnFailure
class RetryOnFailureWithSystemPropertySpec extends Specification {

    static Integer defaultLevelTries = 0
    static Integer methodLevelTries = 0
    static Integer thrownTries = 0

    static {
        System.setProperty("spock-retry.times", "5")
    }

    void 'default level test'() {
        when:
        if (defaultLevelTries < 5) {
            defaultLevelTries++
            throw new RuntimeException("have not tried enough times ($defaultLevelTries)")
        }

        then:
        defaultLevelTries == 5
    }

    @RetryOnFailure(times=2)
    void 'method level test'() {
        when:
        if (methodLevelTries < 2) {
            methodLevelTries++
            throw new RuntimeException("have not tried enough times ($methodLevelTries)")
        }

        then:
        methodLevelTries == 2
    }

    @RetryOnFailure(times=7)
    void 'expect thrown is okay'() {
        when:
        thrownTries++
        throw new RuntimeException('no problem')

        then:
        thrown(RuntimeException)
        thrownTries == 1
    }

}
