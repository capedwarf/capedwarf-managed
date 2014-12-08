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

package org.jboss.as.capedwarf.deployment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;

/**
 * Add runtime and managed jars to classpath.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfClasspathDeploymentUnitProcessor extends CapedwarfDeploymentUnitProcessor {

    private static final FilenameFilter JARS_SDK = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };

    private List<ResourceLoaderSpec> capedwarfResources;

    protected void doDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();

        final ModuleSpecification moduleSpecification = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        // add CapeDwarf resources directly as libs
        for (ResourceLoaderSpec rls : getCapedwarfResources()) {
            moduleSpecification.addResourceLoader(rls);
        }
    }

    protected synchronized List<ResourceLoaderSpec> getCapedwarfResources() throws DeploymentUnitProcessingException {
        if (capedwarfResources == null) {
            final String path = "org/jboss/capedwarf/runtime/";
            try {
                final List<File> mps = getModulePaths();
                final List<File> jars = findJars(path, mps);
                if (jars.isEmpty()) {
                    throw new DeploymentUnitProcessingException(String.format("No jars found under %s", path));
                }

                capedwarfResources = new ArrayList<>();
                for (File jar : jars) {
                    final JarFile jarFile = new JarFile(jar);
                    final ResourceLoader rl = ResourceLoaders.createJarResourceLoader(jar.getName(), jarFile);
                    capedwarfResources.add(ResourceLoaderSpec.createResourceLoaderSpec(rl));
                }
            } catch (DeploymentUnitProcessingException e) {
                throw e;
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }
        return capedwarfResources;
    }

    protected static List<File> getModulePaths() {
        final List<File> mps;
        final String modulePaths = System.getProperty("module.path");
        if (modulePaths == null) {
            mps = Collections.singletonList(new File(System.getProperty("jboss.home.dir"), "modules"));
        } else {
            mps = new ArrayList<>();
            for (String s : modulePaths.split(":"))
                mps.add(new File(s));
        }
        return mps;
    }

    protected List<File> findJars(String path, List<File> mps) {
        final List<File> results = new ArrayList<>();
        final Set<String> existing = new HashSet<>();
        for (File mp : mps) {
            findJars(path, mp, results, existing);
        }
        return results;
    }

    protected void findJars(String path, File mp, List<File> results, Set<String> existing) {
        final File cdModules = new File(mp, path + "main");
        if (cdModules != null) {
            for (File jar : cdModules.listFiles(JARS_SDK)) {
                if (existing.add(jar.getName())) {
                    results.add(jar);
                }
            }
        }
    }
}
