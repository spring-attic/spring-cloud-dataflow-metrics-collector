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

package org.springframework.cloud.dataflow.metrics.collector.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * @author Vinicius Carvalho
 */
public class CaffeineHealthIndicator extends AbstractHealthIndicator {

	private Cache cache;

	public CaffeineHealthIndicator(Cache cache) {
		this.cache = cache;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		CacheStats stats = this.cache.stats();
		builder.up().withDetail("evictionCount", stats.evictionCount());
		builder.up().withDetail("hitRate", stats.hitRate());
		builder.up().withDetail("missRate", stats.missRate());
		builder.up().withDetail("hitCount", stats.hitCount());
	}
}
