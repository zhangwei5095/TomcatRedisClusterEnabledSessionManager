package com.r.tomcat.session.management.commons;

/**
 * Tomcat session de-serialization container
 *
 * @author Ranjith
 * @since 1.0
 */
public class DeserializedSessionContainer
{
	public final IRequestSession session;

	public final SessionSerializationMetadata metadata;

	public DeserializedSessionContainer(IRequestSession session, SessionSerializationMetadata metadata) {
		this.session = session;
		this.metadata = metadata;
	}
}