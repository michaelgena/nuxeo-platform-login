/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     François Maturel
 */

package org.nuxeo.ecm.platform.ui.web.keycloak;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.codehaus.jackson.map.ObjectMapper;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.SystemPropertiesJsonParserFactory;

/**
 * This class is developed to overcome a Jackson version problem between Nuxeo and Keycloak.<br>
 * Nuxeo uses Jackson version 1.8.x where Keycloak uses 1.9.x<br>
 * Sadly the {@link ObjectMapper#setSerializationInclusion} method is not in 1.8.x<br>
 * Then {@link KeycloakNuxeoDeployment} is the same class as {@link KeycloakDeploymentBuilder}, rewriting static method
 * {@link KeycloakDeploymentBuilder#loadAdapterConfig} to avoid the use of
 * {@link ObjectMapper#setSerializationInclusion}
 *
 * @since 7.4
 */
public class KeycloakNuxeoDeployment {

    /**
     * Invokes KeycloakDeploymentBuilder.internalBuild with reflection to avoid rewriting source code
     *
     * @param is the configuration file {@link InputStream}
     * @return the {@link KeycloakDeployment} corresponding to the configuration file
     */
    public static KeycloakDeployment build(InputStream is) {
        AdapterConfig adapterConfig = loadAdapterConfig(is);

        try {
            Constructor<KeycloakDeploymentBuilder> constructor = KeycloakDeploymentBuilder.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            KeycloakDeploymentBuilder builder = constructor.newInstance();

            Method method = KeycloakDeploymentBuilder.class.getDeclaredMethod("internalBuild", AdapterConfig.class);
            method.setAccessible(true);
            return (KeycloakDeployment) method.invoke(builder, adapterConfig);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static AdapterConfig loadAdapterConfig(InputStream is) {
        ObjectMapper mapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());
        AdapterConfig adapterConfig;
        try {
            adapterConfig = mapper.readValue(is, AdapterConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return adapterConfig;
    }

}
