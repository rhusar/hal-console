/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.hal.client.runtime.subsystem.webservice;

import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;

import com.google.gwt.user.client.rpc.AsyncCallback;
import elemental2.dom.HTMLElement;
import org.jboss.hal.core.deployment.DeploymentResource;
import org.jboss.hal.core.deployment.DeploymentResources;
import org.jboss.hal.core.finder.ColumnActionFactory;
import org.jboss.hal.core.finder.Finder;
import org.jboss.hal.core.finder.FinderColumn;
import org.jboss.hal.core.finder.FinderContext;
import org.jboss.hal.core.finder.ItemDisplay;
import org.jboss.hal.dmr.dispatch.Dispatcher;
import org.jboss.hal.resources.Ids;
import org.jboss.hal.resources.Names;
import org.jboss.hal.resources.Resources;
import org.jboss.hal.spi.AsyncColumn;

import static org.jboss.hal.core.Strings.abbreviateFqClassName;
import static org.jboss.hal.dmr.ModelDescriptionConstants.ENDPOINT;
import static org.jboss.hal.dmr.ModelDescriptionConstants.WEBSERVICES;
import static org.jboss.hal.resources.CSS.breakTooltip;

@AsyncColumn(Ids.ENDPOINT)
public class EndpointColumn extends FinderColumn<DeploymentResource> {

    @Inject
    public EndpointColumn(final Finder finder,
            final ColumnActionFactory columnActionFactory,
            final Dispatcher dispatcher,
            final DeploymentResources deploymentResources, final Resources resources) {

        super(new Builder<DeploymentResource>(finder, Ids.ENDPOINT, Names.ENDPOINT)

                .columnAction(columnActionFactory.refresh(Ids.ENDPOINT_REFRESH))

                .itemsProvider((FinderContext context, AsyncCallback<List<DeploymentResource>> callback) -> {
                    deploymentResources.readChildren(WEBSERVICES, ENDPOINT,
                            (address, modelNode) -> {
                                DeploymentResource deploymentResource = new DeploymentResource(address, modelNode);
                                deploymentResource.setName(deploymentResource.getName().replaceAll("%3A", ":"));
                                return deploymentResource;

                            }, endpoints -> {
                                endpoints.sort(Comparator.comparing(DeploymentResource::getName));
                                callback.onSuccess(endpoints);
                            });
                })

                .itemRenderer(item -> new ItemDisplay<DeploymentResource>() {
                    @Override
                    public String getId() {
                        return Ids.restResource(item.getDeployment(), item.getSubdeployment(), item.getName());
                    }

                    @Override
                    public String getTitle() {
                        return abbreviateFqClassName(item.getName());
                    }

                    @Override
                    public HTMLElement asElement() {
                        HTMLElement element = ItemDisplay
                                .withSubtitle(abbreviateFqClassName(item.getName()),
                                        item.getPath());
                        element.classList.add(breakTooltip);
                        return element;
                    }

                    @Override
                    public String getFilterData() {
                        return String.join(" ", item.getName(), item.getPath());
                    }

                    @Override
                    public String getTooltip() {
                        return item.getName();
                    }
                })

                .onPreview(item -> new EndpointPreview(item, dispatcher, resources))
                .useFirstActionAsBreadcrumbHandler()
                .withFilter()
                .showCount()
                .pinnable());
    }
}
