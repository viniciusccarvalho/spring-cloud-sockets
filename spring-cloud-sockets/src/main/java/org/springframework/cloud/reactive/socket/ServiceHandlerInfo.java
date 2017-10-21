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

import org.springframework.util.MimeType;

/**
 * @author Vinicius Carvalho
 */
public class ServiceHandlerInfo {

	private String path;

	private MimeType mimeType;

	private ExchangeMode exchangeMode;


	public ServiceHandlerInfo(String path, String mimeType, ExchangeMode exchangeMode){
		this.path = path;
		this.exchangeMode = exchangeMode;
		this.mimeType = MimeType.valueOf(mimeType);
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public MimeType getMimeType() {
		return mimeType;
	}

	public void setMimeType(MimeType mimeType) {
		this.mimeType = mimeType;
	}

	public ExchangeMode getExchangeMode() {
		return exchangeMode;
	}

	public void setExchangeMode(ExchangeMode exchangeMode) {
		this.exchangeMode = exchangeMode;
	}
}
