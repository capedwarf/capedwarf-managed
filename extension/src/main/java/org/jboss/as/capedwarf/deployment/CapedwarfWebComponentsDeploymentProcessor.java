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

package org.jboss.as.capedwarf.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.web.jboss.ContainerListenerMetaData;
import org.jboss.metadata.web.jboss.ContainerListenerType;
import org.jboss.metadata.web.jboss.JBoss80WebMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.AuthConstraintMetaData;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionsMetaData;

/**
 * Add GAE filter and auth servlet.
 * Enable Faces, if not yet configured.
 *
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 * @author <a href="mailto:gregor.sfiligoj@gmail.com">Gregor Sfiligoj</a>
 */
public class CapedwarfWebComponentsDeploymentProcessor extends CapedwarfWebModificationDeploymentProcessor {

    private static final String CAPEDWARF_TGT = "CAPEDWARF";

    private static final String TX_DETECTOR_FILTER_NAME = "AbandonedTransactionDetector";
    private static final String SAVE_SESSION_FILTER_NAME = "SaveSessionFilter";
    private static final String PARSE_BLOB_FILTER_NAME = "_ah_ParseBlobUploadFilter";
    private static final String VM_STOP_FILTER_NAME = "_ah_VmStopFilter";
    private static final String HEALTH_SERVLET_NAME = "_ah_health";
    private static final String SESSION_CLEANUP_SERVLET_NAME = "_ah_sessioncleanup";
    private static final String WARMUP_SERVLET_NAME = "_ah_warmup";
    private static final String QUEUE_DEFERRED_SERVLET_NAME = "_ah_queue_deferred";

    private final FilterMetaData TX_DETECTOR_FILTER;
    private final FilterMetaData SAVE_SESSION_FILTER;
    private final FilterMetaData PARSE_BLOB_FILTER;
    private final FilterMetaData VM_STOP_FILTER;

    private final FilterMappingMetaData TX_DETECTOR_FILTER_MAPPING;
    private final FilterMappingMetaData SAVE_SESSION_FILTER_MAPPING;
    private final FilterMappingMetaData PARSE_BLOB_FILTER_MAPPING;
    private final FilterMappingMetaData VM_STOP_FILTER_MAPPING;

    private final ServletMetaData HEALTH_SERVLET;
    private final ServletMetaData SESSION_CLEANUP_SERVLET;
    private final ServletMetaData WARMUP_SERVLET;
    private final ServletMetaData QUEUE_DEFERRED_SERVLET;

    private final ServletMappingMetaData HEALTH_SERVLET_MAPPING;
    private final ServletMappingMetaData SESSION_CLEANUP_SERVLET_MAPPING;
    private final ServletMappingMetaData WARMUP_SERVLET_MAPPING;
    private final ServletMappingMetaData QUEUE_DEFERRED_SERVLET_MAPPING;

    private final String adminTGT;

    public CapedwarfWebComponentsDeploymentProcessor(String tgt) {
        adminTGT = tgt;

        TX_DETECTOR_FILTER = createFilter(TX_DETECTOR_FILTER_NAME, "com.google.apphosting.utils.servlet.TransactionCleanupFilter");
        SAVE_SESSION_FILTER = createFilter(SAVE_SESSION_FILTER_NAME, "org.jboss.capedwarf.managed.SaveSessionFilter");
        PARSE_BLOB_FILTER = createFilter(PARSE_BLOB_FILTER_NAME, "com.google.apphosting.utils.servlet.ParseBlobUploadFilter");
        VM_STOP_FILTER = createFilter(VM_STOP_FILTER_NAME, "com.google.apphosting.utils.servlet.VmStopFilter");

        TX_DETECTOR_FILTER_MAPPING = createFilterMapping(TX_DETECTOR_FILTER_NAME, "/*");
        SAVE_SESSION_FILTER_MAPPING = createFilterMapping(SAVE_SESSION_FILTER_NAME, "/*");
        PARSE_BLOB_FILTER_MAPPING = createFilterMapping(PARSE_BLOB_FILTER_NAME, "/*");
        VM_STOP_FILTER_MAPPING = createFilterMapping(VM_STOP_FILTER_NAME, "/_ah/stop");

        HEALTH_SERVLET = createServlet(HEALTH_SERVLET_NAME, "com.google.apphosting.utils.servlet.VmHealthServlet");
        SESSION_CLEANUP_SERVLET = createServlet(SESSION_CLEANUP_SERVLET_NAME, "com.google.apphosting.utils.servlet.SessionCleanupServlet");
        WARMUP_SERVLET = createServlet(WARMUP_SERVLET_NAME, "com.google.apphosting.utils.servlet.WarmupServlet");
        QUEUE_DEFERRED_SERVLET = createServlet(QUEUE_DEFERRED_SERVLET_NAME, "com.google.apphosting.utils.servlet.DeferredTaskServlet");

        HEALTH_SERVLET_MAPPING = createServletMapping(HEALTH_SERVLET_NAME, new String[]{"/_ah/health"});
        SESSION_CLEANUP_SERVLET_MAPPING = createServletMapping(SESSION_CLEANUP_SERVLET_NAME, new String[]{"/_ah/sessioncleanup"});
        WARMUP_SERVLET_MAPPING = createServletMapping(WARMUP_SERVLET_NAME, new String[]{"/_ah/warmup"});
        QUEUE_DEFERRED_SERVLET_MAPPING = createServletMapping(QUEUE_DEFERRED_SERVLET_NAME, new String[]{"/_ah/queue/__deferred__"});
    }

