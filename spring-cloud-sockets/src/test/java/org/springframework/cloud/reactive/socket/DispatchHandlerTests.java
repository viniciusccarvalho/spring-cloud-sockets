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


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import io.rsocket.util.PayloadImpl;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.cloud.reactive.socket.annotation.OneWayMapping;
import org.springframework.cloud.reactive.socket.annotation.Payload;
import org.springframework.cloud.reactive.socket.annotation.RequestOneMapping;
import org.springframework.cloud.reactive.socket.converter.JacksonConverter;
import org.springframework.cloud.reactive.socket.converter.SerializableConverter;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
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
		Mono<Void> result = this.handler.fireAndForget(new PayloadImpl(converter.write(user), getMetadataBytes(MimeType.valueOf("application/json") ,"/oneway")));
		User output = (User) resultsQueue.poll();
		assertThat(output).isEqualTo(user);
	}

	@Test
	public void oneWaySerializable() throws Exception {
		User user = new User("Mary", "blue");
		SerializableConverter serializableConverter = new SerializableConverter();
		Mono<Void> result = this.handler.fireAndForget(new PayloadImpl(serializableConverter.write(user), getMetadataBytes(MimeType.valueOf("application/java-serialized-object") ,"/onewaybinary")));
		User output = (User) resultsQueue.poll();
		assertThat(output).isEqualTo(user);
	}

	@Test
	public void oneWayWrongMimeType() throws Exception {
		User user = new User("Mary", "blue");
		Mono<Void> result = this.handler.fireAndForget(new PayloadImpl(converter.write(user), getMetadataBytes(MimeType.valueOf("application/binary") ,"/oneway")));
		result.doOnError(throwable -> resultsQueue.offer(throwable)).subscribe();
		assertThat(resultsQueue.poll()).isInstanceOf(Throwable.class);
	}

	@Test
	public void requestOneHandler() throws Exception {
		User user = new User("Mary", "red");
		Mono<io.rsocket.Payload> invocationResult = this.handler.requestResponse(new PayloadImpl(converter.write(user), getMetadataBytes(MimeType.valueOf("application/json") ,"/redblue")));
		User result = invocationResult.map(payload -> {
			return (User)converter.read(payload.getDataUtf8().getBytes(), ResolvableType.forType(User.class));
		}).block();
		assertThat("blue").isEqualTo(result.getFavoriteColor());
	}



	@Test
	public void requestOneWrongPath() throws Exception {
		User user = new User("Mary", "red");
		Mono<io.rsocket.Payload> invocationResult = this.handler.requestResponse(new PayloadImpl(converter.write(user), getMetadataBytes(MimeType.valueOf("application/json") ,"/notfound")));
		invocationResult.doOnError(throwable -> { resultsQueue.offer(throwable);}).subscribe();
		assertThat(resultsQueue.poll()).isInstanceOf(Throwable.class);
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

	}

	static class User implements Serializable{
		private String name;
		private String favoriteColor;

		User(){}

		User(String name, String favoriteColor) {
			this.name = name;
			this.favoriteColor = favoriteColor;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getFavoriteColor() {
			return favoriteColor;
		}

		public void setFavoriteColor(String favoriteColor) {
			this.favoriteColor = favoriteColor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			User user = (User) o;

			if (name != null ? !name.equals(user.name) : user.name != null) return false;
			return favoriteColor != null ? favoriteColor.equals(user.favoriteColor) : user.favoriteColor == null;
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + (favoriteColor != null ? favoriteColor.hashCode() : 0);
			return result;
		}
	}

}
