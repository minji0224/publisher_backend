package com.cmj.publisher.auth
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
// Target: 이 어노테이션을 어디에 적용할지 지정(함수레벨, 프로퍼티 등)
annotation class Auth (val require: Boolean = true)
// @Auth(require = true) 이 require라는 변수를 통해 어떤 동작을 수행할지 지정하는 것