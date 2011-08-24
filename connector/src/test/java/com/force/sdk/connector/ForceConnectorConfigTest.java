/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.sdk.connector;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

/**
 * Unit Tests for ForceConnectorConfig.
 *
 * @author Tim Kral
 */
public class ForceConnectorConfigTest {

    @BeforeMethod
    public void clearNamedConnectionsCache() {
        ForceConnectorUtils.clearCache();
    }

    @DataProvider
    protected Object[][] envVariableConnNameProvider() {
        // FORCE_ENVVARCONN_URL is set in pom file
        return new Object[][] {
            {"${FORCE_ENVVARCONN_URL}", true}
        };
    }

    // NOTE: This is not going to pass in an IDE.  You have to execute from the command line.
    @Test(dataProvider = "envVariableConnNameProvider")
    public void testLoadFromEnvVariable(String connectionName) throws Exception {
        ForceConnectorConfig config = ForceConnectorConfig.loadFromName(connectionName);

        assertNotNull(config);
        
        assertEquals(config.getAuthEndpoint(), "https://url" + ForceConnectorUtils.FORCE_API_ENDPOINT_PATH);
        assertEquals(config.getUsername(), "user");
        assertEquals(config.getPassword(), "password");
    }
    
    @DataProvider
    protected Object[][] javaPropertyProvider() {
        return new Object[][] {
            {"force.xyz.url"},
            {"force.xYz.url"},
            {"force.XYZ.url"},
            {"force.xyz1.url"},
            {"force.xyz1.url"},
            {"force.xyz-1.url"},
            {"force.xyz_1.url"},
        };
    }
    
    @Test(dataProvider = "javaPropertyProvider")
    public void testLoadFromJavaProperty(String connectionName) throws Exception {
        try {
            System.setProperty(connectionName, "force://url?user=user@org.com&password=password");
            ForceConnectorConfig config = ForceConnectorConfig.loadFromName("${" + connectionName + "}");
            assertNotNull(config);
            
            assertEquals(config.getAuthEndpoint(), "https://url" + ForceConnectorUtils.FORCE_API_ENDPOINT_PATH);
            assertEquals(config.getUsername(), "user@org.com");
            assertEquals(config.getPassword(), "password");
        } finally {
            System.clearProperty(connectionName);
        }
    }
    
