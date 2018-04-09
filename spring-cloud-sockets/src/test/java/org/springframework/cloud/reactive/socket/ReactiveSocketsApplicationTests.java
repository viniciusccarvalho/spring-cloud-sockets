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

import java.time.Duration;

import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.reactive.socket.annotation.EnableReactiveSockets;
import org.springframework.cloud.reactive.socket.annotation.Payload;
import org.springframework.cloud.reactive.socket.annotation.RequestManyMapping;
import org.springframework.cloud.reactive.socket.client.ReactiveSocketClient;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Vinicius Carvalho
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ReactiveSocketsApplicationTests {

	@Test
	public void integrityTest() {
		RSocket rSocket = RSocketFactory.connect()
		                              .transport(TcpClientTransport.create("localhost", 5000))
		                              .start()
		                              .block();

		ReactiveSocketClient client = new ReactiveSocketClient(rSocket);
		TestClient clientProxy = client.create(TestClient.class);

		StepVerifier.create(Flux.merge(
						clientProxy.receiveStream1("a"),
						clientProxy.receiveStream2("b")
					))
		            .expectSubscription()
		            .expectNext("a", "b")
		            .expectNextCount(5)
		            .thenCancel()
		            .verify();
	}

	@SpringBootApplication
	@EnableReactiveSockets
	public static class TestApplication {

		@RequestManyMapping(value = "/stream1", mimeType = "application/json")
		public Flux<String> stream1(@Payload String a) {
			return Flux.just(a)
			           .mergeWith(Flux.interval(Duration.ofMillis(100))
			                          .map(i -> "1. Stream Message : [" + i + "]"));
		}

		@RequestManyMapping(value = "/stream2", mimeType = "application/json")
		public Flux<String> stream2(@Payload String b) {
			return Flux.just(b)
			           .mergeWith(Flux.interval(Duration.ofMillis(500))
			                          .map(i -> "2. Stream Message : [" + i + "]"));
		}
	}

	public interface TestClient {
		@RequestManyMapping(value = "/stream1", mimeType = "application/json")
		Flux<String> receiveStream1(String a);

		@RequestManyMapping(value = "/stream1", mimeType = "application/json")
		Flux<String> receiveStream2(String b);
	}
}
