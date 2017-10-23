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


import org.springframework.core.ResolvableType;
import org.springframework.util.MimeType;
import org.springframework.util.SerializationUtils;

/**
 * @author Vinicius Carvalho
 */
public class SerializableConverter extends AbstractConverter {

	public SerializableConverter() {
		super(MimeType.valueOf("application/java-serialized-object"));
	}


	@Override
	public Object read(byte[] payload, ResolvableType targetType) {
		return SerializationUtils.deserialize(payload);
	}

	@Override
	public byte[] write(Object target) {
		return SerializationUtils.serialize(target);
	}


}
