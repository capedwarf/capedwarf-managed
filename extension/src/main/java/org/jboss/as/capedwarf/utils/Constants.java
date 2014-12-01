/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.utils;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;

/**
 * Constants.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public final class Constants {
    public static final String CAPEDWARF = "capedwarf";
    public static final ServiceName CAPEDWARF_NAME = ServiceName.JBOSS.append(CAPEDWARF);
    // JNDI names
    static final String JMSXA_JNDI = JndiName.of("java:").append("JmsXA").getAbsoluteName();
    static final String QUEUE_JNDI = JndiName.of("java:").append("queue").append(CAPEDWARF).getAbsoluteName();
    public static final ContextNames.BindInfo JMSXA_BIND_INFO = ContextNames.bindInfoFor(JMSXA_JNDI);
    public static final ContextNames.BindInfo QUEUE_BIND_INFO = ContextNames.bindInfoFor(QUEUE_JNDI);
}
