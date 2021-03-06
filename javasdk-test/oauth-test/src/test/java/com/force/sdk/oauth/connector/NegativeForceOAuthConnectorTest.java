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

package com.force.sdk.oauth.connector;

import static org.testng.Assert.*;

import java.io.IOException;

import org.testng.annotations.Test;

import com.force.sdk.oauth.BaseOAuthTest;

/**
 * Negative functional tests for ForceOAuthConnector.
 *
 * @author Tim Kral
 */
public class NegativeForceOAuthConnectorTest extends BaseOAuthTest {

    @Test
    public void testConnectorWithNoConnectionInfo() throws Exception {
        ForceOAuthConnector connector = new ForceOAuthConnector();
        
        try {
            connector.getConnInfo();
            fail("ForceOAuthConnector.getConnInfo() should have failed due to no state");
        } catch (IOException expected) {
            assertNotNull(expected.getMessage());
            assertTrue(expected.getMessage().contains("No state was found to construct an oauth connection."));
            assertTrue(expected.getMessage().contains("Please provide an endpoint, key and secret or connection url."));
        }
    }
    
    @Test
    public void testConnectorWithPartialConnectionInfo() throws Exception {
        ForceOAuthConnectionInfo connInfo = new ForceOAuthConnectionInfo();
        connInfo.setEndpoint(endpoint);
        connInfo.setOauthKey(oauthKey);
        
        ForceOAuthConnector connector = new ForceOAuthConnector();
        connector.setConnectionInfo(connInfo);
        
        try {
            connector.getConnInfo();
            fail("ForceOAuthConnector.getConnInfo() should have failed due to incomplete state");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
            assertTrue(expected.getMessage().contains("The ForceConnectionProperty (oauth_secret) must have a value"));
        }
    }
    
    @Test
    public void testConnectorWithBadName() throws Exception {
        ForceOAuthConnector connector = new ForceOAuthConnector();
        connector.setConnectionName("badConnectionName");
        
        try {
            connector.getConnInfo();
            fail("ForceOAuthConnector.getConnInfo() should have failed due to a bad connection name");
        } catch (IOException expected) {
            assertNotNull(expected.getMessage());
            assertTrue(expected.getMessage()
                        .contains("Or create a classpath properties file, environment variable or java property "
                                    + "for the name 'badConnectionName'"));
        }
    }
}
