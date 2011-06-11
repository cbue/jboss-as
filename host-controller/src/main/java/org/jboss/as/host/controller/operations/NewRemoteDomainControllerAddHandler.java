/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller.operations;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;
import static org.jboss.as.host.controller.operations.DomainControllerAddUtil.installLocalDomainController;
import static org.jboss.as.host.controller.operations.DomainControllerAddUtil.installRemoteDomainControllerConnection;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.controller.DomainModelImpl;
import org.jboss.as.domain.controller.DomainModelUtil;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.host.controller.DomainModelProxy;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.LocalFileRepository;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @version $Revision: 1.1 $
 */
public class NewRemoteDomainControllerAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = "write-remote-domain-controller";

    private final ParametersValidator parametersValidator = new ParametersValidator();

    private final ModelNodeRegistration rootRegistration;
    private final DomainModelProxy domainModelProxy;
    private final HostControllerEnvironment environment;
    private final ExtensibleConfigurationPersister domainConfigPersister;
    private final ContentRepository contentRepo;
    private final FileRepository fileRepository;

    public static NewRemoteDomainControllerAddHandler getInstance(final ModelNodeRegistration rootRegistration,
                                                                 final DomainModelProxy domainModelProxy,
                                                                 final HostControllerEnvironment environment,
                                                                 final ExtensibleConfigurationPersister domainConfigPersister,
                                                                 final ContentRepository contentRepo,
                                                                 final FileRepository fileRepository) {
        return new NewRemoteDomainControllerAddHandler(rootRegistration, domainModelProxy, environment, domainConfigPersister, contentRepo, fileRepository);
    }

    /**
     * Create the ServerAddHandler
     */
    NewRemoteDomainControllerAddHandler(final ModelNodeRegistration rootRegistration, DomainModelProxy domainModelProxy,
                                        final HostControllerEnvironment environment, final ExtensibleConfigurationPersister domainConfigPersister,
                                        final ContentRepository contentRepo, final FileRepository fileRepository) {
        this.domainModelProxy = domainModelProxy;
        this.environment = environment;
        this.rootRegistration = rootRegistration;
        this.domainConfigPersister = domainConfigPersister;
        this.contentRepo = contentRepo;
        this.fileRepository = fileRepository;
        this.parametersValidator.registerValidator(PORT, new IntRangeValidator(1, 65535, false, true));
        this.parametersValidator.registerValidator(HOST, new StringLengthValidator(1, Integer.MAX_VALUE, false, true));
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(DOMAIN_CONTROLLER, REMOTE, PORT).set(operation.require(PORT));
        model.get(DOMAIN_CONTROLLER, REMOTE, HOST).set(operation.require(HOST));
        DomainModelUtil.initializeSlaveDomainRegistry(rootRegistration, domainConfigPersister, contentRepo, fileRepository, domainModelProxy.getDomainModel());
    }

    protected void performRuntime(NewOperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final DomainModelImpl domainModel = domainModelProxy.getDomainModel();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final FileRepository fileRepository = new LocalFileRepository(environment);
        installRemoteDomainControllerConnection(domainModel.getHostModel(), serviceTarget, fileRepository);
        newControllers.addAll(installLocalDomainController(environment, domainModel.getHostModel(), serviceTarget, true, fileRepository, domainModelProxy.getDomainModel(), verificationHandler));
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        // TODO - Add the ModelDescription
        return new ModelNode();
    }
}