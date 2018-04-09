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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import io.rsocket.util.DefaultPayload;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.reactive.socket.annotation.OneWayMapping;
import org.springframework.cloud.reactive.socket.annotation.Payload;
import org.springframework.cloud.reactive.socket.annotation.RequestManyMapping;
import org.springframework.cloud.reactive.socket.annotation.RequestOneMapping;
import org.springframework.cloud.reactive.socket.annotation.RequestStreamMapping;
import org.springframework.cloud.reactive.socket.common.User;
import org.springframework.cloud.reactive.socket.converter.JacksonConverter;
import org.springframework.cloud.reactive.socket.converter.SerializableConverter;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vinicius Carvalho
 */
public class DispatchHandlerTests {

	private DispatcherHandler handler = new DispatcherHandler();

	private JacksonConverter converter = new JacksonConverter();

	private ArrayBlockingQueue resultsQueue = new ArrayBlockingQueue(10);

	@Before
	public void setup() throws Exception{
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("simpleService",new SimpleReactiveService());
		context.refresh();
		this.handler.setApplicationContext(context);
		this.handler.afterPropertiesSet();
	}

	@Test
	public void oneWayHandler() throws Exception{
		User user = new User("Mary", "blue");
		Mono<Void> result = this.handler.fireAndForget(DefaultPayload.create(converter.write(user), getMetadataBytes(MimeType.valueOf("application/json") ,"/oneway")));
		User output = (User) resultsQueue.poll();
		assertThat(output).isEqualTo(user);
	}

	@Test
	public void oneWaySerializable() throws Exception {
		User user = new User("Mary", "blue");
		SerializableConverter serializableConverter = new SerializableConverter();
		Mono<Void> result = this.handler.fireAndForget(DefaultPayload.create(serializableConverter.write(user), getMetadataBytes(MimeType.valueOf("application/java-serialized-object") ,"/onewaybinary")));
		User output = (User) resultsQueue.poll();
		assertThat(output).isEqualTo(user);
	}

	@Test
	public void oneWayWrongMimeType() throws Exception {
		User user = new User("Mary", "blue");
		Mono<Void> result = this.handler.fireAndForget(DefaultPayload.create(converter.write(user), getMetadataBytes(MimeType.valueOf("application/binary") ,"/oneway")));
		result.doOnError(throwable -> resultsQueue.offer(throwable)).subscribe();
		assertThat(resultsQueue.poll()).isInstanceOf(Throwable.class);
	}

	@Test
	public void requestOneHandler() throws Exception {
		User user = new User("Mary", "red");
		Mono<io.rsocket.Payload> invocationResult = this.handler.requestResponse(DefaultPayload.create(converter.write(user), getMetadataBytes(MimeType.valueOf("application/json") ,"/redblue")));
		User result = invocationResult.map(payload -> {
			return (User)converter.read(payload.getDataUtf8().getBytes(), User.class);
		}).block();
		assertThat("blue").isEqualTo(result.getFavoriteColor());
	}



	@Test
	public void requestOneWrongPath() throws Exception {
		User user = new User("Mary", "red");
		Mono<io.rsocket.Payload> invocationResult = this.handler.requestResponse(DefaultPayload.create(converter.write(user), getMetadataBytes(MimeType.valueOf("application/json") ,"/notfound")));
		invocationResult.doOnError(throwable -> { resultsQueue.offer(throwable);}).subscribe();
		assertThat(resultsQueue.poll()).isInstanceOf(Throwable.class);
	}


	@Test
	public void requestMany() throws Exception {
		Integer count = 10;
		Flux<io.rsocket.Payload> invocationResult = this.handler.requestStream(DefaultPayload.create(converter.write(count), getMetadataBytes(MimeType.valueOf("application/json") ,"/requestMany")));
		List<Integer> results = invocationResult.map(payload -> (Integer)converter.read(payload.getDataUtf8().getBytes(), Integer.class)
		).collectList().block();

		assertThat(results).size().isEqualTo(10);
	}

	@Test
	public void requestStream() throws Exception {
		Flux<Integer> from = Flux.range(0,10);
		Flux<io.rsocket.Payload> payloadFlux = from.map(integer -> {
			return DefaultPayload.create(converter.write(integer), getMetadataBytes(MimeType.valueOf("application/json") ,"/requestStream"));
		});

		Flux<io.rsocket.Payload> invocationResult = this.handler.requestChannel(payloadFlux);
		List<Integer> results = invocationResult.map(payload -> {
			return (Integer)converter.read(payload.getDataUtf8().getBytes(),Integer.class);
		}).take(10).collectList().block();

		assertThat(results).size().isEqualTo(10);
	}

	private byte[] getMetadataBytes(MimeType mimeType, String path) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("PATH", path);
		metadata.put("MIME_TYPE", mimeType.toString());
		return converter.write(metadata);
	}

	class SimpleReactiveService {

		@OneWayMapping(value = "/oneway", mimeType = "application/json")
		public void oneWay(User user){
			DispatchHandlerTests.this.resultsQueue.offer(user);
		}

		@OneWayMapping("/onewaybinary")
		public void oneWayBinary(User user){
			DispatchHandlerTests.this.resultsQueue.offer(user);
		}

		@RequestOneMapping(value = "/redblue", mimeType = "application/json")
		public User redOrBlue(String nothing, @Payload User user){
			user.setFavoriteColor("blue");
			return user;
		}

		@RequestManyMapping(value = "/requestMany", mimeType = "application/json")
		public Flux<Integer> range(Integer count){
			return Flux.range(0, count);
		}

		@RequestStreamMapping(value = "/requestStream", mimeType = "application/json")
		public Flux<Integer> adder(Flux<Integer> input){
			return input.map(integer -> integer+1);
		}
	}

}
