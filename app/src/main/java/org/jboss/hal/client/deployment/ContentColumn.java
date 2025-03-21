/*
 *  Copyright 2022 Red Hat
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.jboss.hal.client.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.jboss.hal.ballroom.dialog.Dialog;
import org.jboss.hal.ballroom.dialog.DialogFactory;
import org.jboss.hal.ballroom.wizard.Wizard;
import org.jboss.hal.client.deployment.DeploymentTasks.AddUnmanagedDeployment;
import org.jboss.hal.client.deployment.DeploymentTasks.CheckDeployment;
import org.jboss.hal.client.deployment.DeploymentTasks.LoadContent;
import org.jboss.hal.client.deployment.DeploymentTasks.UploadOrReplace;
import org.jboss.hal.client.deployment.dialog.AddUnmanagedDialog;
import org.jboss.hal.client.deployment.dialog.CreateEmptyDialog;
import org.jboss.hal.client.deployment.dialog.DeployContentDialog1;
import org.jboss.hal.client.deployment.wizard.DeploymentContext;
import org.jboss.hal.client.deployment.wizard.DeploymentState;
import org.jboss.hal.client.deployment.wizard.NamesStep;
import org.jboss.hal.client.deployment.wizard.UploadContentStep;
import org.jboss.hal.client.shared.uploadwizard.UploadElement;
import org.jboss.hal.config.Environment;
import org.jboss.hal.core.deployment.Content;
import org.jboss.hal.core.deployment.ServerGroupDeployment;
import org.jboss.hal.core.finder.ColumnAction;
import org.jboss.hal.core.finder.ColumnActionFactory;
import org.jboss.hal.core.finder.Finder;
import org.jboss.hal.core.finder.FinderColumn;
import org.jboss.hal.core.finder.ItemAction;
import org.jboss.hal.core.finder.ItemActionFactory;
import org.jboss.hal.core.finder.ItemDisplay;
import org.jboss.hal.core.finder.ItemMonitor;
import org.jboss.hal.core.mvp.Places;
import org.jboss.hal.dmr.Composite;
import org.jboss.hal.dmr.CompositeResult;
import org.jboss.hal.dmr.ModelNode;
import org.jboss.hal.dmr.Operation;
import org.jboss.hal.dmr.ResourceAddress;
import org.jboss.hal.dmr.dispatch.Dispatcher;
import org.jboss.hal.flow.FlowContext;
import org.jboss.hal.flow.Progress;
import org.jboss.hal.flow.Task;
import org.jboss.hal.js.JsHelper;
import org.jboss.hal.meta.AddressTemplate;
import org.jboss.hal.meta.ManagementModel;
import org.jboss.hal.meta.Metadata;
import org.jboss.hal.meta.MetadataRegistry;
import org.jboss.hal.meta.StatementContext;
import org.jboss.hal.meta.security.AuthorisationDecision;
import org.jboss.hal.meta.security.Constraint;
import org.jboss.hal.meta.security.Constraints;
import org.jboss.hal.meta.security.SecurityContextRegistry;
import org.jboss.hal.meta.token.NameTokens;
import org.jboss.hal.resources.Ids;
import org.jboss.hal.resources.Names;
import org.jboss.hal.resources.Resources;
import org.jboss.hal.resources.Strings;
import org.jboss.hal.resources.UIConstants;
import org.jboss.hal.spi.Column;
import org.jboss.hal.spi.Footer;
import org.jboss.hal.spi.Message;
import org.jboss.hal.spi.MessageEvent;
import org.jboss.hal.spi.Requires;

import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;

import elemental2.dom.HTMLElement;
import elemental2.promise.Promise;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jboss.elemento.Elements.span;
import static org.jboss.hal.client.deployment.ContentColumn.CONTENT_ADDRESS;
import static org.jboss.hal.client.deployment.ContentColumn.ROOT_ADDRESS;
import static org.jboss.hal.client.deployment.ContentColumn.SERVER_GROUP_DEPLOYMENT_ADDRESS;
import static org.jboss.hal.client.deployment.wizard.DeploymentState.NAMES;
import static org.jboss.hal.client.deployment.wizard.DeploymentState.UPLOAD;
import static org.jboss.hal.core.finder.FinderColumn.RefreshMode.CLEAR_SELECTION;
import static org.jboss.hal.core.finder.FinderColumn.RefreshMode.RESTORE_SELECTION;
import static org.jboss.hal.dmr.ModelDescriptionConstants.ADD;
import static org.jboss.hal.dmr.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.hal.dmr.ModelDescriptionConstants.CONTENT;
import static org.jboss.hal.dmr.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.hal.dmr.ModelDescriptionConstants.EMPTY;
import static org.jboss.hal.dmr.ModelDescriptionConstants.ENABLED;
import static org.jboss.hal.dmr.ModelDescriptionConstants.EXPLODE;
import static org.jboss.hal.dmr.ModelDescriptionConstants.FULL_REPLACE_DEPLOYMENT;
import static org.jboss.hal.dmr.ModelDescriptionConstants.NAME;
import static org.jboss.hal.dmr.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.hal.dmr.ModelDescriptionConstants.READ_CONTENT;
import static org.jboss.hal.dmr.ModelDescriptionConstants.REMOVE;
import static org.jboss.hal.dmr.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.hal.dmr.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.hal.flow.Flow.sequential;
import static org.jboss.hal.resources.CSS.fontAwesome;
import static org.jboss.hal.resources.CSS.pfIcon;
import static org.jboss.hal.spi.MessageEvent.fire;

/** Column used in domain mode to manage content in the content repository. */
@Column(Ids.CONTENT)
@Requires(value = { ROOT_ADDRESS, CONTENT_ADDRESS, SERVER_GROUP_DEPLOYMENT_ADDRESS }, recursive = false)
public class ContentColumn extends FinderColumn<Content> {

