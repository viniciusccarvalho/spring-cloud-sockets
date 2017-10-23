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

import org.junit.Test;
import reactor.core.publisher.Flux;

import org.springframework.cloud.reactive.socket.annotation.OneWayMapping;
import org.springframework.cloud.reactive.socket.annotation.Payload;
import org.springframework.cloud.reactive.socket.annotation.RequestManyMapping;
import org.springframework.cloud.reactive.socket.annotation.RequestOneMapping;
import org.springframework.cloud.reactive.socket.annotation.RequestStreamMapping;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vinicius Carvalho
 */
public class ServiceMethodInfoTests {


	@Test(expected = IllegalStateException.class)
	public void failNotAnnotated() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "failNotAnnotated", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
	}

	@Test(expected = IllegalStateException.class)
	public void failNoArgument() throws Exception{
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "failNoArgument", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
	}

	@Test
	public void oneWayMethod() throws Exception{

		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "oneWay", String.class);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
		assertThat(ExchangeMode.ONE_WAY).isEqualTo(serviceMethodInfo.getMappingInfo().getExchangeMode());
		assertThat(Void.TYPE).isEqualTo(serviceMethodInfo.getReturnType().resolve());
		assertThat(String.class).isAssignableFrom(serviceMethodInfo.getParameterType().resolve());
		assertThat("/foo").isEqualTo(serviceMethodInfo.getMappingInfo().getPath());
		assertThat("application/json").isEqualTo(serviceMethodInfo.getMappingInfo().getMimeType().toString());
	}

	@Test
	public void multipleParameters() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "oneWayMultipleParameters", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
		assertThat(String.class).isAssignableFrom(serviceMethodInfo.getParameterType().resolve());
	}

	@Test(expected = IllegalArgumentException.class)
	public void requestOneWrongReturn() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "requestOneWrongReturn", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
	}

	@Test(expected = IllegalStateException.class)
	public void multipleParametersNotAnnotated() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "multipleParametersNotAnnotated", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
	}

	@Test(expected = IllegalStateException.class)
	public void multiplePayloads() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "multiplePayloadAnnotations", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
	}

	@Test
	public void requestOneMapping() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "requestOneMapping", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
		assertThat(String.class).isAssignableFrom(serviceMethodInfo.getReturnType().resolve());
		assertThat(Integer.class).isAssignableFrom(serviceMethodInfo.getParameterType().resolve());
	}

	@Test(expected = IllegalArgumentException.class)
	public void requestManyWrongReturn() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "requestManyWrongReturn", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
	}

	@Test
	public void requestMany() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "requestMany", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
		assertThat(Flux.class).isAssignableFrom(serviceMethodInfo.getReturnType().resolve());
		assertThat(String.class).isAssignableFrom(serviceMethodInfo.getParameterType().resolve());
	}

	@Test
	public void requestStream() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "requestStream", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
		assertThat(Flux.class).isAssignableFrom(serviceMethodInfo.getReturnType().resolve());
		assertThat(Flux.class).isAssignableFrom(serviceMethodInfo.getParameterType().resolve());
	}

	@Test(expected = IllegalArgumentException.class)
	public void requestStreamWrongReturn() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "requestStreamWrongReturn", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
	}

	@Test(expected = IllegalArgumentException.class)
	public void requestStreamWrongParameter() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "requestStreamWrongParameter", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
	}

	@Test
	public void requestStreamAnnotated() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "requestStreamAnnotated", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
		assertThat(Flux.class).isAssignableFrom(serviceMethodInfo.getReturnType().resolve());
		assertThat(Flux.class).isAssignableFrom(serviceMethodInfo.getParameterType().resolve());
	}

	@Test
	public void testParameterMapping() throws Exception {
		Method m = ReflectionUtils.findMethod(ServiceInfoTest.class, "oneWayMultipleParameters", null);
		ServiceMethodInfo serviceMethodInfo = new ServiceMethodInfo(m);
		String payload  = "foo";
		Object[] args = serviceMethodInfo.buildInvocationArguments(payload, null);
		assertThat(3).isEqualTo(args.length);
		assertThat(args[0]).isNull();
		assertThat(args[1]).isEqualTo(payload);
		assertThat(args[2]).isEqualTo(false);

	}

	interface ServiceInfoTest{

		void failNotAnnotated(String payload);

		@OneWayMapping("/foo")
		void failNoArgument();

		@OneWayMapping(value = "/foo", mimeType = "application/json")
		void oneWay(String payload);

		@OneWayMapping(value = "/foo", mimeType = "application/json")
		void oneWayMultipleParameters(Integer index, @Payload String payload, boolean optional);

		@RequestOneMapping("/foo")
		void requestOneWrongReturn(String foo);

		@RequestOneMapping("/foo")
		String multipleParametersNotAnnotated(String foo, String bar);

		@RequestOneMapping("/foo")
		String multiplePayloadAnnotations(@Payload String foo, @Payload String bar);

		@RequestOneMapping("/foo")
		String requestOneMapping(Integer x);

		@RequestManyMapping("/foo")
		String requestManyWrongReturn(String foo);

		@RequestManyMapping("/foo")
		Flux<String> requestMany(String foo);

		@RequestStreamMapping("/foo")
		Flux<String> requestStream(Flux<String> foo);

		@RequestStreamMapping("/foo")
		String requestStreamWrongReturn(Flux<String> foo);

		@RequestStreamMapping("/foo")
		Flux<String> requestStreamWrongParameter(String foo);

		@RequestStreamMapping("/foo")
		Flux<String> requestStreamAnnotated(String foo, @Payload Flux<String> bar);


	}
}
