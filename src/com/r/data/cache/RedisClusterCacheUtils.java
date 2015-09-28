package com.r.data.cache;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.exceptions.JedisClusterMaxRedirectionsException;
import redis.clients.jedis.exceptions.JedisConnectionException;

import com.r.data.cache.manager.RedisClusterManager;

/**
 * Redis cluster cache utils
 *
 * @author Ranjith
 * @since 1.0
 */
public class RedisClusterCacheUtils implements ICacheUtils
{
	private Log log = LogFactory.getLog(RedisClusterCacheUtils.class);

	public boolean available = false;

	private static int numRetries = 30;

	private RedisClusterManager clusterManager = null;

	RedisClusterCacheUtils(Properties properties) throws Exception {
		try {
			clusterManager = RedisClusterManager.createInstance(properties);
		} catch (Exception e) {
			this.available = false;
			log.error("Exception initializing Redis cluster: " + e);
		}
		this.available = true;
	}

	@Override
	public boolean isAvailable() {
		return available;
	}

	@Override
	public void setByteArray(byte[] key, byte[] value) {
		int tries = 0;
		boolean sucess = false;
		do {
			tries++;
			try {
				if (key != null && value != null) {
					clusterManager.getJedis().set(key, value);
				}
				sucess = true;
			} catch (JedisClusterMaxRedirectionsException | JedisConnectionException e) {
				log.error("Jedis connection failed, retrying..." + tries);
				waitforFailover();
			}
		} while (!sucess && tries <= numRetries);
	}

	@Override
	public Long setStringIfKeyNotExists(byte[] key, byte[] value) {
		int tries = 0;
		Long retVal = null;
		boolean sucess = false;
		do {
			tries++;
			try {
				if (key != null && value != null) {
					retVal = clusterManager.getJedis().setnx(key, value);
				}
				sucess = true;
			} catch (JedisClusterMaxRedirectionsException | JedisConnectionException e) {
				log.error("Jedis connection failed, retrying..." + tries);
				waitforFailover();
			}
		} while (!sucess && tries <= numRetries);
		return retVal;
	}

	@Override
	public void expire(byte[] key, int ttl) {
		int tries = 0;
		boolean sucess = false;
		do {
			tries++;
			try {
				clusterManager.getJedis().expire(key, ttl);
				sucess = true;
			} catch (JedisClusterMaxRedirectionsException | JedisConnectionException e) {
				log.error("Jedis connection failed, retrying..." + tries);
				waitforFailover();
			}
		} while (!sucess && tries <= numRetries);
	}

	@Override
	public byte[] getByteArray(String key) {
		int tries = 0;
		boolean sucess = false;
		byte[] array = new byte[1];
		do {
			tries++;
			try {
				if (key != null) {
					array = clusterManager.getJedis().get(key.getBytes());
				}
				sucess = true;
			} catch (JedisClusterMaxRedirectionsException | JedisConnectionException e) {
				log.error("Jedis connection failed, retrying..." + tries);
				waitforFailover();
			}
		} while (!sucess && tries <= numRetries);
		return array;
	}

	@Override
	public void deleteKey(String key) {
		int tries = 0;
		boolean sucess = false;
		do {
			tries++;
			try {
				if (key != null) {
					clusterManager.getJedis().del(key);
				}
				sucess = true;
			} catch (JedisClusterMaxRedirectionsException | JedisConnectionException e) {
				log.error("Jedis connection failed, retrying..." + tries);
				waitforFailover();
			}
		} while (!sucess && tries <= numRetries);
	}

	/*
	 * method to wait for cluster fails
	 */
	private void waitforFailover() {
		try {
			Thread.sleep(4000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
