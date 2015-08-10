package com.r.data.cache;

/**
 * IRedis cache utils
 *
 * @author Ranjith
 * @since 1.0
 */
public interface ICacheUtils
{
	public boolean isAvailable();
	
	public void setByteArray(byte[] key, byte[] value);

	public byte[] getByteArray(String key);
	
	public void deleteKey(String key);
	
	public Long setStringIfKeyNotExists(byte[] key, byte[] value);

	public void expire(byte[] key, int ttl);
}