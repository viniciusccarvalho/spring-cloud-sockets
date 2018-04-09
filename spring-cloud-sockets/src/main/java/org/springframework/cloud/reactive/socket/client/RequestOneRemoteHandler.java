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

import java.nio.ByteBuffer;

import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;

import org.springframework.cloud.reactive.socket.ServiceMethodInfo;
import org.springframework.cloud.reactive.socket.util.ServiceUtils;

/**
 * @author Vinicius Carvalho
 */
public class RequestOneRemoteHandler extends AbstractRemoteHandler {

	public RequestOneRemoteHandler(RSocket socket, ServiceMethodInfo info) {
		super(socket, info);
	}

	@Override
	public Object doInvoke(Object argument) {
		byte[] data = payloadConverter.write(argument);
		Mono monoResult = socket.requestResponse(DefaultPayload.create(
				ByteBuffer.wrap(data),
				getMetadata()
		))
				.map(payload -> payloadConverter.read(ServiceUtils.toByteArray(payload.getData()), ServiceUtils.getActualType(info.getReturnType())));
		if(Mono.class.isAssignableFrom(info.getReturnType().resolve())){
			return monoResult;
		}
		else{
			return monoResult.block();
		}

	}
}
