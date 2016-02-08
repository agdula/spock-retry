package com.anotherchrisberry.spock.extensions.retry

import org.spockframework.compiler.model.FeatureMethod
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.MethodInfo
import org.spockframework.runtime.model.SpecInfo

class RetrySpecExtension extends AbstractAnnotationDrivenExtension<RetryOnFailure> {

    void visitFeatureAnnotation(RetryOnFailure retries, FeatureInfo feature) {
        RetryInterceptor parent = clearInterceptors(feature)
        feature.getFeatureMethod().interceptors.add(new RetryInterceptor(parent, retries.times() ,retries.beforeRetryMethod()))
    }

    void visitSpecAnnotation(RetryOnFailure retries, SpecInfo spec) {

        SpecInfo specToAdd = spec
        spec.subSpec
        List<SpecInfo> selfAndSuperSpecs = [spec]
        List<SpecInfo> selfAndSubSpecs = [spec]
        while (specToAdd.getSuperSpec()) {
            selfAndSuperSpecs << specToAdd.getSuperSpec()
            specToAdd = specToAdd.getSuperSpec()
        }
        specToAdd = spec
        while (specToAdd.subSpec) {
            selfAndSubSpecs << specToAdd.subSpec
            specToAdd = specToAdd.subSpec
        }

        if (selfAndSuperSpecs.any { it.getReflection().isAnnotationPresent(RetryOnFailure.class)}) {
            List<FeatureInfo> featuresToRetry = [selfAndSubSpecs.features].flatten().unique()
            for (FeatureInfo feature : featuresToRetry) {
                RetryInterceptor parent = clearInterceptors(feature)
                addInterceptors(feature, parent,  retries)
            }
        }
    }

    private List<MethodInfo> getInterceptableMethods(FeatureInfo feature) {
        SpecInfo spec = feature.getSpec()
        [ spec.setupMethods,
          spec.setupSpecMethods,
          spec.cleanupMethods,
          spec.cleanupSpecMethods,
          feature.featureMethod
        ].flatten().unique() as List<MethodInfo>
    }

    private RetryInterceptor clearInterceptors(FeatureInfo featureInfo) {
        List<MethodInfo> interceptableMethods = getInterceptableMethods(featureInfo)
        List<RetryInterceptor> found = []
        interceptableMethods.each {
            found.addAll(it.interceptors.findAll { it.class == RetryInterceptor })
            it.interceptors.removeAll { it.class == RetryInterceptor }
        }
        if(found.size() > 0) return found[0]
        return null
    }

    private void addInterceptors(FeatureInfo featureInfo, RetryInterceptor parent,  RetryOnFailure retries) {
        def interceptor = new RetryInterceptor(parent,retries.times(),retries.beforeRetryMethod())
        getInterceptableMethods(featureInfo).each {
            println "adding RetryInterceptor to $it.name"
            it.addInterceptor(interceptor)
        }
    }
}
