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

package org.springframework.cloud.reactive.socket.converter;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.ResolvableType;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Vinicius Carvalho
 */
public class JacksonBinaryConverter extends AbstractBinaryConverter {

	private ObjectMapper mapper = new ObjectMapper();

	public JacksonBinaryConverter() {
		super(MimeTypeUtils.APPLICATION_JSON);
	}

	@Override
	public Object fromPayload(byte[] payload, ResolvableType targetType) {
		try {
			return mapper.readValue(payload, targetType.resolve());
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public byte[] toPayload(Object target) {
		try {
			return mapper.writeValueAsBytes(target);
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
