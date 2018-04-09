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


import com.fasterxml.jackson.databind.JsonNode;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


import org.springframework.cloud.reactive.socket.annotation.OneWayMapping;
import org.springframework.cloud.reactive.socket.annotation.RequestOneMapping;
import org.springframework.cloud.reactive.socket.client.ReactiveSocketClient;
import org.springframework.cloud.reactive.socket.common.User;
import org.springframework.cloud.reactive.socket.converter.Converter;
import org.springframework.cloud.reactive.socket.converter.JacksonConverter;



/**
 * @author Vinicius Carvalho
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactiveSocketClientTests {

	@Mock
	private RSocket mockSocket;

	private Converter converter = new JacksonConverter();


	@Test
	public void oneWayClientTests() throws Exception {
		ReactiveSocketClient socketClient = new ReactiveSocketClient(mockSocket);
		SampleClient client = socketClient.create(SampleClient.class);
		ArgumentCaptor<Payload> captor = ArgumentCaptor.forClass(Payload.class);
		User user = new User("Alice","blue");
		client.oneWay(user);
		byte[] converted = converter.write(user);
		verify(mockSocket, times(1)).fireAndForget(captor.capture());


		Payload payload = captor.getValue();
		JsonNode metadata = (JsonNode) converter.read(payload.getMetadataUtf8().getBytes(), JsonNode.class);
		assertThat(converted).isEqualTo(payload.getDataUtf8().getBytes());
		assertThat("/oneway").isEqualTo(metadata.get("PATH").textValue());
		assertThat("application/json").isEqualTo(metadata.get("MIME_TYPE").textValue());
	}


	@Test
	public void requestOneClientTests() throws Exception {
		ReactiveSocketClient socketClient = new ReactiveSocketClient(mockSocket);
		SampleClient client = socketClient.create(SampleClient.class);
		ArgumentCaptor<Payload> captor = ArgumentCaptor.forClass(Payload.class);
		User user = new User("Alice","blue");
		byte[] converted = converter.write(user);
		when(mockSocket.requestResponse(Mockito.any(Payload.class))).thenReturn(Mono.just(DefaultPayload.create(converted)));
		client.create(user);
		verify(mockSocket, times(1)).requestResponse(captor.capture());
		Payload payload = captor.getValue();
		JsonNode metadata = (JsonNode) converter.read(payload.getMetadataUtf8().getBytes(), JsonNode.class);
		assertThat(converted).isEqualTo(payload.getDataUtf8().getBytes());
		assertThat("/requestOne").isEqualTo(metadata.get("PATH").textValue());
		assertThat("application/json").isEqualTo(metadata.get("MIME_TYPE").textValue());
	}


	interface SampleClient {

		@OneWayMapping(value="/oneway", mimeType = "application/json")
		Mono<Void> oneWay(User user);

		@RequestOneMapping(value="/requestOne", mimeType = "application/json")
		Mono<User> create(User user);
	}

}
