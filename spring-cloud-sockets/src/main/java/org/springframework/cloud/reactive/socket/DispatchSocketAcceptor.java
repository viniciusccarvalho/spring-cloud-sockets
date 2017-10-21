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

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * @author Vinicius Carvalho
 *
 * A {@link io.rsocket.SocketAcceptor} that delegates to a {@link DispatcherHandler} to route requests to the appropriate
 * service class.
 */
public class DispatchSocketAcceptor implements SocketAcceptor{

	private DispatcherHandler handler;

	private Logger logger = LoggerFactory.getLogger(SocketAcceptor.class);

	public DispatchSocketAcceptor(DispatcherHandler handler) {
		this.handler = handler;
	}

	@Override
	public Mono<RSocket> accept(ConnectionSetupPayload connectionSetupPayload, RSocket rSocket) {
		logger.info("Receiving connection");
		return Mono.just(handler);
	}

}