    @Test
    public void testLoadFromJavaPropertyIsCaseSensitive() {
        try {
            System.setProperty("force.xyz.url", "force://url?user=user@org.com&password=password");
            assertNull(ForceConnectorConfig.loadFromName("${FORCE.XYZ.URL}"));
            Assert.fail("IllegalArgumentException was expected as FORCE.XYZ.URL is not " +
                    "set as system property or environment variable");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("Unable to load ForceConnectorConfig from environment or system "));
        } finally {
            System.clearProperty("force.xyz.url");
        }
    }
    
    // NOTE: This is not going to pass in STS.  You have to execute from the command line.
    @Test
    public void testLoadEnvVariableBeforeJavaProperty() throws Exception {
/*        try {
            // Set a Java property that conflicts with the environment variable set in the pom
            System.setProperty("force.connUrlJavaProperty.url", "force://url?user=user&password=password");

            // Try loading from the environment variable.
            // The assertions in that test should still work.
            testLoadFromEnvVariable("${force.envvarconn.url}");
        } finally {
            System.clearProperty("force.envvarconn.url");
        }*/
    }

    @Test
    public void testLoadWithNothingSet() {
        try {
            assertNull(ForceConnectorConfig.loadFromName("${xyz}"));
            Assert.fail("IllegalArgumentException was expected as xyz is not set as system property or environment variable");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("Unable to load ForceConnectorConfig from environment or system "));
        }
    }
    
    @DataProvider
    protected Object[][] badConnectionUrlProvider() {
        return new Object[][] {
            {null},
            {""},
            {"url"},
            {"url?user=user&password=password"},
            {"force://"},
            {"force://url"},
            {"force://url?user=user"},
            {" force://url?user=user&password=password"}, // Leading space
        };
    }
    
    @Test(dataProvider = "badConnectionUrlProvider")
    public void testParseBadConnectionUrl(String connectionUrl) throws Exception {
        ForceConnectorConfig config = new ForceConnectorConfig();
        try {
            // Parse the connectionUrl in the setter
            config.setConnectionUrl(connectionUrl);
            fail("ForceConnectorConfig.setConnectionUrl should have failed with bad connection url.");
        } catch (IllegalArgumentException expected) {
            if (connectionUrl != null) {
                assertTrue(expected.getMessage().contains(connectionUrl));
            } else {
                assertTrue(expected.getMessage().contains("null"));
            }
        }
    }

    @DataProvider
    protected Object[][] connectionUrlWithBadPropertyProvider() {
        return new Object[][] {
            {"force://?user=user&password=password", "endpoint", null},
            {"force://url?user=&password=password", "user", null},
            {"force://url?user=user&password=", "password", null},
            {"force://url/a?user=user&password=", "endpoint", "url/a"},
            {"force://url?user=user&password=password&timeout=abc", "timeout", "abc"},
        };
    }
    
    @Test(dataProvider = "connectionUrlWithBadPropertyProvider")
    public void testParseConnectionUrlWithBadProperty(String connectionUrl, String badProperty, String badValue)
    throws Exception {
        ForceConnectorConfig config = new ForceConnectorConfig();
        try {
            // Parse the connectionUrl in the setter
            config.setConnectionUrl(connectionUrl);
            fail("ForceConnectorConfig.setConnectionUrl should have failed with missing property (" + badProperty + ")");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(badProperty));
            if (badValue != null) {
                assertTrue(expected.getMessage().contains(badValue));
            }
        }
    }
    
    @DataProvider
    protected Object[][] basicGoodConnectionUrlProvider() {
        return new Object[][] {
            {"force://url?user=user&password=password"},
            {"force://url?password=password&user=user"},
            {"force://url/?user=user&password=password"},
            {"force://url?user=user&password=password&testProp"}, // Ignore unknown properties
            {"force://url?user=user&password=password&testProp="}, // Ignore unknown properties
            {"force://url?user=user&password=password&oauth_secret=abc"}, // Ignore unused properties
            {"force://url?user=user&password=password&"}, // Trailing '&'
        };
    }
    
    @Test(dataProvider = "basicGoodConnectionUrlProvider")
    public void testParseBasicGoodConnectionUrl(String connectionUrl) throws Exception {
        
        // Parse the connectionUrl in the setter
        ForceConnectorConfig config = new ForceConnectorConfig();
        config.setConnectionUrl(connectionUrl);
        
        assertEquals(config.getAuthEndpoint(), "https://url" + ForceConnectorUtils.FORCE_API_ENDPOINT_PATH);
        assertEquals(config.getUsername(), "user");
        assertEquals(config.getPassword(), "password");
        assertEquals(config.getClientId(), null);
        assertEquals(config.getReadTimeout(), 0);
    }

    @DataProvider
    protected Object[][] goodConnectionUrlProvider() {
        return new Object[][] {
            {"force://url?user=user&password=password&clientid=id&timeout=1&trace=true",
                    null /* skip validation */, "user", "password", "id", 1},
            {"force://url/services/Soap/u/0?user=user&password=password",
                    "https://url/services/Soap/u/0", "user", "password", null, 0},
            {"force://url?user=user&password=password=",
                    null /* skip validation */, "user", "password=", null, 0}, // Trailing '='
        };
    }
    
    @Test(dataProvider = "goodConnectionUrlProvider")
    public void testParseGoodConnectionUrl(String connectionUrl, String endpoint, String user, String password,
            String clientId, int timeout) throws Exception {
        
        // Parse the connectionUrl in the setter
        ForceConnectorConfig config = new ForceConnectorConfig();
        config.setConnectionUrl(connectionUrl);
        
        // Allow the testcase to skip validation of the url
        // so we don't have to keep building the full API endpoint
        if (endpoint != null) assertEquals(config.getAuthEndpoint(), endpoint);
        assertEquals(config.getUsername(), user);
        assertEquals(config.getPassword(), password);
        assertEquals(config.getClientId(), clientId);
        assertEquals(config.getReadTimeout(), timeout);
    }
    
    @Test
    public void testConfigClone() {
        // TODO: This test could be expanded but for right now
        //       these are the big items we care about.
        ForceConnectorConfig config = new ForceConnectorConfig();
        config.setAuthEndpoint("url");
        config.setUsername("user");
        config.setPassword("password");

        ForceConnectorConfig clonedConfig = (ForceConnectorConfig) config.clone();
        
        assertEquals(clonedConfig.getAuthEndpoint(), config.getAuthEndpoint(),
                "Unexpected AuthEndpoint in cloned ForceConnectorConfig");
        assertEquals(clonedConfig.getUsername(), config.getUsername(),
                "Unexpected Username in cloned ForceConnectorConfig");
        assertEquals(clonedConfig.getPassword(), config.getPassword(),
                "Unexpected Password in cloned ForceConnectorConfig");
        
        assertEquals(clonedConfig.getCacheId(), config.getCacheId(),
                "Unexpected CacheId in cloned ForceConnectorConfig");
    }
}