    protected boolean isCapedwarfAuth() {
        return CAPEDWARF_TGT.equalsIgnoreCase(adminTGT);
    }

    @Override
    protected void doDeploy(DeploymentUnit unit, WebMetaData webMetaData, Type type) {
        if (type == Type.SPEC) {
            getFilterMappings(webMetaData).add(TX_DETECTOR_FILTER_MAPPING);
            getFilterMappings(webMetaData).add(SAVE_SESSION_FILTER_MAPPING);
            getFilterMappings(webMetaData).add(PARSE_BLOB_FILTER_MAPPING);
            getFilterMappings(webMetaData).add(VM_STOP_FILTER_MAPPING);

            getFilters(webMetaData).add(TX_DETECTOR_FILTER);
            getFilters(webMetaData).add(SAVE_SESSION_FILTER);
            getFilters(webMetaData).add(PARSE_BLOB_FILTER);
            getFilters(webMetaData).add(VM_STOP_FILTER);

            addServletAndMapping(webMetaData, HEALTH_SERVLET, HEALTH_SERVLET_MAPPING);
            addServletAndMapping(webMetaData, SESSION_CLEANUP_SERVLET, SESSION_CLEANUP_SERVLET_MAPPING);
            addServletAndMapping(webMetaData, WARMUP_SERVLET, WARMUP_SERVLET_MAPPING);
            addServletAndMapping(webMetaData, QUEUE_DEFERRED_SERVLET, QUEUE_DEFERRED_SERVLET_MAPPING);

            getSecurityConstraints(webMetaData).add(createServletSecurityConstraint("Queue Deferred", "/_ah/queue/__deferred__", "admin"));

            getSessionConfig(webMetaData).setSessionTimeout(1440);
        }
    }

    protected JBossWebMetaData createJBossWebMetaData(Type type) {
        return (type == Type.JBOSS) ? new JBoss80WebMetaData() : null;
    }

    private static ParamValueMetaData create(String name, String value) {
        ParamValueMetaData pvmd = new ParamValueMetaData();
        pvmd.setParamName(name);
        pvmd.setParamValue(value);
        return pvmd;
    }

    private void addServletAndMapping(WebMetaData webMetaData, ServletMetaData servletMetaData, ServletMappingMetaData servletMappingMetaData) {
        getServlets(webMetaData).add(servletMetaData);
        getServletMappings(webMetaData).add(servletMappingMetaData);
    }

