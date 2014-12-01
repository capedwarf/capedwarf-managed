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
import org.jboss.metadata.web.spec.TransportGuaranteeType;
import org.jboss.metadata.web.spec.UserDataConstraintMetaData;
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

    private static final String TX_DETECTOR_FILTER_NAME = "AbandonedTransactionDetector";
    private static final String CAPEDWARF_TGT = "CAPEDWARF";

    private final FilterMetaData TX_DETECTOR_FILTER;
    private final FilterMappingMetaData TX_DETECTOR_FILTER_MAPPING;
//    private final ServletMetaData AUTH_SERVLET;
//    private final ServletMappingMetaData AUTH_SERVLET_MAPPING;

    private final String adminTGT;

    public CapedwarfWebComponentsDeploymentProcessor(String tgt) {
        adminTGT = tgt;

        TX_DETECTOR_FILTER = createFilter(TX_DETECTOR_FILTER_NAME, "org.jboss.capedwarf.appidentity.GAEFilter");
        TX_DETECTOR_FILTER_MAPPING = createFilterMapping(TX_DETECTOR_FILTER_NAME, "/*");
    }

    protected boolean isCapedwarfAuth() {
        return CAPEDWARF_TGT.equalsIgnoreCase(adminTGT);
    }

    @Override
    protected void doDeploy(DeploymentUnit unit, WebMetaData webMetaData, Type type) {
        if (type == Type.SPEC) {
            getFilterMappings(webMetaData).add(0, TX_DETECTOR_FILTER_MAPPING);
            getFilters(webMetaData).add(TX_DETECTOR_FILTER);
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

    private SecurityConstraintMetaData createAdminServletSecurityConstraint() {
        SecurityConstraintMetaData scMetaData = new SecurityConstraintMetaData();
        scMetaData.setDisplayName("CapeDwarf admin console.");
        WebResourceCollectionsMetaData resourceCollections = new WebResourceCollectionsMetaData();
        WebResourceCollectionMetaData resourcePath = new WebResourceCollectionMetaData();
        // resourcePath.setUrlPatterns(Arrays.asList(ADMIN_SERVLET_URL_MAPPING));
        resourceCollections.add(resourcePath);
        scMetaData.setResourceCollections(resourceCollections);

        AuthConstraintMetaData authConstraint = new AuthConstraintMetaData();
        authConstraint.setRoleNames(Arrays.asList("admin"));
        scMetaData.setAuthConstraint(authConstraint);

        if (adminTGT != null && isCapedwarfAuth() == false) {
            UserDataConstraintMetaData userDataConstraint = new UserDataConstraintMetaData();
            userDataConstraint.setTransportGuarantee(TransportGuaranteeType.valueOf(adminTGT));
            scMetaData.setUserDataConstraint(userDataConstraint);
        }

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

    private SecurityRoleMetaData createAdminServletSecurityRole() {
        SecurityRoleMetaData roleMetaData = new SecurityRoleMetaData();
        roleMetaData.setName("admin");
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
