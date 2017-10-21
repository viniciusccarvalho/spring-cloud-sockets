/*
 *  Copyright 2017 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.springframework.cloud.reactive.socket;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.exceptions.ApplicationException;
import io.rsocket.exceptions.SetupException;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.reactive.socket.annotation.ReactiveSocket;
import org.springframework.cloud.reactive.socket.util.ServiceUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Vinicius Carvalho
 */
public class DispatcherHandler extends AbstractRSocket implements ApplicationContextAware, InitializingBean {

	private ApplicationContext applicationContext;

	private List<MethodHandler> mappingHandlers = new LinkedList<>();

	private ObjectMapper mapper = new ObjectMapper();

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(this.applicationContext, Service.class);
		for(String beanName : beanNames){
			Class<?> beanType = this.applicationContext.getType(beanName);
			if(beanType != null){
				final Class<?> userType = ClassUtils.getUserClass(beanType);
				ReflectionUtils.doWithMethods(userType, method -> {
					ServiceHandlerInfo info = ServiceUtils.info(method);
					if(info != null){
						logger.info("Registering remote endpoint at path {}, exchange {} for method {}", info.getPath(), info.getExchangeMode(), method);
						validateServiceMethod(method, info);
						MethodHandler methodHandler = new MethodHandler(applicationContext.getBean(beanName), method, info);
						mappingHandlers.add(methodHandler);
					}

				});
			}
		}
	}

	private JsonNode readConnectionMetadata(String metadata){
		try {
			return mapper.readValue(metadata, JsonNode.class);
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not read metadata from client");
		}
	}

	@Override
	public Mono<Void> fireAndForget(Payload payload) {
		JsonNode metadata = readConnectionMetadata(payload.getMetadataUtf8());
		MethodHandler handler = handlerFor(metadata);
		if(handler != null){
			handler.invoke(payload);
			return Mono.empty();
		}else{
			return Mono.error(new ApplicationException("No path found for " + metadata.get("PATH").asText()));
		}

	}

	@Override
	public Mono<Payload> requestResponse(Payload payload) {
		JsonNode metadata = readConnectionMetadata(payload.getMetadataUtf8());
		MethodHandler handler = handlerFor(metadata);
		if(handler != null){
			return (Mono<Payload>) handler.invoke(payload);
		}else{
			return Mono.error(new ApplicationException("No path found for " + metadata.get("PATH").asText()));
		}
	}

	@Override
	public Flux<Payload> requestStream(Payload payload) {
		JsonNode metadata = readConnectionMetadata(payload.getMetadataUtf8());
		MethodHandler handler = handlerFor(metadata);
		if(handler != null){
			return (Flux<Payload>) handler.invoke(payload);
		}else{
			return Flux.error(new ApplicationException("No path found for " + metadata.get("PATH").asText()));
		}
	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> payloads) {

		return super.requestChannel(payloads);
	}

	private MethodHandler handlerFor(JsonNode metadata){
		return this.mappingHandlers.stream().filter(methodHandler -> { return methodHandler.getMappingInfo().getPath().equals(metadata.get("PATH").asText()); }).findFirst().orElseGet(() -> null);
	}

	private void validateServiceMethod(Method method, ServiceHandlerInfo info){
		if(method.getParameterCount() != 1){
			throw new IllegalArgumentException("Service method must have exact one argument");
		}

		switch (info.getExchangeMode()){
			case REQUEST_MANY:
				if(!Flux.class.isAssignableFrom(method.getReturnType())){
					throw new IllegalArgumentException("Request Many methods must return a Flux");
				}
				break;
			case REQUEST_STREAM:
				if(!Flux.class.isAssignableFrom(method.getReturnType())){
					throw new IllegalArgumentException("Request Many methods must return a Flux");
				}
				break;
		}
	}
}
