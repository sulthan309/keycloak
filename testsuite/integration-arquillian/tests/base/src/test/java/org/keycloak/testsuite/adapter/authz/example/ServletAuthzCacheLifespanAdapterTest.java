/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
package org.keycloak.testsuite.adapter.authz.example;

import java.io.File;
import java.io.IOException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
public class ServletAuthzCacheLifespanAdapterTest extends AbstractServletAuthzAdapterTest {

    @Deployment(name = RESOURCE_SERVER_ID, managed = false)
    public static WebArchive deployment() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID)
                .addAsWebInfResource(new File(TEST_APPS_HOME_DIR + "/servlet-authz-app/keycloak-cache-lifespan-authz-service.json"), "keycloak.json");
    }

    @Test
    public void testCreateNewResourceWaitExpiration() {
        performTests(() -> {
            login("alice", "alice");
            assertWasNotDenied();

            this.driver.navigate().to(getResourceServerUrl() + "/new-resource");
            assertWasNotDenied();

            ResourceRepresentation resource = new ResourceRepresentation();

            resource.setName("New Resource");
            resource.setUri("/new-resource");

            getAuthorizationResource().resources().create(resource);

            ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

            permission.setName(resource.getName() + " Permission");
            permission.addResource(resource.getName());
            permission.addPolicy("Deny Policy");

            getAuthorizationResource().permissions().resource().create(permission).readEntity(ResourcePermissionRepresentation.class);

            login("alice", "alice");
            assertWasNotDenied();

            this.driver.navigate().to(getResourceServerUrl() + "/new-resource");
            assertWasNotDenied();

            //Thread.sleep(5000);
            setTimeOffset(30);
            setTimeOffsetOfAdapter(30);

            login("alice", "alice");
            assertWasNotDenied();

            this.driver.navigate().to(getResourceServerUrl() + "/new-resource");
            assertWasDenied();

            resetTimeOffset();
            setTimeOffsetOfAdapter(0);
        });
    }

    public void setTimeOffsetOfAdapter(int offset) {
        this.driver.navigate().to(getResourceServerUrl() + "/timeOffset.jsp?offset=" + String.valueOf(offset));
    }
}
