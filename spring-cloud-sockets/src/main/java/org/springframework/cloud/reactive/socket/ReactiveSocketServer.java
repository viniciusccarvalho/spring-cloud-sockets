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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.ServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;

import org.springframework.context.SmartLifecycle;

/**
 * @author Vinicius Carvalho
 */
public class ReactiveSocketServer implements SmartLifecycle{

	private ServerTransport transport;

	private SocketAcceptor acceptor;

	private Disposable disposable;

	private Lock lifecycleMonitor = new ReentrantLock();

	private volatile boolean running = false;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public ReactiveSocketServer(ServerTransport transport, SocketAcceptor acceptor) {
		this.transport = transport;
		this.acceptor = acceptor;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable runnable) {
		try{
			this.lifecycleMonitor.lock();
			this.stop();
			runnable.run();
		} finally {
			this.lifecycleMonitor.unlock();
		}
	}

	@Override
	public void start() {
		try{
			this.lifecycleMonitor.lock();
			if(!running){
				logger.info("Starting RSocket server using transport: {} ", this.transport.getClass().getName());
				this.disposable = RSocketFactory.receive()
						.acceptor(acceptor)
						.transport(transport)
						.start()
						.subscribe();
				running = true;
			}
		}finally {
			lifecycleMonitor.unlock();
		}
	}

	@Override
	public void stop() {
		try{
			this.lifecycleMonitor.lock();
			this.disposable.dispose();
			this.running = false;
		} finally {
			this.lifecycleMonitor.unlock();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return 0;
	}
}
