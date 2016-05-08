/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */
package org.apache.roller.weblogger.business;

import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.roller.weblogger.business.startup.StartupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates weblogger mail configuration, returns mail sessions.
 */
public class MailProvider {

    private static Logger log = LoggerFactory.getLogger(MailProvider.class);

    private enum ConfigurationType {JNDI_NAME, MAIL_PROPERTIES }
    
    private Session session = null;
    
    private ConfigurationType type = ConfigurationType.JNDI_NAME;
    
    private String mailHostname = null;
    private int    mailPort = -1;
    private String mailUsername = null;
    private String mailPassword = null;

    public MailProvider() throws StartupException {
        
        String connectionTypeString = WebloggerStaticConfig.getProperty("mail.configurationType");
        if ("properties".equals(connectionTypeString)) {
            type = ConfigurationType.MAIL_PROPERTIES;
        }
        String jndiName = WebloggerStaticConfig.getProperty("mail.jndi.name");
        mailHostname = WebloggerStaticConfig.getProperty("mail.hostname");
        mailUsername = WebloggerStaticConfig.getProperty("mail.username");
        mailPassword = WebloggerStaticConfig.getProperty("mail.password");
        try {
            String portString = WebloggerStaticConfig.getProperty("mail.port");
            if (portString != null) {
                mailPort = Integer.parseInt(portString);
            }
        } catch (Exception e) {
            log.warn("mail server port not a valid integer, ignoring");
        }
        
        // init and connect now so we fail early
        if (type == ConfigurationType.JNDI_NAME) {            
            if (jndiName != null && !jndiName.startsWith("java:")) {
                jndiName = "java:comp/env/" + jndiName;
            }
            try {
                Context ctx = new InitialContext();
                session = (Session) ctx.lookup(jndiName);
            } catch (NamingException ex) {
                throw new StartupException("ERROR looking up mail-session with JNDI name: " + jndiName);
            }
        } else {
            Properties props = new Properties();
            props.put("mail.smtp.host", mailHostname);
            if (mailUsername != null && mailPassword != null) {
                props.put("mail.smtp.auth", "true");   
            }
            if (mailPort != -1) {
                props.put("mail.smtp.port", ""+mailPort);
            }
            session = Session.getDefaultInstance(props, null);
        }
        
        try {
            Transport transport = getTransport();
            transport.close();
        } catch (Exception e) {
            throw new StartupException("ERROR connecting to mail server", e);
        }
        
    }
    
    
    /**
     * Get a mail Session.
     */
    public Session getSession() {
        return session;
    }
    
    
    /**
     * Create and connect to transport, caller is responsible for closing transport.
     */
    public Transport getTransport() throws MessagingException {
        
        Transport transport;
        
        if (type == ConfigurationType.MAIL_PROPERTIES) {
            // Configure transport ourselves using mail properties
            transport = session.getTransport("smtp"); 
            if (mailUsername != null && mailPassword != null && mailPort != -1) {
                transport.connect(mailHostname, mailPort, mailUsername, mailPassword); 
            } else if (mailUsername != null && mailPassword != null) {
                transport.connect(mailHostname, mailUsername, mailPassword); 
            } else {
                transport.connect();
            }
        } else {
            // Assume container set things up properly
            transport = session.getTransport(); 
            transport.connect();
        }
        
        return transport;
    }
    
}
