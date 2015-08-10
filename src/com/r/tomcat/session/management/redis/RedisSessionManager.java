package com.r.tomcat.session.management.redis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Properties;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.Valve;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.r.data.cache.ICacheUtils;
import com.r.data.cache.RedisCacheFactory;
import com.r.data.cache.constants.RedisConstants;
import com.r.tomcat.session.management.commons.DeserializedSessionContainer;
import com.r.tomcat.session.management.commons.IRequestSessionManager;
import com.r.tomcat.session.management.commons.ISerializer;
import com.r.tomcat.session.management.commons.JavaSerializer;
import com.r.tomcat.session.management.commons.SessionHandlerValve;
import com.r.tomcat.session.management.commons.SessionSerializationMetadata;

/**
 * Tomcat redis session manager
 *
 * @author Ranjith
 * @since 1.0
 */
public class RedisSessionManager extends ManagerBase implements IRequestSessionManager, Lifecycle
{
	private Log log = LogFactory.getLog(RedisSessionManager.class);
	
	private ICacheUtils cache;

	protected ISerializer serializer;

	protected SessionHandlerValve handlerValve;

	protected byte[] NULL_SESSION = "null".getBytes();

	protected LifecycleSupport lifecycle = new LifecycleSupport(this);

	protected ThreadLocal<String> currentSessionId = new ThreadLocal<>();

	protected ThreadLocal<RedisSession> currentSession = new ThreadLocal<>();

	protected ThreadLocal<Boolean> currentSessionIsPersisted = new ThreadLocal<>();

	protected EnumSet<SessionPersistPolicy> sessionPersistPoliciesSet = EnumSet.of(SessionPersistPolicy.DEFAULT);

	protected ThreadLocal<SessionSerializationMetadata> currentSessionSerializationMetadata = new ThreadLocal<>();

	enum SessionPersistPolicy {
		DEFAULT, SAVE_ON_CHANGE, ALWAYS_SAVE_AFTER_REQUEST;

		static SessionPersistPolicy fromName(String name) {
			for (SessionPersistPolicy policy : SessionPersistPolicy.values()) {
				if (policy.name().equalsIgnoreCase(name)) {
					return policy;
				}
			}
			throw new IllegalArgumentException("Invalid session persist policy [" + name + "]. Must be one of " + Arrays.asList(SessionPersistPolicy.values()) + ".");
		}
	}

	public String getSessionPersistPolicies() {
		StringBuilder policies = new StringBuilder();
		for (Iterator<SessionPersistPolicy> iter = this.sessionPersistPoliciesSet.iterator(); iter.hasNext();) {
			SessionPersistPolicy policy = iter.next();
			policies.append(policy.name());
			if (iter.hasNext()) {
				policies.append(",");
			}
		}
		return policies.toString();
	}

	public void setSessionPersistPolicies(String sessionPersistPolicies) {
		String[] policyArray = sessionPersistPolicies.split(",");
		EnumSet<SessionPersistPolicy> policySet = EnumSet.of(SessionPersistPolicy.DEFAULT);
		for (String policyName : policyArray) {
			SessionPersistPolicy policy = SessionPersistPolicy.fromName(policyName);
			policySet.add(policy);
		}
		this.sessionPersistPoliciesSet = policySet;
	}

	public boolean getSaveOnChange() {
		return this.sessionPersistPoliciesSet.contains(SessionPersistPolicy.SAVE_ON_CHANGE);
	}

	public boolean getAlwaysSaveAfterRequest() {
		return this.sessionPersistPoliciesSet.contains(SessionPersistPolicy.ALWAYS_SAVE_AFTER_REQUEST);
	}

