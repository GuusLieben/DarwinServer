/*
 * Copyright 2019-2022 the original author or authors.
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

package org.dockbox.hartshorn.reporting.system;

import org.dockbox.hartshorn.reporting.DiagnosticsReporter;
import org.dockbox.hartshorn.reporting.DiagnosticsPropertyCollector;

public class JavaDiagnosticsReporter implements DiagnosticsReporter {

    @Override
    public void report(final DiagnosticsPropertyCollector collector) {
        collector.property("version").write(System.getProperty("java.version"));
        collector.property("vendor").write(vendorCollector -> {
            vendorCollector.property("name").write(System.getProperty("java.vendor"));
            vendorCollector.property("url").write(System.getProperty("java.vendor.url"));
        });
    }
}
