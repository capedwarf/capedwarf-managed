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

package org.jboss.as.capedwarf.extension;

import java.util.List;

import org.jboss.as.capedwarf.deployment.CapedwarfInitializationProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfWebComponentsDeploymentProcessor;
import org.jboss.as.capedwarf.utils.Constants;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
class CapedwarfSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final CapedwarfSubsystemAdd INSTANCE = new CapedwarfSubsystemAdd();

    private boolean initialized;

    private CapedwarfSubsystemAdd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        CapedwarfDefinition.APPENGINE_API.validateAndSet(operation, model);
        CapedwarfDefinition.ADMIN_TGT.validateAndSet(operation, model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performBoottime(final OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers)
        throws OperationFailedException {

        final ModelNode appEngineModel = CapedwarfDefinition.APPENGINE_API.resolveModelAttribute(context, model);
        final String appengineAPI = appEngineModel.isDefined() ? appEngineModel.asString() : null;

        final ModelNode adminTGTModel = CapedwarfDefinition.ADMIN_TGT.resolveModelAttribute(context, model);
        final String adminTGT = adminTGTModel.isDefined() ? adminTGTModel.asString() : null;

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                final ServiceTarget serviceTarget = context.getServiceTarget();

                final int initialStructureOrder = Math.max(Math.max(Phase.STRUCTURE_WAR, Phase.STRUCTURE_WAR_DEPLOYMENT_INIT), Phase.STRUCTURE_EAR);
                processorTarget.addDeploymentProcessor(Constants.CAPEDWARF, Phase.STRUCTURE, initialStructureOrder + 10, new CapedwarfInitializationProcessor());
                processorTarget.addDeploymentProcessor(Constants.CAPEDWARF, Phase.PARSE, Phase.PARSE_WEB_COMPONENTS - 1, new CapedwarfWebComponentsDeploymentProcessor(adminTGT));
            }
        }, OperationContext.Stage.RUNTIME);

    }
}
