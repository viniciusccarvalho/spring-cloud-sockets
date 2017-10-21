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

package org.springframework.cloud.reactive.socket.client;

import java.lang.reflect.Method;

import io.rsocket.RSocket;

import org.springframework.cloud.reactive.socket.ServiceHandlerInfo;
import org.springframework.cloud.reactive.socket.converter.PayloadConverter;
import org.springframework.core.ResolvableType;

/**
 * @author Vinicius Carvalho
 */
public abstract class AbstractRemoteHandler {

	protected RSocket socket;

	protected ServiceHandlerInfo info;

	protected ResolvableType returnType;

	protected ResolvableType parameterType;

	protected PayloadConverter converter;

	public PayloadConverter getConverter() {
		return converter;
	}

	public void setConverter(PayloadConverter converter) {
		this.converter = converter;
	}

	public AbstractRemoteHandler(RSocket socket, ServiceHandlerInfo info, Method method) {
		this.socket = socket;
		this.info = info;
		this.returnType = ResolvableType.forMethodReturnType(method);
		this.parameterType = ResolvableType.forMethodParameter(method, 0);
	}

	public Object invoke(Object argument){
		return doInvoke(argument);
	}

	public abstract Object doInvoke(Object argument);

}
