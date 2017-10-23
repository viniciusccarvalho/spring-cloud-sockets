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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.rsocket.RSocket;

import org.springframework.cloud.reactive.socket.ServiceMethodInfo;
import org.springframework.cloud.reactive.socket.converter.Converter;
import org.springframework.cloud.reactive.socket.converter.JacksonConverter;
import org.springframework.cloud.reactive.socket.converter.SerializableConverter;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Vinicius Carvalho
 */
public class ReactiveSocketClient {

	private volatile RSocket socket;

	private Map<Method, AbstractRemoteHandler> remoteHandlers = new ConcurrentHashMap<>();
	
	private List<Converter> converters = new LinkedList<>();


	public ReactiveSocketClient(RSocket socket){
		initDefaultConverters();
		this.socket = socket;
	}

	private void initDefaultConverters() {
		this.converters.add(new JacksonConverter());
		this.converters.add(new SerializableConverter());
	}

	public <T> T create(final Class<T> service) {
		if(!service.isInterface()){
			throw new IllegalArgumentException("service must be an interface");
		}

		return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				AbstractRemoteHandler handler = findHandler(method);
				if(handler == null){
					return null;
				}
				return handler.invoke(args[0]);
			}
		});
	}

	private AbstractRemoteHandler findHandler(Method method){

		AbstractRemoteHandler handler = remoteHandlers.get(method);

		if(handler != null){
			return handler;
		}

		synchronized (remoteHandlers){
			ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(method);
			Converter converter = converters.stream().filter(payloadConverter -> payloadConverter.accept(serviceMethodInfo.getMappingInfo().getMimeType())).findFirst().orElseThrow(IllegalStateException::new);
			Converter metadataConverter = converters.stream().filter(binaryConverter -> binaryConverter.accept(MimeTypeUtils.APPLICATION_JSON)).findFirst().orElseThrow(IllegalStateException::new);

			switch (serviceMethodInfo.getMappingInfo().getExchangeMode()){
				case ONE_WAY:
					handler = new OneWayRemoteHandler(socket, serviceMethodInfo);
					remoteHandlers.put(method, handler);
					break;
				case REQUEST_ONE:
					handler = new RequestOneRemoteHandler(socket, serviceMethodInfo);
					remoteHandlers.put(method, handler);
					break;
			}
			handler.setPayloadConverter(converter);
			handler.setMetadataConverter(metadataConverter);
		}

		return handler;
	}

}