    static final String ROOT_ADDRESS = "/";
    static final String CONTENT_ADDRESS = "/deployment=*";
    static final String SERVER_GROUP_DEPLOYMENT_ADDRESS = "/server-group=*/deployment=*";
    private static final String SPACE = " ";

    static final AddressTemplate CONTENT_TEMPLATE = AddressTemplate.of(CONTENT_ADDRESS);
    private static final AddressTemplate SERVER_GROUP_DEPLOYMENT_TEMPLATE = AddressTemplate.of(
            SERVER_GROUP_DEPLOYMENT_ADDRESS);

    private final Environment environment;
    private final Dispatcher dispatcher;
    private final EventBus eventBus;
    private final Provider<Progress> progress;
    private final MetadataRegistry metadataRegistry;
    private final SecurityContextRegistry securityContextRegistry;
    private final StatementContext statementContext;
    private final Resources resources;

    @Inject
    public ContentColumn(Finder finder,
            ColumnActionFactory columnActionFactory,
            ItemActionFactory itemActionFactory,
            Environment environment,
            Dispatcher dispatcher,
            EventBus eventBus,
            Places places,
            @Footer Provider<Progress> progress,
            MetadataRegistry metadataRegistry,
            SecurityContextRegistry securityContextRegistry,
            StatementContext statementContext,
            Resources resources) {

        super(new FinderColumn.Builder<Content>(finder, Ids.CONTENT, resources.constants().content())
                .itemsProvider(
                        finderContext -> sequential(new FlowContext(progress.get()), singletonList(new LoadContent(dispatcher)))
                                .then(flowContext -> {
                                    List<Content> content = flowContext.pop();
                                    return Promise.resolve(content);
                                }))

                .useFirstActionAsBreadcrumbHandler()
                .pinnable()
                .showCount()
                .withFilter()
                .filterDescription(resources.messages().contentFilterDescription()));

        this.environment = environment;
        this.dispatcher = dispatcher;
        this.eventBus = eventBus;
        this.progress = progress;
        this.metadataRegistry = metadataRegistry;
        this.securityContextRegistry = securityContextRegistry;
        this.statementContext = statementContext;
        this.resources = resources;

        List<ColumnAction<Content>> addActions = new ArrayList<>();
        addActions.add(new ColumnAction.Builder<Content>(Ids.CONTENT_ADD)
                .title(resources.constants().uploadContent())
                .handler(column -> uploadContent())
                .constraint(Constraint.executable(CONTENT_TEMPLATE, ADD))
                .build());
        addActions.add(new ColumnAction.Builder<Content>(Ids.CONTENT_UNMANAGED_ADD)
                .title(resources.messages().addResourceTitle(Names.UNMANAGED_DEPLOYMENT))
                .handler(column -> addUnmanaged())
                .constraint(Constraint.executable(CONTENT_TEMPLATE, ADD))
                .build());
        addActions.add(new ColumnAction.Builder<Content>(Ids.DEPLOYMENT_EMPTY_CREATE)
                .title(resources.constants().deploymentEmptyCreate())
                .handler(column -> createEmpty())
                .constraint(Constraint.executable(CONTENT_TEMPLATE, ADD))
                .build());
        addColumnActions(Ids.CONTENT_ADD_ACTIONS, pfIcon(UIConstants.ADD_CIRCLE_O), resources.constants().add(), addActions);
        addColumnAction(columnActionFactory.refresh(Ids.CONTENT_REFRESH));
        setPreviewCallback(item -> new ContentPreview(this, item, environment, places,
                metadataRegistry.lookup(SERVER_GROUP_DEPLOYMENT_TEMPLATE), resources));

        setItemRenderer(item -> new ItemDisplay<Content>() {
            @Override
            public String getId() {
                return Strings.sanitize(Ids.content(item.getName()));
            }

            @Override
            public String getTitle() {
                return item.getName();
            }

            @Override
            public HTMLElement element() {
                if (!item.getServerGroupDeployments().isEmpty()) {
                    return ItemDisplay.withSubtitle(item.getName(), item.getServerGroupDeployments().stream()
                            .map(ServerGroupDeployment::getServerGroup)
                            .collect(joining(", ")));
                }
                return null;
            }

            @Override
            public String getTooltip() {
                return String.join(", ",
                        item.isExploded() ? resources.constants().exploded() : resources.constants().archived(),
                        item.isManaged() ? resources.constants().managed() : resources.constants().unmanaged());
            }

            @Override
            public HTMLElement getIcon() {
                String icon = item.isExploded() ? fontAwesome("folder-open") : fontAwesome("archive");
                return span().css(icon).element();
            }

            @Override
            public String getFilterData() {
                String status = String.join(SPACE,
                        item.isExploded() ? resources.constants().exploded() : resources.constants().archived(),
                        item.isManaged() ? resources.constants().managed() : resources.constants().unmanaged());
                String deployments = item.getServerGroupDeployments().isEmpty()
                        ? resources.constants().undeployed()
                        : item.getServerGroupDeployments().stream().map(ServerGroupDeployment::getServerGroup)
                                .collect(joining(SPACE));
                return getTitle() + SPACE + status + SPACE + deployments;
            }

            @Override
            public List<ItemAction<Content>> actions() {
                List<ItemAction<Content>> actions = new ArrayList<>();

                // order is: view, (explode), deploy, replace, download, undeploy / remove
                // only managed deployments can read-content
                if (item.isManaged()) {
                    actions.add(itemActionFactory.view(new PlaceRequest.Builder().nameToken(NameTokens.BROWSE_CONTENT)
                            .with(CONTENT, item.getName()).build()));
                }
                if (ManagementModel.supportsExplodeDeployment(environment.getManagementVersion())
                        && item.getServerGroupDeployments().isEmpty() && !item.isExploded()) {
                    actions.add(new ItemAction.Builder<Content>()
                            .title(resources.constants().explode())
                            .handler(itm -> explode(itm))
                            .constraint(Constraint.executable(CONTENT_TEMPLATE, EXPLODE))
                            .build());
                }
                actions.add(new ItemAction.Builder<Content>()
                        .title(resources.constants().deploy())
                        .handler(itm -> deploy(itm))
                        .constraint(Constraint.executable(SERVER_GROUP_DEPLOYMENT_TEMPLATE, ADD))
                        .build());
                if (item.isManaged()) {
                    actions.add(new ItemAction.Builder<Content>()
                            .title(resources.constants().replace())
                            .handler(itm -> replace(itm))
                            .constraint(Constraint.executable(AddressTemplate.ROOT, FULL_REPLACE_DEPLOYMENT))
                            .constraint(Constraint.executable(CONTENT_TEMPLATE, ADD))
                            .build());
                }
                if (ManagementModel.supportsReadContentFromDeployment(environment.getManagementVersion())
                        && item.isManaged() && !item.isExploded()) {
                    ResourceAddress address = new ResourceAddress().add(DEPLOYMENT, item.getName());
                    Operation operation = new Operation.Builder(address, READ_CONTENT).build();
                    actions.add(new ItemAction.Builder<Content>()
                            .title(resources.constants().download())
                            .href(dispatcher.downloadUrl(operation))
                            .constraint(Constraint.executable(CONTENT_TEMPLATE, READ_CONTENT))
                            .build());
                }
                if (item.getServerGroupDeployments().isEmpty()) {
                    actions.add(new ItemAction.Builder<Content>()
                            .title(resources.constants().remove())
                            .handler(itm -> remove(itm))
                            .constraint(Constraint.executable(CONTENT_TEMPLATE, REMOVE))
                            .build());
                } else {
                    actions.add(new ItemAction.Builder<Content>()
                            .title(resources.constants().undeploy())
                            .handler(itm -> undeploy(itm))
                            .constraint(Constraint.executable(SERVER_GROUP_DEPLOYMENT_TEMPLATE, REMOVE))
                            .build());
                }
                return actions;
            }
        });
    }

