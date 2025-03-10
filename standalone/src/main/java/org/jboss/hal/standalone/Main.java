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
package org.jboss.hal.standalone;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

@ApplicationScoped
public class Main {

    public void init(@Observes Router router) {
        StaticHandler noCache = StaticHandler.create().setCachingEnabled(false);
        StaticHandler staticHandler = StaticHandler.create();

        router.getWithRegex(".*nocache.*").order(0).handler(noCache);
        router.get().order(1).handler(staticHandler);
    }
}
