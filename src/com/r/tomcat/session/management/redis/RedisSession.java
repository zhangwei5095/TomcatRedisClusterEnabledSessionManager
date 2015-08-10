package com.r.tomcat.session.management.redis;

import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.r.tomcat.session.management.commons.IRequestSession;

/**
 * Tomcat redis session
 *
 * @author Ranjith
 * @since 1.0
 */
public class RedisSession extends StandardSession implements IRequestSession
{
	private Log log = LogFactory.getLog(RedisSession.class);
	
	private static final long serialVersionUID = 2L;
	
	protected Boolean dirty;

	protected HashMap<String, Object> changedAttributes;

	protected static Boolean manualDirtyTrackingSupportEnabled = false;

	protected static String manualDirtyTrackingAttributeKey = "__changed__";

	public static void setManualDirtyTrackingSupportEnabled(Boolean enabled) {
		manualDirtyTrackingSupportEnabled = enabled;
	}

	public static void setManualDirtyTrackingAttributeKey(String key) {
		manualDirtyTrackingAttributeKey = key;
	}

	public RedisSession(Manager manager) {
		super(manager);
		resetDirtyTracking();
	}

	public Boolean isDirty() {
		return dirty || !changedAttributes.isEmpty();
	}

	public HashMap<String, Object> getChangedAttributes() {
		return changedAttributes;
	}

	public void resetDirtyTracking() {
		changedAttributes = new HashMap<>();
		dirty = false;
	}

	@Override
	public void setAttribute(String key, Object value) {
		if (manualDirtyTrackingSupportEnabled && manualDirtyTrackingAttributeKey.equals(key)) {
			dirty = true;
			return;
		}
		Object oldValue = getAttribute(key);
		super.setAttribute(key, value);

		if ((value != null || oldValue != null) && (value == null && oldValue != null || oldValue == null && value != null || !value.getClass().isInstance(oldValue) || !value.equals(oldValue))) {
			if (this.manager instanceof RedisSessionManager && ((RedisSessionManager) this.manager).getSaveOnChange()) {
				try {
					((RedisSessionManager) this.manager).save(this, true);
				} catch (Exception e) {
					log.error("Error saving session on setAttribute (triggered by saveOnChange=true):", e);
				}
			} else {
				changedAttributes.put(key, value);
			}
		}
	}

	@Override
	public Object getAttribute(String name) {
		return super.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return super.getAttributeNames();
	}

	@Override
	public void removeAttribute(String name) {
		super.removeAttribute(name);
		if (this.manager instanceof RedisSessionManager && ((RedisSessionManager) this.manager).getSaveOnChange()) {
			try {
				((RedisSessionManager) this.manager).save(this, true);
			} catch (Exception e) {
				log.error("Error saving session on setAttribute (triggered by saveOnChange=true): ", e);
			}
		} else {
			dirty = true;
		}
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void setPrincipal(Principal principal) {
		dirty = true;
		super.setPrincipal(principal);
	}

	@Override
	public void writeObjectData(java.io.ObjectOutputStream out) throws IOException {
		super.writeObjectData(out);
		out.writeLong(this.getCreationTime());
	}

	@Override
	public void readObjectData(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		super.readObjectData(in);
		this.setCreationTime(in.readLong());
	}
}