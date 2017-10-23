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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.cloud.reactive.socket.annotation.ReactiveService;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.reactive.socket.annotation.EnableReactiveSockets;
import org.springframework.cloud.reactive.socket.annotation.OneWayMapping;
import org.springframework.cloud.reactive.socket.annotation.RequestOneMapping;
import org.springframework.cloud.reactive.socket.client.ReactiveSocketClient;
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
		ReactiveSocketClient client = new ReactiveSocketClient("localhost", 5000);
		MyService clientService = client.create(MyService.class);

		User user = new User();
		user.setFavoriteColor("blue");
		user.setName("Mary");
		clientService.process(user);
		Mono<Integer> result = clientService.create(user);
		System.out.println("Id: " + result.block());
	}


	@SpringBootApplication
	@EnableReactiveSockets
	public static class TestApplication {

		@Bean
		public MyService myService(){
			return new MyServiceImpl();
		}
	}



	interface MyService {
		@OneWayMapping(value = "/forget", mimeType = "application/json")
		void process(User user);

		@RequestOneMapping(value = "/user", mimeType = "application/json")
		Mono<Integer> create(User user);
	}

	@ReactiveService
	public static class MyServiceImpl implements MyService{

		public void process(User user){
			System.out.println(user);
		}

		public Mono<Integer> create(User user){
			return Mono.just(1);
		}

	}



	public static class User {
		private String name;
		private String favoriteColor;

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
		public String toString() {
			final StringBuilder sb = new StringBuilder("User{");
			sb.append("name='").append(name).append('\'');
			sb.append(", favoriteColor='").append(favoriteColor).append('\'');
			sb.append('}');
			return sb.toString();
		}
	}

}
