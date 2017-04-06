/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.metrics.collector;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.github.benmanes.caffeine.cache.Ticker;

/**
 * @author Vinicius Carvalho
 */
public abstract class BaseCacheTests {




	public FakeTicker getTicker(){
		return new FakeTicker();
	}


	public class FakeTicker implements Ticker {

		private final AtomicLong nanos = new AtomicLong();

		/** Advances the ticker currentValue by {@code time} in {@code timeUnit}. */
		public FakeTicker advance(long time, TimeUnit timeUnit) {
			nanos.addAndGet(timeUnit.toNanos(time));
			return this;
		}

		@Override
		public long read() {
			long value = nanos.getAndAdd(0);
			return value;
		}
	}
}
