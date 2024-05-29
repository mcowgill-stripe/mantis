/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mantisrx.server.master.client.config;

import io.mantisrx.server.core.CoreConfiguration;
import io.mantisrx.server.core.LeaderElectorFactoryCoercer;
import io.mantisrx.server.core.LeaderMonitorFactoryCoercer;
import io.mantisrx.server.core.MetricsCoercer;
import java.util.Properties;
import org.skife.config.ConfigurationObjectFactory;

public class StaticPropertiesConfigurationFactory implements ConfigurationFactory {
    private final ConfigurationObjectFactory delegate;
    private final CoreConfiguration config;
    public StaticPropertiesConfigurationFactory(Properties props) {
        delegate = new ConfigurationObjectFactory(props);
        delegate.addCoercible(new MetricsCoercer(props));
        delegate.addCoercible(new LeaderElectorFactoryCoercer());
        delegate.addCoercible(new LeaderMonitorFactoryCoercer());
//        delegate.addCoercible(new MantisPropertiesCoercer(props));
        config = delegate.build(CoreConfiguration.class);
    }
    @Override
    public CoreConfiguration getConfig() {
        return config;
    }
    @Override
    public String toString() {
        return "StaticPropertiesConfigurationFactory{" +
                "delegate=" + delegate +
                ", config=" + config +
                '}';
    }
}
