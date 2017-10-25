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

package org.springframework.cloud.reactive.socket.util;

import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;

/**
 * @author Vinicius Carvalho
 */
public class ServiceUtils {

	public static byte[] toByteArray(ByteBuffer buffer){
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes, 0, bytes.length);
		return bytes;
	}

	public static Class<?> getActualType(ResolvableType type){
		if(Publisher.class.isAssignableFrom(type.resolve())){
			return type.getGeneric(0).resolve();
		}else{
			return type.resolve();
		}
	}
}
