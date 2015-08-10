package com.r.data.cache;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.r.data.cache.constants.RedisConstants;

/**
 * Get the active Redis cache.
 *
 * @author Ranjith
 * @since 1.0
 */
public class RedisCacheFactory
{
	private static Log log = LogFactory.getLog(RedisCacheFactory.class);
	
	private static Properties properties;

	protected static ICacheUtils cacheUtils;

	public static synchronized ICacheUtils createInstance(Properties props) throws Exception {
		try {
			if (props != null && !props.isEmpty()) {
				properties = props;
				if (!Boolean.valueOf(properties.getProperty(RedisConstants.IS_CLUSTER_ENABLED, RedisConstants.DEFAULT_IS_CLUSTER_ENABLED))) {
					cacheUtils = new RedisCacheUtils(properties);
				} else {
					cacheUtils = new RedisClusterCacheUtils(properties);
				}
			}
		} catch (Exception e) {
			log.error("Error creating redis instance", e);
		}
		return cacheUtils;
	}

	public static synchronized ICacheUtils getInstance() {
		return cacheUtils;
	}
}