    @Override
    public void attach() {
        super.attach();
        Constraints deployConstraints = Constraints.and(
                Constraint.executable(AddressTemplate.ROOT, FULL_REPLACE_DEPLOYMENT),
                Constraint.executable(CONTENT_TEMPLATE, ADD));
        if (JsHelper.supportsAdvancedUpload() &&
                AuthorisationDecision.from(environment, securityContextRegistry).isAllowed(deployConstraints)) {
            setOnDrop(event -> DeploymentTasks.upload(this, environment, dispatcher, eventBus, progress,
                    event.dataTransfer.files, resources));
        }
    }

    private void uploadContent() {
        Metadata metadata = metadataRegistry.lookup(CONTENT_TEMPLATE);
        Wizard<DeploymentContext, DeploymentState> wizard = new Wizard.Builder<DeploymentContext, DeploymentState>(
                resources.messages().addResourceTitle(resources.constants().content()), new DeploymentContext())

                .addStep(UPLOAD, new UploadContentStep(resources))
                .addStep(NAMES, new NamesStep(environment, metadata, resources))

                .onBack((context, currentState) -> currentState == NAMES ? UPLOAD : null)
                .onNext((context, currentState) -> currentState == UPLOAD ? NAMES : null)

                .stayOpenAfterFinish()
                .onFinish((wzd, context) -> {
                    String name = context.name;
                    String runtimeName = context.runtimeName;
                    wzd.showProgress(resources.constants().uploadInProgress(),
                            resources.messages().uploadInProgress(name));

                    List<Task<FlowContext>> tasks = Arrays.asList(new CheckDeployment(dispatcher, name),
                            new UploadOrReplace(environment, dispatcher, name, runtimeName, context.file, false));
                    sequential(new FlowContext(progress.get()), tasks)
                            .then(__ -> {
                                refresh(Ids.content(name));
                                wzd.showSuccess(resources.constants().uploadSuccessful(),
                                        resources.messages().uploadSuccessful(name),
                                        resources.messages().view(resources.constants().content()),
                                        cxt -> {
                                            /* nothing to do, content is already selected */ });
                                return null;
                            })
                            .catch_(error -> {
                                wzd.showError(resources.constants().uploadError(),
                                        resources.messages().uploadError(name), String.valueOf(error));
                                return null;
                            });
                })
                .build();
        wizard.show();
    }

