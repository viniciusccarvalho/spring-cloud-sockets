package org.springframework.cloud.reactive.socket.annotation;

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
public @interface ReactiveService {
}
