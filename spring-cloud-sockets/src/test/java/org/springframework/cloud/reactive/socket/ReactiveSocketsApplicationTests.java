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
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.PayloadImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.reactive.socket.annotation.EnableReactiveSockets;
import org.springframework.cloud.reactive.socket.annotation.OneWayMapping;
import org.springframework.cloud.reactive.socket.annotation.RequestStreamMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Vinicius Carvalho
 */
@SpringBootTest(classes = ReactiveSocketsApplicationTests.TestApplication.class)
@RunWith(SpringRunner.class)
public class ReactiveSocketsApplicationTests {

	@Test
	public void contextLoads()  throws Exception{
		Request request = new Request("Hello", "/forget", "text/plain");
		RSocket socket = RSocketFactory.connect()
				.transport(TcpClientTransport.create("localhost", 5000))
				.start()
				.block();

		socket.fireAndForget(request.toPayload()).block();
		Thread.sleep(10000);

	}




	@SpringBootApplication
	@EnableReactiveSockets
	public static class TestApplication {

		@Bean
		public MyService myService(){
			return new MyService();
		}
	}


	@Service
	public static class MyService {

		@OneWayMapping(value = "/forget", mimeType = "text/plain")
		public void process(Payload payload){
			System.out.println(payload.getDataUtf8());
		}

		@RequestStreamMapping(value = "/hash", mimeType = "application/json")
		public Flux<Integer> hash(Flux<String> flux){
			return flux.map(s -> s.hashCode());
		}

	}


	public class Request {

		private ObjectMapper mapper = new ObjectMapper();

		private String contentType;

		private String path;

		private String data;

		public Request(String data, String path, String contentType) {
			this.contentType = contentType;
			this.path = path;
			this.data = data;
		}

		public Payload toPayload() {
			Map<String,String> metadata = new HashMap<>();
			metadata.put("PATH", this.path);
			metadata.put("MIME_TYPE", this.contentType);
			try {
				return new PayloadImpl(this.data.getBytes(), mapper.writeValueAsBytes(metadata));
			}
			catch (JsonProcessingException e) {
				throw new IllegalStateException("Could not serialize metadata");
			}
		}
	}

}
