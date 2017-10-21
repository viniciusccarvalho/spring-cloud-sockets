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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.core.ResolvableType;

/**
 * @author Vinicius Carvalho
 */
public class MethodHandler {

	private Object bean;

	private Method method;

	ResolvableType returnType;

	ResolvableType parameterType;

	private ServiceHandlerInfo mappingInfo;

	public MethodHandler(Object bean, Method method, ServiceHandlerInfo mappingInfo) {
		this.bean = bean;
		this.method = method;
		this.mappingInfo = mappingInfo;
		initResolvers();
	}

	private void initResolvers() {
		returnType = ResolvableType.forMethodReturnType(this.method);
		parameterType = ResolvableType.forMethodParameter(this.method, 0);
	}

	public Object invoke(Object... args) {
		try {
			return method.invoke(bean, args);
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
		catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}

	}

	public ResolvableType getReturnType() {
		return returnType;
	}

	public ResolvableType getParameterType() {
		return parameterType;
	}

	public ServiceHandlerInfo getMappingInfo() {
		return mappingInfo;
	}
}
