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

import java.lang.reflect.Method;

import org.springframework.cloud.reactive.socket.ServiceHandlerInfo;
import org.springframework.cloud.reactive.socket.annotation.ReactiveSocket;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * @author Vinicius Carvalho
 */
public class ServiceUtils {

	public static ServiceHandlerInfo info(Method method){
		ReactiveSocket annotated = AnnotatedElementUtils.findMergedAnnotation(method, ReactiveSocket.class);
		if(annotated == null){
			return null;
		}
		ServiceHandlerInfo info = new ServiceHandlerInfo(annotated.value(), annotated.mimeType(), annotated.exchangeMode());
		return info;
	}
}
