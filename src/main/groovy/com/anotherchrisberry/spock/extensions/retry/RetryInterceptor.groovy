package com.anotherchrisberry.spock.extensions.retry

import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.util.ReflectionUtil
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class RetryInterceptor implements IMethodInterceptor {

    static Logger LOG = LoggerFactory.getLogger(RetryInterceptor.class);

    Integer retryMax
    String beforeRetryMethod

    RetryInterceptor(RetryInterceptor parent ,int retryMax , String beforeRetryMethod) {
        // if default value use value from parent
        this.retryMax = retryMax == -1 ?  (parent?.retryMax ?: -1) : retryMax
        this.beforeRetryMethod = beforeRetryMethod == "" ?  (parent?.beforeRetryMethod ?: "") : beforeRetryMethod
    }


    void intercept(IMethodInvocation invocation) throws Throwable {
        Integer attempts = 0
        // if no default value at any point is defined for retry times, use one from system.properties
        Integer curRetryMax = retryMax == -1 ? Integer.parseInt(System.getProperty("spock-retry.times","1")) : retryMax
        while (attempts <= curRetryMax) {
            try {
                invocation.proceed()
                attempts = curRetryMax + 1
            } catch (Throwable t) {
                LOG.info("Retry caught failure ${attempts + 1} / ${curRetryMax + 1}: ", t)
                attempts++
                if (attempts > curRetryMax) {
                    throw t
                }
                invocation.spec.cleanupMethods.each {
                    try {
                        if (it.reflection) {
                            ReflectionUtil.invokeMethod(invocation.target, it.reflection)
                        }
                    } catch (Throwable t2) {
                        LOG.warn("Retry caught failure ${attempts + 1} / ${curRetryMax + 1} while cleaning up", t2)
                    }
                }
                invocation.spec.setupMethods.each {
                    try {
                        if (it.reflection) {
                            ReflectionUtil.invokeMethod(invocation.target, it.reflection)
                        }
                    } catch (Throwable t2) {
                        // increment counter, since this is the start of the re-run
                        attempts++
                        LOG.info("Retry caught failure ${attempts + 1} / ${curRetryMax + 1} while setting up", t2)
                    }
                }

                if(beforeRetryMethod != "") {
                    try {
                        invocation.target."$beforeRetryMethod"()
                    } catch (Throwable t2) {
                        // increment counter, since this is the start of the re-run
                        LOG.info("Invoking $beforeRetryMethod on $invocation.target failed", t2)
                    }
                }
            }
        }
    }
}
