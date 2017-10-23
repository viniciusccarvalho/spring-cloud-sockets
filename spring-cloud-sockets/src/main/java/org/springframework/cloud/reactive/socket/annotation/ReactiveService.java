package org.springframework.cloud.reactive.socket.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Mark the service is ReactiveService, DispatcherHandled by
 * {@link org.springframework.cloud.reactive.socket.DispatcherHandler}
 *
 * @Authrize meijies
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ReactiveService {
    String value() default "";
}
