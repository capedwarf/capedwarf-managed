/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.capedwarf.managed;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.runtime.SessionData;
import com.google.apphosting.runtime.SessionStore;
import com.google.apphosting.runtime.jetty9.DatastoreSessionStore;
import com.google.apphosting.runtime.jetty9.DeferredDatastoreSessionStore;
import com.google.apphosting.runtime.jetty9.MemcacheSessionStore;
import com.google.apphosting.utils.config.AppEngineWebXml;
import com.google.apphosting.utils.config.AppEngineWebXmlReader;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SecureRandomSessionIdGenerator;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionIdGenerator;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineSessionManager implements SessionManager {
    private static final Logger logger = Logger.getLogger(AppEngineSessionManager.class.getName());

    static final String SESSION_PREFIX = "_ahs";

    private SessionIdGenerator sessionIdGenerator = new SecureRandomSessionIdGenerator();
    private volatile int defaultSessionTimeout = 30 * 60;

    private final SessionListeners sessionListeners = new SessionListeners();

    private final Deployment deployment;

    private final List<SessionStore> sessionStoresInWriteOrder;
    private final List<SessionStore> sessionStoresInReadOrder;

    private static List<SessionStore> createSessionStores(AppEngineWebXml appEngineWebXml) {
        DatastoreSessionStore datastoreSessionStore =
            appEngineWebXml.getAsyncSessionPersistence() ? new DeferredDatastoreSessionStore(
                appEngineWebXml.getAsyncSessionPersistenceQueueName())
                : new DatastoreSessionStore();
        return Arrays.asList(datastoreSessionStore, new MemcacheSessionStore());
    }

    public AppEngineSessionManager(Deployment deployment) {
        this.deployment = deployment;
        AppEngineWebXmlReader reader = new CustomAppEngineWebXmlReader(getAppEngineWebXml());
        AppEngineWebXml appEngineWebXml = reader.readAppEngineWebXml();
        sessionStoresInWriteOrder = createSessionStores(appEngineWebXml);
        sessionStoresInReadOrder = new ArrayList<>(sessionStoresInWriteOrder);
        Collections.reverse(sessionStoresInReadOrder);
    }

    private URL getAppEngineWebXml() {
        try {
            return deployment.getDeploymentInfo().getResourceManager().getResource("WEB-INF/appengine-web.xml").getUrl();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    SessionListeners getSessionListeners() {
        return sessionListeners;
    }

    public String getDeploymentName() {
        return deployment.getDeploymentInfo().getDeploymentName();
    }

    public void start() {
    }

    public void stop() {
    }

    public Session createSession(HttpServerExchange serverExchange, SessionConfig sessionCookieConfig) {
        long time = System.currentTimeMillis();
        AppEngineSession session = new AppEngineSession(this, createId(serverExchange, sessionCookieConfig), time, time);
        sessionListeners.sessionCreated(session, serverExchange);
        return session;
    }

    public Session getSession(HttpServerExchange serverExchange, SessionConfig sessionCookieConfig) {
        return getSession(sessionCookieConfig.findSessionId(serverExchange));
    }

    public Session getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        SessionData data = loadSession(sessionId);
        if (data != null) {
            long time = System.currentTimeMillis();
            return new AppEngineSession(this, sessionId, data, time, time);
        } else {
            return null;
        }
    }

    public void registerSessionListener(SessionListener listener) {
        sessionListeners.addSessionListener(listener);
    }

    public void removeSessionListener(SessionListener listener) {
        sessionListeners.removeSessionListener(listener);
    }

    public void setDefaultSessionTimeout(int timeout) {
        defaultSessionTimeout = timeout;
    }

    public Set<String> getTransientSessions() {
        return Collections.emptySet(); // none are transient
    }

    public Set<String> getActiveSessions() {
        return null; // TODO
    }

    public Set<String> getAllSessions() {
        return null; // TODO
    }

    String createId(HttpServerExchange exchange, SessionConfig config) {
        String sessionID = config.findSessionId(exchange);
        int count = 0;
        while (sessionID == null) {
            sessionID = sessionIdGenerator.createSessionId();
            if(loadSession(sessionID) != null) {
                sessionID = null;
            }
            if(count++ == 100) {
                throw new IllegalStateException("Cannot generate session id!");
            }
        }
        return sessionID;
    }

    SessionData createSession(String sessionId) {
        SessionData data = new SessionData();
        data.setExpirationTime(System.currentTimeMillis() + getSessionExpirationInMilliseconds());
        saveSession(sessionId, data);
        return data;
    }

    void saveSession(String sessionId, SessionData data) {
        String key = SESSION_PREFIX + sessionId;
        for (SessionStore sessionStore : sessionStoresInWriteOrder) {
            try {
                sessionStore.saveSession(key, data);
            } catch (SessionStore.Retryable retryable) {
                throw retryable.getCause();
            }
        }
    }

    SessionData loadSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;

        SessionData data = null;
        for (SessionStore sessionStore : sessionStoresInReadOrder) {
            try {
                data = sessionStore.getSession(key);
                if (data != null) {
                    break;
                }
            } catch (RuntimeException e) {
                String msg = "Exception while loading session data";
                logger.log(Level.WARNING, msg, e);
                if (ApiProxy.getCurrentEnvironment() != null) {
                    ApiProxy.log(createWarningLogRecord(msg, e));
                }
                break;
            }
        }
        if (data != null) {
            if (System.currentTimeMillis() > data.getExpirationTime()) {
                logger.fine("Session " + sessionId + " expired " + ((System.currentTimeMillis() - data.getExpirationTime()) / 1000) + " seconds ago, ignoring.");
                return null;
            }
        }
        return data;
    }

    private long getSessionExpirationInMilliseconds() {
        long seconds = defaultSessionTimeout;
        if (seconds < 0) {
            return Integer.MAX_VALUE * 1000L;
        } else {
            return seconds * 1000;
        }
    }

    private ApiProxy.LogRecord createWarningLogRecord(String message, Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println(message);
        if (ex != null) {
            ex.printStackTrace(printWriter);
        }

        return new ApiProxy.LogRecord(ApiProxy.LogRecord.Level.warn, System.currentTimeMillis() * 1000, stringWriter.toString());
    }

    private static class CustomAppEngineWebXmlReader extends AppEngineWebXmlReader {
        private URL url;

        public CustomAppEngineWebXmlReader(URL appengineWebXml) {
            super("");
            this.url = appengineWebXml;
        }

        protected InputStream getInputStream() {
            try {
                return url.openStream();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
