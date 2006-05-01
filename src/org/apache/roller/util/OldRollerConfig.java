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
*/
package org.apache.roller.util;import java.beans.IntrospectionException;import java.io.File;import java.io.FileInputStream;import java.io.FileNotFoundException;import java.io.FileOutputStream;import java.io.IOException;import java.io.InputStream;import java.io.OutputStream;import java.lang.reflect.AccessibleObject;import java.lang.reflect.Field;import java.util.ArrayList;import java.util.Arrays;import java.util.List;import org.apache.commons.betwixt.io.BeanReader;import org.apache.commons.betwixt.io.BeanWriter;import org.apache.commons.logging.Log;import org.apache.commons.logging.LogFactory;import org.apache.roller.RollerException;import org.apache.roller.pojos.RollerConfigData;import org.xml.sax.SAXException;/** * Configuration object for Roller.  Reads and writes roller-config.xml. * This file is a relic of the old days, back when we used to store the Roller * configuration in an XML file. In Roller 0.9.9 and later, this file only  * exists to allow use to read only roller-config.xml files on startup and * copy them into the database.  */public class OldRollerConfig implements java.io.Serializable{    static final long serialVersionUID = -6625873343838437510L;        private static Log mLogger =        LogFactory.getFactory().getInstance( OldRollerConfig.class );    /**     * Absolute URL for site, for cases where infered absolute URL doesn't     * work.     */    protected String mAbsoluteURL = null;    /** Should Roller cache return RSS pages. */    protected boolean mRssUseCache = false;    /** Duration to cache RSS pages (in seconds). */    protected int mRssCacheTime = 3000;    /** Does Roller allow the creation of new users. */    protected boolean mNewUserAllowed = false;    /** List of usernames with Admin priviledges. */    protected List mAdminUsers = new ArrayList();    /** Where to get data for creating new users (new-user.xml). */    protected String mNewUserData = "/templates";    /** Where to get Themes presented to new users. */    protected String mNewUserThemes = "/themes";    /** List of "editor pages" for the Weblog entry editor. */    protected List mEditorPages = new ArrayList();    /** Dis/enble RSS aggregation capabilities. */    protected boolean mEnableAggregator = false;    /** Are file uploads enabled. */    protected boolean mUploadEnabled = false;    /** The maximum size of each user's upload directory. */    protected Float mUploadMaxDirMB = new Float( "2" );    /** The maximum size allowed per uploaded file. */    protected Float mUploadMaxFileMB = new Float( ".5" );    /**     * List of permitted file extensions (not including the "dot"). This     * attribute is mutually exclusive with uploadForbid.     */    protected List mUploadAllow = new ArrayList();    /**     * List of forbidden file extensions (not including the "dot"). This     * attribute is mutually exclusive with uploadAllow.     */    protected List mUploadForbid = new ArrayList();    /**     * Directory where uploaded files will be stored. May end with a slash.     * Optional, this value will default to RollerContext.USER_RESOURCES.  If     * specified, should be a full path on the system harddrive or relative to     * the WebApp.     */    protected String mUploadDir = "";    /**     * The path from which the webserver will serve upload files. This values     * must not end in a slash.     */    protected String uploadPath = "/resources";    protected boolean mMemDebug = false;    /**     * Determines if the Comment page will "autoformat" comments.  That is,     * replace carriage-returns with <br />.     */    protected boolean mAutoformatComments = false;    /** Determines if the Comment page will escape html in comments. */    protected boolean mEscapeCommentHtml = false;    /** Determines if e-mailing comments is enabled. */    protected boolean mEmailComments = false;    /** Enable linkback extraction. */    protected boolean mEnableLinkback = false;    /** Name of this site */    protected String mSiteName = "Roller-based Site";    /** Description of this site */    protected String mSiteDescription = "Roller-based Site";    /** Site administrator's email address */    protected String mEmailAddress = "";    /** Lucene index directory */    protected String mIndexDir =        "${user.home}" + File.separator + "roller-index";    /**     * Flag for encrypting passwords     */    protected boolean mEncryptPasswords = false;        /** Algorithm for encrypting passwords */    protected String mAlgorithm = "SHA";        public OldRollerConfig()    {    }    public OldRollerConfig( RollerConfigData rConfig )    {        this.setAbsoluteURL( rConfig.getAbsoluteURL() );        this.setRssUseCache( rConfig.getRssUseCache().booleanValue() );        this.setRssCacheTime( rConfig.getRssCacheTime().intValue() );        this.setNewUserAllowed( rConfig.getNewUserAllowed().booleanValue() );        this.setNewUserThemes( rConfig.getUserThemes() );        this.setEditorPages( rConfig.getEditorPagesList() );        this.setEnableAggregator( rConfig.getEnableAggregator().booleanValue() );        this.setUploadEnabled( rConfig.getUploadEnabled().booleanValue() );        this.setUploadMaxDirMB( new Float( rConfig.getUploadMaxDirMB()                                                  .doubleValue() ) );        this.setUploadMaxFileMB( new Float( rConfig.getUploadMaxFileMB()                                                   .doubleValue() ) );        this.setUploadAllow( Arrays.asList( rConfig.uploadAllowArray() ) );        this.setUploadForbid( Arrays.asList( rConfig.uploadForbidArray() ) );        this.setUploadDir( rConfig.getUploadDir() );        this.setUploadPath( rConfig.getUploadPath() );        this.setMemDebug( rConfig.getMemDebug().booleanValue() );        this.setAutoformatComments( rConfig.getAutoformatComments()                                           .booleanValue() );        this.setEscapeCommentHtml( rConfig.getEscapeCommentHtml()                                          .booleanValue() );        this.setEmailComments( rConfig.getEmailComments().booleanValue() );        this.setEnableLinkback( rConfig.getEnableLinkback().booleanValue() );        this.setSiteName( rConfig.getSiteName() );        this.setSiteDescription( rConfig.getSiteDescription() );        this.setEmailAddress( rConfig.getEmailAddress() );        this.setIndexDir( rConfig.getIndexDir() );        this.setEncryptPasswords( rConfig.getEncryptPasswords().booleanValue() );        this.setAlgorithm( rConfig.getAlgorithm() );    }    //-------------------------------------- begin requisite getters & setters    public String getAbsoluteURL()    {        return mAbsoluteURL;    }    public void setAbsoluteURL( String string )    {        mAbsoluteURL = string;    }    public boolean getRssUseCache()    {        return mRssUseCache;    }    public void setRssUseCache( boolean use )    {        mRssUseCache = use;    }    public int getRssCacheTime()    {        return mRssCacheTime;    }    public void setRssCacheTime( int cacheTime )    {        mRssCacheTime = cacheTime;    }    public boolean getNewUserAllowed()    {        return mNewUserAllowed;    }    public void setNewUserAllowed( boolean use )    {        mNewUserAllowed = use;    }    public List getAdminUsers()    {        return mAdminUsers;    }    /**     * @param _adminUsers     */    public void setAdminUsers( List _adminUsers )    {        mAdminUsers = _adminUsers;    }    /**     * @param ignore     */    public void addAdminUsers( String ignore )    {        mAdminUsers.add( ignore );    }    public String getNewUserData()    {        return mNewUserData;    }    /**     * @param str     */    public void setNewUserData( String str )    {        mNewUserData = str;    }    public String getNewUserThemes()    {        return mNewUserThemes;    }    /**     * @param str     */    public void setNewUserThemes( String str )    {        mNewUserThemes = str;    }    public List getEditorPages()    {        return mEditorPages;    }    /**     * @param _editorPages     */    public void setEditorPages( List _editorPages )    {        mEditorPages = _editorPages;    }    /**     * @param ignore     */    public void addEditorPages( String ignore )    {        mEditorPages.add( ignore );    }    public boolean getEnableAggregator()    {        return mEnableAggregator;    }    public void setEnableAggregator( boolean use )    {        mEnableAggregator = use;    }    public boolean getUploadEnabled()    {        return mUploadEnabled;    }    public void setUploadEnabled( boolean use )    {        mUploadEnabled = use;    }    public Float getUploadMaxDirMB()    {        return mUploadMaxDirMB;    }    public void setUploadMaxDirMB( Float use )    {        mUploadMaxDirMB = use;    }    public Float getUploadMaxFileMB()    {        return mUploadMaxFileMB;    }    public void setUploadMaxFileMB( Float use )    {        mUploadMaxFileMB = use;    }    public List getUploadAllow()    {        return mUploadAllow;    }    /**     * @param _uploadAllow     */    public void setUploadAllow( List _uploadAllow )    {        mUploadAllow = _uploadAllow;    }    /**     * @param ignore     */    public void addUploadAllow( String ignore )    {        mUploadAllow.add( ignore );    }    public List getUploadForbid()    {        return mUploadForbid;    }    /**     * @param _uploadForbid     */    public void setUploadForbid( List _uploadForbid )    {        mUploadForbid = _uploadForbid;    }    /**     * @param ignore     */    public void addUploadForbid( String ignore )    {        mUploadForbid.add( ignore );    }    public String getUploadDir()    {        return mUploadDir;    }    /**     * @param str     */    public void setUploadDir( String str )    {        mUploadDir = str;    }    public String getUploadPath()    {        return uploadPath;    }    /**     * @param str     */    public void setUploadPath( String str )    {        uploadPath = str;    }    public boolean getMemDebug()    {        return mMemDebug;    }    /**     * Set memory debugging on or off.     *     * @param memDebug The mMemDebug to set     */    public void setMemDebug( boolean memDebug )    {        mMemDebug = memDebug;    }    public boolean getAutoformatComments()    {        return mAutoformatComments;    }    /**     * @param value     */    public void setAutoformatComments( boolean value )    {        mAutoformatComments = value;    }    public boolean getEscapeCommentHtml()    {        return mEscapeCommentHtml;    }    /**     * @param value     */    public void setEscapeCommentHtml( boolean value )    {        mEscapeCommentHtml = value;    }    /**     * @return boolean     */    public boolean getEmailComments()    {        return mEmailComments;    }    /**     * Sets the emailComments.     *     * @param emailComments The emailComments to set     */    public void setEmailComments( boolean emailComments )    {        this.mEmailComments = emailComments;    }    /**     * Enable linkback.     *     * @return     */    public boolean isEnableLinkback()    {        return mEnableLinkback;    }    /**     * Enable linkback.     *     * @param b     */    public void setEnableLinkback( boolean b )    {        mEnableLinkback = b;    }    /**     * @return     */    public String getSiteDescription()    {        return mSiteDescription;    }    /**     * @return     */    public String getSiteName()    {        return mSiteName;    }    /**     * @param string     */    public void setSiteDescription( String string )    {        mSiteDescription = string;    }    /**     * @param string     */    public void setSiteName( String string )    {        mSiteName = string;    }    /**     * @return     */    public String getEmailAddress()    {        return mEmailAddress;    }    /**     * @param emailAddress     */    public void setEmailAddress( String emailAddress )    {        mEmailAddress = emailAddress;    }    /**     * @return the index directory     */    public String getIndexDir()    {        return mIndexDir;    }    /**     * @param indexDir new index directory     */    public void setIndexDir( String indexDir )    {        mIndexDir = indexDir;    }    public boolean getEncryptPasswords()    {        return mEncryptPasswords;    }    public void setEncryptPasswords( boolean use )    {        mEncryptPasswords = use;    }        /**     * @return the algorithm for encrypting passwords     */    public String getAlgorithm()    {        return mAlgorithm;    }    /**     * @param algorithm, the new algorithm     */    public void setAlgorithm( String algorithm )    {        mAlgorithm = algorithm;    }        //---------------------------------------- end requisite getters & setters    /**     * Convenience method for getAdminUsers.     *     * @return     */    public String[] adminUsersArray()    {        if ( mAdminUsers == null )        {            mAdminUsers = new ArrayList();        }        return (String[]) mAdminUsers.toArray( new String[mAdminUsers.size()] );    }    /**     * Convenience method for getEditorPages.     *     * @return     */    public String[] editorPagesArray()    {        if ( mEditorPages == null )        {            mEditorPages = new ArrayList();        }        return (String[]) mEditorPages.toArray( new String[mEditorPages.size()] );    }    /**     * Convenience method for getUploadAllow.     *     * @return     */    public String[] uploadAllowArray()    {        if ( mUploadAllow == null )        {            mUploadAllow = new ArrayList();        }        return (String[]) mUploadAllow.toArray( new String[mUploadAllow.size()] );    }    /**     * Convenience method for getUploadForbid.     *     * @return     */    public String[] uploadForbidArray()    {        if ( mUploadForbid == null )        {            mUploadForbid = new ArrayList();        }        return (String[]) mUploadForbid.toArray( new String[mUploadForbid.size()] );    }    public void updateValues( OldRollerConfig child )    {        this.mAbsoluteURL = child.getAbsoluteURL();        this.mRssUseCache = child.getRssUseCache();        this.mRssCacheTime = child.getRssCacheTime();        this.mNewUserAllowed = child.getNewUserAllowed();        this.mAdminUsers = child.getAdminUsers();        this.mNewUserData = child.getNewUserData();        this.mNewUserThemes = child.getNewUserThemes();        this.mEditorPages = child.getEditorPages();        this.mEnableAggregator = child.getEnableAggregator();        this.mUploadEnabled = child.getUploadEnabled();        this.mUploadMaxDirMB = child.getUploadMaxDirMB();        this.mUploadMaxFileMB = child.getUploadMaxFileMB();        this.mUploadAllow = child.getUploadAllow();        this.mUploadForbid = child.getUploadForbid();        this.mUploadDir = child.getUploadDir();        this.uploadPath = child.getUploadPath();        this.mMemDebug = child.getMemDebug();        this.mAutoformatComments = child.getAutoformatComments();        this.mEscapeCommentHtml = child.getEscapeCommentHtml();        this.mEmailComments = child.getEmailComments();        this.mEnableLinkback = child.isEnableLinkback();        this.mSiteName = child.getSiteName();        this.mSiteDescription = child.getSiteDescription();        this.mEmailAddress = child.getEmailAddress();        this.mIndexDir = child.getIndexDir();        this.mEncryptPasswords = child.getEncryptPasswords();        this.mAlgorithm = child.getAlgorithm();    }    /**     * nice output for debugging     *     * @return     */    public String toString()    {        StringBuffer buf = new StringBuffer();        buf.append( "RollerConfig \n" );        Class clazz = getClass();        Field[] fields = clazz.getDeclaredFields();        try        {            AccessibleObject.setAccessible( fields, true );            for ( int i = 0; i < fields.length; i++ )            {                buf.append( "\t[" + fields[i].getName() + "=" +                            fields[i].get( this ) + "], \n" );            }        }        catch ( Exception e )        {            // ignored!        }        return buf.toString();    }    /**     * Read the RollerConfig from a file, as specified by a String path.     *     * @param path     *     * @return     */    public static OldRollerConfig readConfig( String path )    {        InputStream in = null;        try        {            in = new FileInputStream( path );            return OldRollerConfig.readConfig( in );        }        catch ( Exception e )        {            System.out.println( "Exception reading RollerConfig: " +                                e.getMessage() );        }        finally        {            try            {                if ( in != null )                {                    in.close();                }            }            catch ( java.io.IOException ioe )            {                System.err.println( "RollerConfig.writeConfig() unable to close InputStream" );            }        }        return new OldRollerConfig();    }    /**     * Read the RollerConfig from a file, as specified by an InputStream.     *     * @param in     *     * @return     *     * @throws RuntimeException     */    public static OldRollerConfig readConfig( InputStream in )    {        try        {            BeanReader reader = new BeanReader();            reader.setDebug(99);            reader.registerBeanClass( OldRollerConfig.class );            return (OldRollerConfig) reader.parse( in );        }        catch ( IOException e )        {            throw new RuntimeException( "FATAL ERROR reading RollerConfig inputstream.",                                        e );        }        catch ( SAXException e )        {            throw new RuntimeException( "FATAL ERROR parsing RollerConfig, file is corrupted?",                                        e );        }        catch ( IntrospectionException e )        {            throw new RuntimeException( "FATAL ERROR introspecting RollerConfig bean.",                                        e );        }    }    /**     * Write RollerConfig to file, as specified by a String path.     *     * @param path     *     * @throws RollerException     */    public void writeConfig( String path ) throws RollerException    {        FileOutputStream out = null;        try        {            out = new FileOutputStream( path );            writeConfig( out );        }        catch ( FileNotFoundException e )        {            throw new RollerException( "ERROR file not found: " + path, e );        }        finally        {            try            {                if ( out != null )                {                    out.close();                }            }            catch ( java.io.IOException ioe )            {                System.err.println( "RollerConfig.writeConfig() unable to close OutputStream" );            }        }    }    /**     * Write RollerConfig to file, as specified by an OutputStream.     *     * @param out     *     * @throws RollerException     */    public void writeConfig( OutputStream out ) throws RollerException    {        BeanWriter writer = new BeanWriter( out );        writer.enablePrettyPrint();        writer.setIndent( "    " );        writer.setWriteIDs( false );        try        {            writer.write( this );        }        catch ( IOException e )        {            throw new RollerException( "ERROR writing to roller-config.xml stream.",                                       e );        }        catch ( SAXException e )        {            throw new RollerException( "ERROR writing to roller-config.xml stream.",                                       e );        }        catch ( IntrospectionException e )        {            throw new RollerException( "ERROR introspecting RollerConfig bean.",                                       e );        }    }    /**     * test stuff     *     * @param args     */    public static void main( String[] args )    {        String basedir = System.getProperty( "basedir" );        String path = "build/roller/WEB-INF/roller-config.xml";        path = new java.io.File( basedir, path ).getAbsolutePath();        if ( ( args.length > 0 ) && args[0].equals( "read" ) )        {            OldRollerConfig.readConfig( path );        }        else if ( ( args.length > 0 ) && args[0].equals( "write" ) ) // write        {            path = "build/roller/WEB-INF/roller-config-test.xml";            path = new java.io.File( basedir, path ).getAbsolutePath();            OldRollerConfig bean = new OldRollerConfig();            try            {                bean.writeConfig( path );            }            catch ( Exception e )            {                mLogger.error( "Unexpected exception", e );            }        }        else // both        {            OldRollerConfig bean = OldRollerConfig.readConfig( path );            path = "build/roller/WEB-INF/roller-config-test.xml";            path = new java.io.File( basedir, path ).getAbsolutePath();            try            {                bean.writeConfig( path );            }            catch ( Exception e )            {                mLogger.error( "Unexpected exception", e );            }        }        System.out.println( "RollerConfig.main completed" );    }}