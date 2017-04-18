package org.springframework.cloud.dataflow.metrics.collector.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * @author Vinicius Carvalho
 */
public class CaffeineHealthIndicator extends AbstractHealthIndicator{

	private Cache cache;

	public CaffeineHealthIndicator(Cache cache) {
		this.cache = cache;
	}


	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		CacheStats stats = this.cache.stats();
		builder.up().withDetail("evictionCount",stats.evictionCount());
		builder.up().withDetail("hitRate",stats.hitRate());
		builder.up().withDetail("missRate",stats.missRate());
		builder.up().withDetail("hitCount",stats.hitCount());
	}
}
