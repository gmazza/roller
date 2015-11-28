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
package org.apache.roller.weblogger.ui.struts2.editor;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.MediaFileManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.GlobalRole;
import org.apache.roller.weblogger.pojos.MediaFile;
import org.apache.roller.weblogger.pojos.MediaFileDirectory;
import org.apache.roller.weblogger.pojos.WeblogRole;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;
import org.apache.roller.weblogger.util.RollerMessages;
import org.apache.roller.weblogger.util.RollerMessages.RollerMessage;
import org.apache.roller.weblogger.util.Utilities;
import org.apache.struts2.interceptor.validation.SkipValidation;

/**
 * Adds or edits a media file.
 */
@SuppressWarnings("serial")
public class MediaFileEdit extends UIAction {

    private static Log log = LogFactory.getLog(MediaFileEdit.class);
    private MediaFile bean = new MediaFile();
    private MediaFileDirectory directory;
    private String mediaFileId;
    private List<MediaFileDirectory> allDirectories;

    private MediaFileManager mediaFileManager;

    public void setMediaFileManager(MediaFileManager mediaFileManager) {
        this.mediaFileManager = mediaFileManager;
    }

    // an array of files uploaded by the user, if applicable
    private File uploadedFile = null;

    // content type for upload file
    private String uploadedFileContentType = null;

    // an array of filenames for uploaded files
    private String uploadedFileFileName = null;

    private String directoryName = null;

    public MediaFileEdit() {
        this.desiredMenu = "editor";
    }

    private boolean isAdd() {
        return actionName.equals("mediaFileAdd");
    }

    @Override
    public GlobalRole requiredGlobalRole() {
        return GlobalRole.BLOGGER;
    }

    @Override
    public WeblogRole requiredWeblogRole() {
        return WeblogRole.POST;
    }

    /**
     * Prepares action class
     */
    public void prepare() {
        try {
            allDirectories = mediaFileManager.getMediaFileDirectories(getActionWeblog());
            if (!StringUtils.isEmpty(bean.getDirectoryId())) {
                setDirectory(mediaFileManager.getMediaFileDirectory(bean.getDirectoryId()));
            } else if (StringUtils.isNotEmpty(directoryName)) {
                setDirectory(mediaFileManager.getMediaFileDirectoryByName(getActionWeblog(), directoryName));
            } else {
                MediaFileDirectory root = mediaFileManager.getDefaultMediaFileDirectory(getActionWeblog());
                if (root == null) {
                    root = mediaFileManager.createDefaultMediaFileDirectory(getActionWeblog());
                }
                setDirectory(root);
            }
            directoryName = getDirectory().getName();
            bean.setDirectoryId(getDirectory().getId());
        } catch (WebloggerException ex) {
            log.error("Error looking up media file directory", ex);
        } finally {
            // flush
            try {
                WebloggerFactory.flush();
            } catch (WebloggerException e) {
                // ignored
            }
        }
    }

    /**
     * Validates media file to be added.
     */
    public void myValidate() {
        if (isAdd()) {
            // make sure uploads are enabled
            if (!WebloggerRuntimeConfig.getBooleanProperty("uploads.enabled")) {
                addError("error.upload.disabled");
            }
            if (uploadedFile == null || !uploadedFile.exists()) {
                addError("error.upload.nofile");
            }
        } else {
            MediaFile fileWithSameName = getDirectory().getMediaFile(getBean().getName());
            if (fileWithSameName != null
                    && !fileWithSameName.getId().equals(getMediaFileId())) {
                addError("MediaFile.error.duplicateName", getBean().getName());
            }
        }
    }

    /**
     * Show form for adding a new media file.
     * 
     * @return String The result of the action.
     */
    @SkipValidation
    public String execute() {
        if (!isAdd()) {
            try {
                MediaFile mediaFile = mediaFileManager.getMediaFile(getMediaFileId());
                bean.setId(mediaFile.getId());
                bean.setName(mediaFile.getName());
                bean.setAltText(mediaFile.getAltText());
                bean.setTitleText(mediaFile.getTitleText());
                bean.setAnchor(mediaFile.getAnchor());
                bean.setNotes(mediaFile.getNotes());
                bean.setDirectory(mediaFile.getDirectory());
                bean.setDirectoryId(mediaFile.getDirectory().getId());
                bean.setWidth(mediaFile.getWidth());
                bean.setHeight(mediaFile.getHeight());
                bean.setLength(mediaFile.getLength());
                bean.setContentType(mediaFile.getContentType());
            } catch (Exception e) {
                log.error("Error uploading file " + bean.getName(), e);
                addError("uploadFiles.error.upload", bean.getName());
            }
        }
        return INPUT;
    }