    private void addUnmanaged() {
        Metadata metadata = metadataRegistry.lookup(CONTENT_TEMPLATE);
        AddUnmanagedDialog dialog = new AddUnmanagedDialog(metadata, resources,
                (name, model) -> sequential(new FlowContext(progress.get()),
                        singletonList(new AddUnmanagedDeployment(dispatcher, name, model)))
                        .then(context -> {
                            refresh(Ids.content(name));
                            MessageEvent.fire(eventBus, Message.success(
                                    resources.messages().addResourceSuccess(Names.UNMANAGED_DEPLOYMENT, name)));
                            return null;
                        }));
        dialog.getForm().<String> getFormItem(NAME).addValidationHandler(createUniqueValidation());
        dialog.show();
    }

    private void createEmpty() {
        CreateEmptyDialog dialog = new CreateEmptyDialog(resources, name -> {
            ResourceAddress address = CONTENT_TEMPLATE.resolve(statementContext, name);
            ModelNode contentNode = new ModelNode();
            contentNode.get(EMPTY).set(true);
            Operation operation = new Operation.Builder(address, ADD)
                    .param(CONTENT, new ModelNode().add(contentNode))
                    .build();
            dispatcher.execute(operation, result -> {
                refresh(Ids.deployment(name));
                MessageEvent.fire(eventBus, Message.success(resources.messages().deploymentEmptySuccess(name)));
            });
        });
        dialog.addValidationHandlerForNameItem(createUniqueValidation());
        dialog.show();
    }

