/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.auth;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.extension.UserMapper;
import org.nuxeo.usermapper.service.UserMapperService;

public class ShibbolethAuthenticationPlugin implements NuxeoAuthenticationPlugin,
        NuxeoAuthenticationPluginLogoutExtension {

    private static final Log log = LogFactory.getLog(ShibbolethAuthenticationPlugin.class);

    public static final String EXTERNAL_MAPPER_PARAM = "mapper";

    public static final String DEFAULT_EXTERNAL_MAPPER = "shibboleth";

    protected UserMapper externalMapper = null;

    protected ShibbolethAuthenticationService getService() {
        return Framework.getService(ShibbolethAuthenticationService.class);
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        if (getService() == null) {
            return false;
        }
        String loginURL = getService().getLoginURL(httpRequest);
        if (loginURL == null) {
            log.error("Unable to handle Shibboleth login, no loginURL registered");
            return false;
        }
        try {
            httpResponse.sendRedirect(loginURL);
        } catch (IOException e) {
            String errorMessage = String.format("Unable to handle Shibboleth login on %s", loginURL);
            log.error(errorMessage, e);
            return false;
        }
        return true;
    }

    @Override
    public Boolean handleLogout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (getService() == null) {
            return false;
        }
        String logoutURL = getService().getLogoutURL(httpRequest);
        if (logoutURL == null) {
            return false;
        }
        try {
            httpResponse.sendRedirect(logoutURL);
        } catch (IOException e) {
            log.error("Unable to handle Shibboleth logout", e);
            return false;
        }
        return true;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (getService() == null) {
            return null;
        }

        String userId = getService().getUserID(httpRequest);
        if (userId == null || StringUtils.EMPTY.equals(userId)) {
            return null;
        } else {
            UserMapperService ums = Framework.getService(UserMapperService.class);
            if (ums != null) {
                if (ums.getAvailableMappings().contains(DEFAULT_EXTERNAL_MAPPER)) {
                    externalMapper = ums.getMapper(DEFAULT_EXTERNAL_MAPPER);
                }
            }
        }
        UserManager userManager = Framework.getService(UserManager.class);
        Map<String, Object> fieldMap = getService().getUserMetadata(userManager.getUserIdField(), httpRequest);

        if (externalMapper != null) {
            Map<String, Object> nativeObject = new HashMap<String, Object>();
            nativeObject.putAll(fieldMap);
            nativeObject.put("userId", userId);
            externalMapper.getOrCreateAndUpdateNuxeoPrincipal(nativeObject);
        } else {
            try (Session userDir = Framework.getService(DirectoryService.class).open(userManager.getUserDirectoryName())) {
                DocumentModel entry = userDir.getEntry(userId);
                if (entry == null) {
                    userDir.createEntry(fieldMap);
                } else {
                    entry.getDataModel(userManager.getUserSchemaName()).setMap(fieldMap);
                    userDir.updateEntry(entry);
                }
            } catch (DirectoryException e) {
                log.error("Failed to get or create user entry", e);
            }
        }

        return new UserIdentificationInfo(userId, userId);
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
    }

}