    /**
     * Save a media file.
     * 
     * @return String The result of the action.
     */
    public String save() {
        myValidate();
        if (!hasActionErrors()) {
            try {
                if (isAdd()) {
                    MediaFile mediaFile = new MediaFile();
                    mediaFile.setName(bean.getName());
                    mediaFile.setAltText(bean.getAltText());
                    mediaFile.setTitleText(bean.getTitleText());
                    mediaFile.setAnchor(bean.getAnchor());
                    mediaFile.setNotes(bean.getNotes());
                    String fileName = getUploadedFileFileName();

                    // make sure fileName is valid
                    if (fileName.indexOf('/') != -1
                            || fileName.indexOf('\\') != -1
                            || fileName.contains("..")
                            || fileName.indexOf('\000') != -1) {
                        addError("uploadFiles.error.badPath", fileName);
                    } else {
                        mediaFile.setName(fileName);
                        mediaFile.setDirectory(getDirectory());
                        mediaFile.setLength(this.uploadedFile.length());
                        mediaFile.setInputStream(new FileInputStream(this.uploadedFile));
                        mediaFile.setContentType(this.uploadedFileContentType);

                        // in some cases Struts2 is not able to guess the content
                        // type correctly and assigns the default, which is
                        // octet-stream. So in cases where we see octet-stream
                        // we double check and see if we can guess the content
                        // type via the Java MIME type facilities.
                        mediaFile.setContentType(this.uploadedFileContentType);
                        if (mediaFile.getContentType() == null
                                || mediaFile.getContentType().endsWith("/octet-stream")) {
                            String ctype = Utilities.getContentTypeFromFileName(mediaFile.getName());
                            if (null != ctype) {
                                mediaFile.setContentType(ctype);
                            }
                        }

                        RollerMessages errors = new RollerMessages();
                        mediaFileManager.createMediaFile(getActionWeblog(), mediaFile, errors);
                        for (Iterator it = errors.getErrors(); it.hasNext(); ) {
                            RollerMessage msg = (RollerMessage) it.next();
                            addError(msg.getKey(), Arrays.asList(msg.getArgs()));
                        }

                        WebloggerFactory.flush();
                        // below should not be necessary as createMediaFile refreshes the directory's
                        // file listing but caching of directory's old file listing occurring somehow.
                        mediaFile.getDirectory().getMediaFiles().add(mediaFile);
                    }

                    if (!this.errorsExist()) {
                        addMessage("uploadFiles.uploadedFiles");
                        addMessage("uploadFiles.uploadedFile", mediaFile.getName());
                        this.pageTitle = "mediaFileAddSuccess.title";
                        return SUCCESS;
                    }
                } else {
                    MediaFile mediaFile = mediaFileManager.getMediaFile(getMediaFileId());
                    mediaFile.setName(bean.getName());
                    mediaFile.setAltText(bean.getAltText());
                    mediaFile.setTitleText(bean.getTitleText());
                    mediaFile.setAnchor(bean.getAnchor());
                    mediaFile.setNotes(bean.getNotes());

                    if (uploadedFile != null) {
                        mediaFile.setLength(this.uploadedFile.length());
                        mediaFile.setContentType(this.uploadedFileContentType);
                        mediaFileManager.updateMediaFile(getActionWeblog(), mediaFile,
                                new FileInputStream(this.uploadedFile));
                    } else {
                        mediaFileManager.updateMediaFile(getActionWeblog(), mediaFile);
                    }

                    // Move file
                    if (!getBean().getDirectoryId().equals(mediaFile.getDirectory().getId())) {
                        log.debug("Processing move of " + mediaFile.getId());
                        MediaFileDirectory targetDirectory = mediaFileManager.getMediaFileDirectory(getBean().getDirectoryId());
                        mediaFileManager.moveMediaFile(mediaFile, targetDirectory);
                    }

                    WebloggerFactory.flush();

                    addMessage("mediaFile.update.success");
                    return SUCCESS;
                }
            } catch (Exception e) {
                log.error("Error uploading file " + bean.getName(), e);
                addError("mediaFileAdd.errorUploading", bean.getName());
            }
        }
        return INPUT;
    }


    public MediaFile getBean() {
        return bean;
    }

    public void setBean(MediaFile b) {
        this.bean = b;
    }

    public MediaFileDirectory getDirectory() {
        return directory;
    }

    public void setDirectory(MediaFileDirectory directory) {
        this.directory = directory;
    }

    public File getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(File uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public String getUploadedFileContentType() {
        return uploadedFileContentType;
    }

    public void setUploadedFileContentType(String uploadedFileContentType) {
        this.uploadedFileContentType = uploadedFileContentType;
    }

    public String getUploadedFileFileName() {
        return uploadedFileFileName;
    }

    public void setUploadedFileFileName(String uploadedFileFileName) {
        this.uploadedFileFileName = uploadedFileFileName;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }

    public List<MediaFileDirectory> getAllDirectories() {
        return allDirectories;
    }

    public String getMediaFileId() {
        return mediaFileId;
    }

    public void setMediaFileId(String mediaFileId) {
        this.mediaFileId = mediaFileId;
    }

}
