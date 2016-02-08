package com.anotherchrisberry.spock.extensions.retry

import spock.lang.Specification

@RetryOnFailure(times=0, beforeRetryMethod = 'beforeRetryMethod_Class')
class RetryOnFailureBeforeRetryMethodClass extends Specification{

    static Integer classLevelTries = 0
    static Integer onRetryClassLevel = 0
    static Integer methodLevelTries = 0
    static Integer onRetryMethodLevel = 0

    @RetryOnFailure(times=3)
    void 'expect beforeRetryMethod is called once'() {

        when:
        if (classLevelTries < 1) {
            classLevelTries ++
            throw new RuntimeException("have not tried enough times ($classLevelTries)")
        }

        then:
        onRetryClassLevel == 1
    }

    void beforeRetryMethod_Class(){
        onRetryClassLevel ++
    }

    @RetryOnFailure(times=3 , beforeRetryMethod = 'beforeRetryMethod_Method')
    void 'expect beforeRetryMethod is called twice'() {

        when:
        if (methodLevelTries < 2) {
            methodLevelTries ++
            throw new RuntimeException("have not tried enough times ($methodLevelTries)")
        }

        then:
        onRetryMethodLevel == 2
    }

    void beforeRetryMethod_Method(){
        onRetryMethodLevel ++
    }
}
