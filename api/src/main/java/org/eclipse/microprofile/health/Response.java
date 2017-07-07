/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.microprofile.health;

import org.eclipse.microprofile.health.spi.SPIFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The response to a health check invocation.
 * <p>
 * The Response class is reserved for an extension by implementation providers.
 * An application should use one of the static methods to create a Response instance using a ResponseBuilder.
 * </p>
 *
 */
public abstract class Response {

    private static final Logger LOGGER = Logger.getLogger(Response.class.getName());

    public static ResponseBuilder named(String name) {
        ResponseBuilder builder = find(SPIFactory.class).createResponseBuilder();
        return builder.name(name);
    }

    // the actual contract

    public enum State { UP, DOWN }

    public abstract String getName();

    public abstract State getState();

    public abstract Optional<Map<String, Object>> getAttributes();

    private static <T> T find(Class<T> service) {

        T serviceInstance = null;

        // context classloader
        try {
            Iterator<T> iterator = ServiceLoader.load(service, Response.getContextClassLoader()).iterator();

            if (iterator.hasNext()) {
                serviceInstance = iterator.next();
            }
        }
        catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Error loading service " + service.getName() + ".", t);
        }

        // library classloader
        try {
            Iterator<T> iterator = ServiceLoader.load(service, Response.class.getClassLoader()).iterator();

            if (iterator.hasNext()) {
                serviceInstance = iterator.next();
            }
        }
        catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Error loading service " + service.getName() + ".", t);
        }

        if(null==serviceInstance) {
            throw new IllegalStateException("Unable to load service " + service.getName());
        }

        return serviceInstance;

    }


    private static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
            ClassLoader cl = null;
            try {
                cl = Thread.currentThread().getContextClassLoader();
            }
            catch (SecurityException ex) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to get context classloader instance.",
                        ex);
            }
            return cl;
        });
    }
}
