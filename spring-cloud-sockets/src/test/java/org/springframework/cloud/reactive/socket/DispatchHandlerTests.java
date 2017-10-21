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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import org.springframework.cloud.reactive.socket.annotation.OneWay;
import org.springframework.cloud.reactive.socket.annotation.RequestMany;
import org.springframework.cloud.reactive.socket.annotation.RequestOne;
import org.springframework.cloud.reactive.socket.annotation.RequestStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Vinicius Carvalho
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DispatchHandlerTests.TestConfiguration.class)
public class DispatchHandlerTests {


	@Test
	public void contextLoads() throws Exception {

	}

	@Configuration
	static class TestConfiguration{


		@Bean
		public DispatcherHandler socketHandler(){
			return new DispatcherHandler();
		}

		@Bean
		public SampleSocketService service(){
			return Mockito.mock(SampleSocketService.class);
		}

	}

	@Service
	public interface SampleSocketService {

		@OneWay("/oneway")
		void oneWay(String payload);

		@RequestOne("requestOne")
		String request(String payload);

		@RequestMany("/requestMany")
		Flux<String> requestMany(String payload);

		@RequestStream("/requestStream")
		Flux<String> requestStream(Flux<String> flux);
	}
}