    private void replace(Content content) {
        UploadElement uploadElement = new UploadElement(resources.messages().noContent());
        Dialog dialog = new Dialog.Builder(resources.constants().replaceContent())
                .add(uploadElement.element())
                .cancel()
                .primary(resources.constants().replace(), () -> {
                    boolean valid = uploadElement.validate();
                    if (valid) {
                        ReplaceDeploymentPanel replaceDeploymentPanel = new ReplaceDeploymentPanel();
                        replaceDeploymentPanel.on();
                        List<Task<FlowContext>> tasks = Arrays.asList(new CheckDeployment(dispatcher, content.getName()),
                                new UploadOrReplace(environment, dispatcher, content.getName(), content.getRuntimeName(),
                                        uploadElement.getFiles().item(0), false));
                        sequential(new FlowContext(progress.get()), tasks)
                                .then(context -> {
                                    refresh(Ids.content(content.getName()));
                                    replaceDeploymentPanel.off();
                                    MessageEvent.fire(eventBus, Message.success(
                                            resources.messages().contentReplaceSuccess(content.getName())));
                                    return null;
                                })
                                .catch_(error -> {
                                    replaceDeploymentPanel.off();
                                    MessageEvent.fire(eventBus, Message.error(
                                            resources.messages().contentReplaceError(content.getName()),
                                            String.valueOf(error)));
                                    return null;
                                });
                    }
                    return valid;
                })
                .build();
        dialog.show();
    }

    private void explode(Content content) {
        Operation operation = new Operation.Builder(contentAddress(content), EXPLODE).build();
        dispatcher.execute(operation, result -> {
            refresh(RESTORE_SELECTION);
            MessageEvent
                    .fire(eventBus, Message.success(resources.messages().deploymentExploded(content.getName())));
        });
    }

