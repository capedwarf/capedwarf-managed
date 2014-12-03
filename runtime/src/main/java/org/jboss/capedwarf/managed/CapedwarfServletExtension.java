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
import java.net.URL;

import javax.servlet.ServletContext;

import com.google.apphosting.utils.config.AppEngineWebXml;
import com.google.apphosting.utils.config.AppEngineWebXmlReader;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.SessionManagerFactory;
import org.kohsuke.MetaInfServices;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@MetaInfServices(ServletExtension.class)
public class CapedwarfServletExtension implements ServletExtension {
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        AppEngineWebXml appEngineWebXml = getAppEngineWebXml(deploymentInfo);

        SessionManagerFactory sessionManagerFactory = new CapedwarfSessionManagerFactory(appEngineWebXml);
        deploymentInfo.setSessionManagerFactory(sessionManagerFactory);

        deploymentInfo.addOuterHandlerChainWrapper(new AppEngineHandlerWrapper(appEngineWebXml));
    }

    private static AppEngineWebXml getAppEngineWebXml(DeploymentInfo deployment) {
        try {
            URL url = deployment.getResourceManager().getResource("WEB-INF/appengine-web.xml").getUrl();
            return new CustomAppEngineWebXmlReader(url).readAppEngineWebXml();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
