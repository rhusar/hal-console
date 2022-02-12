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
package org.jboss.hal.ballroom.form;

import org.jboss.hal.ballroom.LabelBuilder;

public class PreTextItem extends TextBoxItem {

    public PreTextItem(String name) {
        super(name, new LabelBuilder().label(name));

        // replace read-only appearance
        addAppearance(Form.State.READONLY, new PreReadOnlyAppearance<>());
    }
}
