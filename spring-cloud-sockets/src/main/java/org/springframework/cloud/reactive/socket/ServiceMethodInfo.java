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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import reactor.core.publisher.Flux;

import org.springframework.cloud.reactive.socket.annotation.Payload;
import org.springframework.cloud.reactive.socket.annotation.ReactiveSocket;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Utility class that holds information about a Service method.
 * @author Vinicius Carvalho
 */
public class ServiceMethodInfo {

	private final Method method;

	private final ServiceMappingInfo mappingInfo;

	ResolvableType returnType;

	MethodParameter payloadParameter;

	public ServiceMethodInfo(Method method) {
		this.method = method;
		ReactiveSocket annotated = AnnotatedElementUtils.findMergedAnnotation(method, ReactiveSocket.class);
		if(annotated == null){
			throw new IllegalStateException("Service methods must be annotated with a one of {@OneWayMapping, @RequestOneMapping, @RequestManyMapping, @RequestStreamMapping} ");
		}
		this.mappingInfo = new ServiceMappingInfo(annotated.value(), annotated.mimeType(), annotated.exchangeMode());
		this.returnType = ResolvableType.forMethodReturnType(method);
		findPayloadParameter();
		validate();

	}

	public void validate(){
		switch (this.mappingInfo.getExchangeMode()){
			case REQUEST_ONE:
				if(Void.TYPE.equals(method.getReturnType())){
					throw new IllegalArgumentException("Request One methods must return");
				}
				break;
			case REQUEST_MANY:
				if(!Flux.class.isAssignableFrom(method.getReturnType())){
					throw new IllegalArgumentException("Request Many methods must return a Flux");
				}
				break;
			case REQUEST_STREAM:
				if(!Flux.class.isAssignableFrom(method.getReturnType()) || !Flux.class.isAssignableFrom(getParameterType().resolve())){
					throw new IllegalArgumentException("Request Many methods must return and receive a Flux");
				}
				break;
		}
	}

	private void findPayloadParameter(){
		if(this.method.getParameterCount() == 0){
			throw new IllegalStateException("Service methods must have at least one receiving parameter");
		}

		else if(this.method.getParameterCount() == 1){
			this.payloadParameter = new MethodParameter(this.method, 0);
		}
		int payloadAnnotations = Flux.just(this.method.getParameters())
				.filter(parameter -> parameter.getAnnotation(Payload.class) != null)
				.reduce(0, (a, parameter) -> { return a+1; })
				.block();
		if(payloadAnnotations > 1){
			throw new IllegalStateException("Service methods can have at most one @Payload annotated parameters");
		}

		for(int i=0; i<this.method.getParameters().length; i++){
			Parameter p = this.method.getParameters()[i];
			if(p.getAnnotation(Payload.class) != null){
				this.payloadParameter = new MethodParameter(this.method, i);
				break;
			}
		}
		if(this.payloadParameter == null){
			throw new IllegalStateException("Service methods annotated with more than one parameter must declare one @Payload parameter");
		}
	}

	public ResolvableType getReturnType() {
		return returnType;
	}

	public ResolvableType getParameterType() {
		return ResolvableType.forMethodParameter(this.method, payloadParameter.getParameterIndex());
	}

	public Method getMethod() {
		return method;
	}

	public ServiceMappingInfo getMappingInfo() {
		return mappingInfo;
	}
}