	/**
	 * Add a lifecycle event listener to this component.
	 * 
	 * @param listener
	 *            - The listener to add
	 */
	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		lifecycle.addLifecycleListener(listener);
	}

	/**
	 * Get the lifecycle listeners associated with this lifecycle. If this Lifecycle has no listeners registered, a zero-length array is returned.
	 */
	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return lifecycle.findLifecycleListeners();
	}

	/**
	 * Remove a lifecycle event listener from this component.
	 * 
	 * @param listener
	 *            - The listener to remove
	 */
	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		lifecycle.removeLifecycleListener(listener);
	}

	/**
	 * Start this component and implement the requirements of {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
	 * 
	 * @exception LifecycleException
	 *                - if this component detects a fatal error that prevents this component from being used
	 */
	@Override
	protected synchronized void startInternal() throws LifecycleException {
		super.startInternal();
		setState(LifecycleState.STARTING);
		Boolean attachedToValve = false;
		for (Valve valve : getContainer().getPipeline().getValves()) {
			if (valve instanceof SessionHandlerValve) {
				this.handlerValve = (SessionHandlerValve) valve;
				this.handlerValve.setRedisSessionManager(this);
				attachedToValve = true;
				break;
			}
		}
		if (!attachedToValve) {
			throw new LifecycleException("Unable to attach to session handling valve; sessions cannot be saved after the request without the valve starting properly.");
		}
		try {
			initializeSerializer();
			cache = RedisCacheFactory.createInstance(getRedisProperties());
		} catch (Exception e) {
			log.error("Error while initializing serializer/rediscache", e);
		}
		log.info("The sessions will expire after " + getMaxInactiveInterval() + " seconds");
		setDistributable(true);
	}

	/**
	 * Stop this component and implement the requirements of {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
	 * 
	 * @exception LifecycleException
	 *                - if this component detects a fatal error that prevents this component from being used
	 */
	@Override
	protected synchronized void stopInternal() throws LifecycleException {
		setState(LifecycleState.STOPPING);
		super.stopInternal();
	}

	@Override
	public Session createSession(String requestedSessionId) {
		RedisSession redisSession = null;
		String sessionId = null;
		String jvmRoute = getJvmRoute();
		if (null != requestedSessionId) {
			sessionId = requestedSessionId;
			if (jvmRoute != null) {
				sessionId += '.' + jvmRoute;
			}
			if (cache.setStringIfKeyNotExists(sessionId.getBytes(), NULL_SESSION) == 0L) {
				sessionId = null;
			}
		} else {
			do {
				sessionId = generateSessionId();
				if (jvmRoute != null) {
					sessionId += '.' + jvmRoute;
				}
			} while (cache.setStringIfKeyNotExists(sessionId.getBytes(), NULL_SESSION) == 0L); // 1 = key set; 0 = key already existed
		}
		if (null != sessionId) {
			redisSession = (RedisSession) createEmptySession();
			redisSession.setNew(true);
			redisSession.setValid(true);
			redisSession.setCreationTime(System.currentTimeMillis());
			redisSession.setMaxInactiveInterval(getMaxInactiveInterval());
			redisSession.setId(sessionId);
			redisSession.tellNew();
		}
		currentSession.set(redisSession);
		currentSessionId.set(sessionId);
		currentSessionIsPersisted.set(false);
		currentSessionSerializationMetadata.set(new SessionSerializationMetadata());
		if (null != redisSession) {
			try {
				save(redisSession, true);
			} catch (Exception e) {
				currentSession.set(null);
				currentSessionId.set(null);
				redisSession = null;
			}
		}
		return redisSession;
	}

	@Override
	public Session createEmptySession() {
		return new RedisSession(this);
	}

	@Override
	public void add(Session session) {
		try {
			save(session);
		} catch (Exception e) {
			log.error("Error occured while adding session", e);
		}
	}

	@Override
	public Session findSession(String id) throws IOException {
		RedisSession redisSession = null;
		if (id == null) {
			currentSessionIsPersisted.set(false);
			currentSession.set(null);
			currentSessionSerializationMetadata.set(null);
			currentSessionId.set(null);
		} else if (id.equals(currentSessionId.get())) {
			redisSession = currentSession.get();
		} else {
			byte[] data = loadSessionDataFromRedis(id);
			if (data != null) {
				DeserializedSessionContainer container = sessionFromSerializedData(id, data);
				redisSession = (RedisSession) container.session;
				currentSession.set(redisSession);
				currentSessionSerializationMetadata.set(container.metadata);
				currentSessionIsPersisted.set(true);
				currentSessionId.set(id);
			} else {
				currentSessionIsPersisted.set(false);
				currentSession.set(null);
				currentSessionSerializationMetadata.set(null);
				currentSessionId.set(null);
			}
		}
		return redisSession;
	}

	public byte[] loadSessionDataFromRedis(String id) throws IOException {
		return cache.getByteArray(id);
	}

	public DeserializedSessionContainer sessionFromSerializedData(String id, byte[] data) throws IOException {
		if (Arrays.equals(NULL_SESSION, data)) {
			throw new IOException("Serialized session data was equal to NULL_SESSION");
		}
		RedisSession requestSession = null;
		SessionSerializationMetadata metadata = null;
		try {
			metadata = new SessionSerializationMetadata();
			requestSession = (RedisSession) createEmptySession();
			serializer.deserializeInto(data, requestSession, metadata);
			requestSession.setId(id);
			requestSession.setNew(false);
			requestSession.setMaxInactiveInterval(getMaxInactiveInterval());
			requestSession.access();
			requestSession.setValid(true);
			requestSession.resetDirtyTracking();
		} catch (Exception e) {
			log.error("Unable to deserialize into session", e);
		}
		return new DeserializedSessionContainer(requestSession, metadata);
	}

	public void save(Session session) throws Exception {
		save(session, false);
	}

	public void save(Session session, boolean forceSave) throws Exception {
		Boolean isCurrentSessionPersisted;
		try {
			RedisSession redisSession = (RedisSession) session;
			byte[] binaryId = redisSession.getId().getBytes();
			SessionSerializationMetadata sessionSerializationMetadata = currentSessionSerializationMetadata.get();
			byte[] originalSessionAttributesHash = sessionSerializationMetadata.getSessionAttributesHash();
			byte[] sessionAttributesHash = null;
			if (forceSave || redisSession.isDirty() || null == (isCurrentSessionPersisted = this.currentSessionIsPersisted.get()) || !isCurrentSessionPersisted || !Arrays.equals(originalSessionAttributesHash, (sessionAttributesHash = serializer.attributesHashFrom(redisSession)))) {
				if (null == sessionAttributesHash) {
					sessionAttributesHash = serializer.attributesHashFrom(redisSession);
				}
				SessionSerializationMetadata updatedSerializationMetadata = new SessionSerializationMetadata();
				updatedSerializationMetadata.setSessionAttributesHash(sessionAttributesHash);
				cache.setByteArray(binaryId, serializer.serializeFrom(redisSession, updatedSerializationMetadata));
				redisSession.resetDirtyTracking();
				currentSessionSerializationMetadata.set(updatedSerializationMetadata);
				currentSessionIsPersisted.set(true);
			}
			cache.expire(binaryId, getMaxInactiveInterval());
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void remove(Session session) {
		remove(session, false);
	}

	@Override
	public void remove(Session session, boolean update) {
		if (cache.isAvailable()) {
			cache.deleteKey(session.getId());
		}
	}

	public void afterRequest() {
		RedisSession redisSession = currentSession.get();
		if (redisSession != null) {
			try {
				if (redisSession.isValid()) {
					log.debug("Request with session completed, saving session " + redisSession.getId());
					save(redisSession, getAlwaysSaveAfterRequest());
				} else {
					log.debug("HTTP Session has been invalidated, removing :" + redisSession.getId());
					remove(redisSession);
				}
			} catch (Exception e) {
				log.error("Error storing/removing session", e);
			} finally {
				currentSession.remove();
				currentSessionId.remove();
				currentSessionIsPersisted.remove();
			}
		}
	}

	private void initializeSerializer() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		serializer = new JavaSerializer();
		Loader loader = null;
		if (getContainer() != null) {
			loader = getContainer().getLoader();
		}
		ClassLoader classLoader = null;
		if (loader != null) {
			classLoader = loader.getClassLoader();
		}
		serializer.setClassLoader(classLoader);
	}

	@Override
	public void load() throws ClassNotFoundException, IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public void unload() throws IOException {
		// TODO Auto-generated method stub
	}

	private Properties getRedisProperties() throws Exception {
		Properties properties = null;
		try {
			if (properties == null || properties.isEmpty()) {
				InputStream resourceStream = null;
				try {
					resourceStream = null;
					properties = new Properties();
					File file = new File(System.getProperty("catalina.home").concat(File.separator).concat("conf").concat(File.separator).concat(RedisConstants.REDIS_PROPERTIES_FILE));
					if (file.exists()) {
						resourceStream = new FileInputStream(file);
					}
					if (resourceStream == null) {
						ClassLoader loader = Thread.currentThread().getContextClassLoader();
						resourceStream = loader.getResourceAsStream(RedisConstants.REDIS_PROPERTIES_FILE);
					}
					properties.load(resourceStream);
				} finally {
					resourceStream.close();
				}
			}
		} catch (IOException e) {
			log.error("Error occurred fetching redis properties", e);
		}
		return properties;
	}
}