    protected void addContextParamsTo(WebMetaData webMetaData, ParamValueMetaData param) {
        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            contextParams = new ArrayList<>();
            webMetaData.setContextParams(contextParams);
        }
        contextParams.add(param);
    }

    private ListenerMetaData createListener(String listenerClass) {
        ListenerMetaData listener = new ListenerMetaData();
        listener.setListenerClass(listenerClass);
        return listener;
    }

    private List<ListenerMetaData> getListeners(WebMetaData webMetaData) {
        List<ListenerMetaData> listeners = webMetaData.getListeners();
        if (listeners == null) {
            listeners = new ArrayList<>();
            webMetaData.setListeners(listeners);
        }
        return listeners;
    }

    private FilterMetaData createFilter(String filterName, String filterClass) {
        FilterMetaData filter = new FilterMetaData();
        filter.setFilterName(filterName);
        filter.setFilterClass(filterClass);
        return filter;
    }

    private FiltersMetaData getFilters(WebMetaData webMetaData) {
        FiltersMetaData filters = webMetaData.getFilters();
        if (filters == null) {
            filters = new FiltersMetaData();
            webMetaData.setFilters(filters);
        }
        return filters;
    }

    private FilterMappingMetaData createFilterMapping(String filterName, String urlPattern) {
        FilterMappingMetaData filterMapping = new FilterMappingMetaData();
        filterMapping.setFilterName(filterName);
        filterMapping.setUrlPatterns(Collections.singletonList(urlPattern));
        filterMapping.setDispatchers(Arrays.asList(DispatcherType.REQUEST, DispatcherType.FORWARD));
        return filterMapping;
    }

    private List<FilterMappingMetaData> getFilterMappings(WebMetaData webMetaData) {
        List<FilterMappingMetaData> filterMappings = webMetaData.getFilterMappings();
        if (filterMappings == null) {
            filterMappings = new ArrayList<>();
            webMetaData.setFilterMappings(filterMappings);
        }
        return filterMappings;
    }

    private ServletsMetaData getServlets(WebMetaData webMetaData) {
        ServletsMetaData servletsMetaData = webMetaData.getServlets();
        if (servletsMetaData == null) {
            servletsMetaData = new ServletsMetaData();
            webMetaData.setServlets(servletsMetaData);
        }
        return servletsMetaData;
    }

    private ServletMetaData createServlet(String servletName, String servletClass) {
        ServletMetaData servlet = new ServletMetaData();
        servlet.setServletName(servletName);
        servlet.setServletClass(servletClass);
        servlet.setEnabled(true);
        return servlet;
    }

    private List<ServletMappingMetaData> getServletMappings(WebMetaData webMetaData) {
        List<ServletMappingMetaData> servletMappings = webMetaData.getServletMappings();
        if (servletMappings == null) {
            servletMappings = new ArrayList<>();
            webMetaData.setServletMappings(servletMappings);
        }
        return servletMappings;
    }

    private ServletMappingMetaData createServletMapping(String servletName, String[] urlPatterns) {
        ServletMappingMetaData servletMapping = new ServletMappingMetaData();
        servletMapping.setServletName(servletName);
        servletMapping.setUrlPatterns(Arrays.asList(urlPatterns));
        return servletMapping;
    }

    private SecurityConstraintMetaData createServletSecurityConstraint(String displayName, String mapping, String role) {
        SecurityConstraintMetaData scMetaData = new SecurityConstraintMetaData();
        scMetaData.setDisplayName(displayName);
        WebResourceCollectionsMetaData resourceCollections = new WebResourceCollectionsMetaData();
        WebResourceCollectionMetaData resourcePath = new WebResourceCollectionMetaData();
        resourcePath.setUrlPatterns(Arrays.asList(mapping));
        resourceCollections.add(resourcePath);
        scMetaData.setResourceCollections(resourceCollections);

        AuthConstraintMetaData authConstraint = new AuthConstraintMetaData();
        authConstraint.setRoleNames(Arrays.asList(role));
        scMetaData.setAuthConstraint(authConstraint);

        return scMetaData;
    }

    private List<SecurityConstraintMetaData> getSecurityConstraints(WebMetaData webMetaData) {
        List<SecurityConstraintMetaData> securityConstraints = webMetaData.getSecurityConstraints();
        if (securityConstraints == null) {
            securityConstraints = new ArrayList<>();
            webMetaData.setSecurityConstraints(securityConstraints);
        }
        return securityConstraints;
    }

    private SecurityRoleMetaData createServletSecurityRole(String role) {
        SecurityRoleMetaData roleMetaData = new SecurityRoleMetaData();
        roleMetaData.setName(role);
        return roleMetaData;
    }

    private SecurityRolesMetaData getSecurityRoles(WebMetaData webMetaData) {
        SecurityRolesMetaData securityRoles = webMetaData.getSecurityRoles();
        if (securityRoles == null) {
            securityRoles = new SecurityRolesMetaData();
            webMetaData.setSecurityRoles(securityRoles);
        }
        return securityRoles;
    }

    private LoginConfigMetaData createAdminServletLogin() {
        LoginConfigMetaData configMetaData = new LoginConfigMetaData();
        configMetaData.setAuthMethod(isCapedwarfAuth() ? "CAPEDWARF" : "CAPEDWARF,BASIC");
        configMetaData.setRealmName("ApplicationRealm");
        return configMetaData;
    }

    private SessionConfigMetaData getSessionConfig(WebMetaData webMetaData) {
        SessionConfigMetaData sessionConfig = webMetaData.getSessionConfig();
        if (sessionConfig == null) {
            sessionConfig = new SessionConfigMetaData();
            webMetaData.setSessionConfig(sessionConfig);
        }
        return sessionConfig;
    }

    protected List<ContainerListenerMetaData> getContainerListeners(JBossWebMetaData webMetaData) {
        List<ContainerListenerMetaData> cl = webMetaData.getContainerListeners();
        if (cl == null) {
            cl = new ArrayList<>();
            webMetaData.setContainerListeners(cl);
        }
        return cl;
    }

    protected ContainerListenerMetaData createContainerListenerMetaData(String clazz, ContainerListenerType type) {
        ContainerListenerMetaData clmd = new ContainerListenerMetaData();
        clmd.setListenerClass(clazz);
        clmd.setListenerType(type);
        return clmd;
    }
}
