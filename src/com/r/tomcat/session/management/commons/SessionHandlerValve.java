package com.r.tomcat.session.management.commons;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/**
 * Tomcat session handler valve
 *
 * @author Ranjith
 * @since 1.0
 */
public class SessionHandlerValve extends ValveBase
{
	private IRequestSessionManager manager;

	public void setRedisSessionManager(IRequestSessionManager manager) {
		this.manager = manager;
	}

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		try {
			getNext().invoke(request, response);
		} finally {
			manager.afterRequest();
		}
	}
}