    void deploy(Content content) {
        Operation operation = new Operation.Builder(ResourceAddress.root(), READ_CHILDREN_NAMES_OPERATION)
                .param(CHILD_TYPE, SERVER_GROUP)
                .build();
        dispatcher.execute(operation, result -> {
            Set<String> serverGroupsWithoutContent = result.asList().stream()
                    .map(ModelNode::asString)
                    .collect(toSet());
            Set<String> serverGroupsWithContent = content.getServerGroupDeployments().stream()
                    .map(ServerGroupDeployment::getServerGroup)
                    .collect(toSet());
            serverGroupsWithoutContent.removeAll(serverGroupsWithContent);

            if (serverGroupsWithoutContent.isEmpty()) {
                MessageEvent.fire(eventBus, Message.warning(
                        resources.messages().contentAlreadyDeployedToAllServerGroups(content.getName())));

            } else {
                new DeployContentDialog1(content, serverGroupsWithoutContent, resources,
                        (cnt, serverGroups, enable) -> {
                            List<Operation> operations = serverGroups.stream()
                                    .map(serverGroup -> {
                                        ResourceAddress resourceAddress = new ResourceAddress()
                                                .add(SERVER_GROUP, serverGroup)
                                                .add(DEPLOYMENT, content.getName());
                                        return new Operation.Builder(resourceAddress, ADD)
                                                .param(RUNTIME_NAME, content.getRuntimeName())
                                                .param(ENABLED, enable)
                                                .build();
                                    })
                                    .collect(toList());
                            if (enable) {
                                ItemMonitor.startProgress(Ids.content(cnt.getName()));
                            }
                            dispatcher.execute(new Composite(operations), (CompositeResult cr) -> {
                                if (enable) {
                                    ItemMonitor.stopProgress(Ids.content(cnt.getName()));
                                }
                                refresh(CLEAR_SELECTION);
                                MessageEvent.fire(eventBus,
                                        Message.success(resources.messages().contentDeployed1(content.getName())));
                            });
                        }).show();
            }
        });
    }

    private void undeploy(Content content) {
        if (!content.getServerGroupDeployments().isEmpty()) {
            Set<String> serverGroupsWithContent = content.getServerGroupDeployments().stream()
                    .map(ServerGroupDeployment::getServerGroup)
                    .collect(toSet());
            new DeployContentDialog1(content, serverGroupsWithContent, resources, (cnt, serverGroups) -> {
                List<Operation> operations = serverGroups.stream()
                        .map(serverGroup -> {
                            ResourceAddress resourceAddress = new ResourceAddress()
                                    .add(SERVER_GROUP, serverGroup)
                                    .add(DEPLOYMENT, content.getName());
                            return new Operation.Builder(resourceAddress, REMOVE).build();
                        })
                        .collect(toList());
                dispatcher.execute(new Composite(operations), (CompositeResult cr) -> {
                    refresh(RESTORE_SELECTION);
                    MessageEvent.fire(eventBus,
                            Message.success(resources.messages().contentUndeployed(content.getName())));
                });
            }).show();

        } else {
            MessageEvent.fire(eventBus, Message.warning(resources.messages().undeployedContent(content.getName())));
        }
    }

    void undeploy(Content content, String serverGroup) {
        ResourceAddress address = new ResourceAddress().add(SERVER_GROUP, serverGroup)
                .add(DEPLOYMENT, content.getName());
        Operation operation = new Operation.Builder(address, REMOVE).build();
        dispatcher.execute(operation, result -> {
            refresh(RESTORE_SELECTION);
            fire(eventBus, Message.success(
                    resources.messages().contentUndeployedFromServerGroup(content.getName(), serverGroup)));
        });
    }

    private void remove(Content content) {
        DialogFactory.showConfirmation(
                resources.messages().removeConfirmationTitle(content.getName()),
                resources.messages().removeConfirmationQuestion(content.getName()),
                () -> {
                    ResourceAddress address = new ResourceAddress().add(DEPLOYMENT, content.getName());
                    Operation operation = new Operation.Builder(address, REMOVE).build();
                    dispatcher.execute(operation, result -> {
                        fire(eventBus, Message.success(resources.messages()
                                .removeResourceSuccess(resources.constants().content(),
                                        content.getName())));
                        refresh(CLEAR_SELECTION);
                    });
                });
    }

    private ResourceAddress contentAddress(Content content) {
        return new ResourceAddress().add(DEPLOYMENT, content.getName());
    }
}
