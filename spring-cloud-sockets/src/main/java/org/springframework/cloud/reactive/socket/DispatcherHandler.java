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
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.exceptions.ApplicationException;
import io.rsocket.util.PayloadImpl;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.reactive.socket.annotation.ReactiveSocket;
import org.springframework.cloud.reactive.socket.converter.Converter;
import org.springframework.cloud.reactive.socket.converter.JacksonConverter;
import org.springframework.cloud.reactive.socket.converter.SerializableConverter;
import org.springframework.cloud.reactive.socket.util.ServiceUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.ReflectionUtils;

/**
 * @author Vinicius Carvalho
 */
public class DispatcherHandler extends AbstractRSocket implements ApplicationContextAware, InitializingBean {

	private ApplicationContext applicationContext;

	private List<MethodHandler> mappingHandlers = new LinkedList<>();

	private ObjectMapper mapper = new ObjectMapper();

	private Logger logger = LoggerFactory.getLogger(getClass());

	private List<Converter> converters = new LinkedList<>();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	private void initDefaultConverters() {
		this.converters.add(new JacksonConverter());
		this.converters.add(new SerializableConverter());
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.applicationContext, Object.class);
		for(String beanName : beanNames){
			Class<?> beanType = this.applicationContext.getType(beanName);
			if(beanType != null){
				final Class<?> userType = ClassUtils.getUserClass(beanType);
				ReflectionUtils.doWithMethods(userType, method -> {
					if(AnnotatedElementUtils.findMergedAnnotation(method, ReactiveSocket.class) != null) {
						ServiceMethodInfo info = new ServiceMethodInfo(method);
						logger.info("Registering remote endpoint at path {}, exchange {} for method {}", info.getMappingInfo().getPath(), info.getMappingInfo().getExchangeMode(), method);
						MethodHandler methodHandler = new MethodHandler(applicationContext.getBean(beanName), info);
						mappingHandlers.add(methodHandler);
					}
				});
			}
		}
		initDefaultConverters();
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


		try{
			MethodHandler handler = handlerFor(metadata);
			Converter converter = converterFor(MimeType.valueOf(metadata.get("MIME_TYPE").textValue()));
			Object converted = converter.read(ServiceUtils.toByteArray(payload.getData()), handler.getInfo().getParameterType());
			handler.invoke(handler.getInfo().buildInvocationArguments(converted, null));
			return Mono.empty();
		}catch (Exception e){
			return Mono.error(e);
		}

	}

	@Override
	public Mono<Payload> requestResponse(Payload payload) {
		JsonNode metadata = readConnectionMetadata(payload.getMetadataUtf8());
		try {
			MethodHandler handler = handlerFor(metadata);
			Converter converter = converterFor(MimeType.valueOf(metadata.get("MIME_TYPE").textValue()));
			Object converted = converter.read(ServiceUtils.toByteArray(payload.getData()), handler.getInfo().getParameterType());
			Object result = handler.invoke(handler.getInfo().buildInvocationArguments(converted, null));
			Mono monoResult = monoFor(result);
			return monoResult.map(o -> {
				byte[] data = converter.write(o);
				return new PayloadImpl(data);
			});

		}catch (Exception e){
			return Mono.error(e);
		}
	}

	@Override
	public Flux<Payload> requestStream(Payload payload) {
		JsonNode metadata = readConnectionMetadata(payload.getMetadataUtf8());
		try {
			MethodHandler handler = handlerFor(metadata);
			Converter converter = converterFor(MimeType.valueOf(metadata.get("MIME_TYPE").textValue()));
			Object converted = converter.read(ServiceUtils.toByteArray(payload.getData()), handler.getInfo().getParameterType());
			Flux result = (Flux)handler.invoke(handler.getInfo().buildInvocationArguments(converted, null));
			return result.map(o ->
				new PayloadImpl(converter.write(o))
			);

		} catch (Exception e){
			return Flux.error(new ApplicationException("No path found for " + metadata.get("PATH").asText()));
		}
	}

	private Mono monoFor(Object argument){

		if(argument.getClass().isAssignableFrom(Mono.class)){
			return (Mono)argument;
		}else{
			return Mono.just(argument);
		}

	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
		Flux<Payload> flux = Flux.from(payloads);
		Payload headerPayload = flux.take(1).next().block();
		JsonNode metadata = readConnectionMetadata(headerPayload.getMetadataUtf8());
		try{
			MethodHandler handler = handlerFor(metadata);
			Converter converter = converterFor(MimeType.valueOf(metadata.get("MIME_TYPE").textValue()));
			Flux converted = flux.repeat().map(payload -> {
				return converter.read(ServiceUtils.toByteArray(payload.getData()), handler.getInfo().getParameterType().getGeneric(0));
			});
			Flux result = (Flux)handler.invoke(handler.getInfo().buildInvocationArguments(converted, null));
			return result.map(o ->
					new PayloadImpl(converter.write(o))
			);
		}catch (Exception e){
			return Flux.error(e);
		}
	}

	private Converter converterFor(MimeType mimeType){
		return this.converters
				.stream()
				.filter(binaryConverter -> binaryConverter.accept(mimeType))
				.findFirst()
				.orElseThrow(IllegalStateException::new);
	}

	private MethodHandler handlerFor(JsonNode metadata){
		return this.mappingHandlers
				.stream()
				.filter(methodHandler -> { return methodHandler
						.getInfo()
						.getMappingInfo()
						.getPath().equals(metadata.get("PATH").asText()); })
				.findFirst()
				.orElseThrow(() -> { return new ApplicationException("No handler found");} );
	}

}
