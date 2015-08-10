package com.r.tomcat.session.management.commons;

import java.io.IOException;

import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Session;

/**
 * IRequest session manager
 *
 * @author Ranjith
 * @since 1.0
 */
public interface IRequestSessionManager
{
	public String getSessionPersistPolicies();

	public void setSessionPersistPolicies(String sessionPersistPolicies);

	public boolean getSaveOnChange();

	public boolean getAlwaysSaveAfterRequest();

	public void addLifecycleListener(LifecycleListener listener);

	public LifecycleListener[] findLifecycleListeners();

	public void removeLifecycleListener(LifecycleListener listener);

	public Session createSession(String requestedSessionId);

	public Session createEmptySession();

	public void add(Session session);

	public Session findSession(String id) throws IOException;

	public byte[] loadSessionDataFromRedis(String id) throws IOException;

	public DeserializedSessionContainer sessionFromSerializedData(String id, byte[] data) throws IOException;

	public void save(Session session) throws Exception;

	public void save(Session session, boolean forceSave) throws Exception;

	public void remove(Session session);

	public void remove(Session session, boolean update);

	public void afterRequest();
}