/*
 * Copyright 2019-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.org.dockbox.hartshorn.commands;

import org.dockbox.hartshorn.commands.SystemSubject;
import org.dockbox.hartshorn.component.Component;
import org.dockbox.hartshorn.i18n.Message;

import java.util.ArrayList;
import java.util.List;

@Component
public class JUnitSystemSubject extends SystemSubject {

    private final List<Message> received = new ArrayList<>();

    @Override
    public void send(final Message text) {
        this.received.add(text);
    }

    public List<Message> received() {
        return this.received;
    }
}
