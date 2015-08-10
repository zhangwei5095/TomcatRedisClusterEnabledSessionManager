package com.r.tomcat.session.management.commons;

/**
 * ISerializer
 *
 * @author Ranjith
 * @since 4.15.9.4_Tomcat
 */
public interface ISerializer
{
	public void setClassLoader(ClassLoader loader);

	public byte[] attributesHashFrom(IRequestSession session) throws Exception;

	public byte[] serializeFrom(IRequestSession session, SessionSerializationMetadata metadata) throws Exception;

	public void deserializeInto(byte[] data, IRequestSession session, SessionSerializationMetadata metadata) throws Exception;
}