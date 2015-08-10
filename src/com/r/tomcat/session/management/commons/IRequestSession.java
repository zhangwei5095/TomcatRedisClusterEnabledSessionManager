package com.r.tomcat.session.management.commons;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * IRequest session
 *
 * @author Ranjith
 * @since 1.0
 */
public interface IRequestSession
{
	public Boolean isDirty();

	public HashMap<String, Object> getChangedAttributes();

	public void resetDirtyTracking();

	public void setAttribute(String name, Object value);

	public Object getAttribute(String name);

	public Enumeration<String> getAttributeNames();

	public void removeAttribute(String name);

	public void setId(String id);

	public void setPrincipal(Principal principal);

	public void writeObjectData(ObjectOutputStream out) throws IOException;

	public void readObjectData(ObjectInputStream in) throws IOException, ClassNotFoundException;
}