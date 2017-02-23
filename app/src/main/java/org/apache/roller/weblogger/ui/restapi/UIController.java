/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.roller.weblogger.ui.restapi;

import org.apache.roller.weblogger.business.MailManager;
import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.weblogger.business.WebloggerContext;
import org.apache.roller.weblogger.business.WebloggerStaticConfig;
import org.apache.roller.weblogger.pojos.GlobalRole;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.UserSearchCriteria;
import org.apache.roller.weblogger.pojos.UserStatus;
import org.apache.roller.weblogger.pojos.UserWeblogRole;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogRole;
import org.apache.roller.weblogger.pojos.WebloggerProperties;
import org.apache.roller.weblogger.ui.core.menu.Menu;
import org.apache.roller.weblogger.ui.core.menu.MenuHelper;
import org.apache.roller.weblogger.ui.rendering.processors.PageProcessor;
import org.apache.roller.weblogger.util.I18nMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping(path = "/tb-ui/app")
public class UIController {

    @Autowired
    private UserManager userManager;

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    @Autowired
    private MailManager mailManager;

    public void setMailManager(MailManager manager) {
        mailManager = manager;
    }

    @Autowired
    private MenuHelper menuHelper;

    public void setMenuHelper(MenuHelper menuHelper) {
        this.menuHelper = menuHelper;
    }

    @RequestMapping(value = "/login")
    public ModelAndView login(@RequestParam(required = false) String activationCode,
                              @RequestParam(required = false) Boolean error) {

        I18nMessages messages = I18nMessages.getMessages(Locale.getDefault());

        Map<String, Object> myMap = new HashMap<>();
        myMap.put("pageTitle", messages.getString("login.title"));

        if (Boolean.TRUE.equals(error)) {
            List<String> actionErrors = new ArrayList<>();
            actionErrors.add(messages.getString("error.password.mismatch"));
            myMap.put("actionErrors", actionErrors);
        }

        if (activationCode != null) {
            UserSearchCriteria usc = new UserSearchCriteria();
            usc.setActivationCode(activationCode);
            List<User> users = userManager.getUsers(usc);

            if (users.size() == 1) {
                User user = users.get(0);
                // enable user account
                user.setActivationCode(null);
                WebloggerProperties.RegistrationPolicy regProcess = WebloggerContext.getWebloggerProperties().getRegistrationPolicy();
                if (WebloggerProperties.RegistrationPolicy.APPROVAL_REQUIRED.equals(regProcess)) {
                    user.setStatus(UserStatus.EMAILVERIFIED);
                    myMap.put("activationStatus", "activePending");
                    mailManager.sendRegistrationApprovalRequest(user);
                } else {
                    user.setStatus(UserStatus.ENABLED);
                    myMap.put("activationStatus", "active");
                }
                userManager.saveUser(user);
            } else {
                myMap.put("actionErrors", messages.getString("error.activate.user.invalidActivationCode"));
            }

        }
        return tightblogModelAndView("login", myMap, null, null);
    }

    @RequestMapping(value = "/logout")
    public void logout(Principal principal, HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession().invalidate();
        response.sendRedirect(request.getContextPath()+"/");
    }

    @RequestMapping(value = "/login-redirect")
    public void loginRedirect(Principal principal, HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (principal == null) {
            // trigger call to login page
            response.sendRedirect(request.getContextPath() + "/tb-ui/menu.rol");
        } else {
            User user = userManager.getEnabledUserByUserName(principal.getName());

            if (user == null) {
                /* If authentication successful but no user, authentication must have been via LDAP without
                   the user having registered yet.  So forward to the registration page... */
                response.sendRedirect(request.getContextPath() + "/tb-ui/register.rol");
            } else {
                List<UserWeblogRole> roles = userManager.getWeblogRoles(user);

                if (!GlobalRole.ADMIN.equals(user.getGlobalRole())) {
                    if (roles.size() == 1) {
                        Weblog weblog = roles.get(0).getWeblog();
                        response.sendRedirect(request.getContextPath() + "/tb-ui/authoring/entryAdd.rol?request_locale="
                                + user.getLocale() + "&weblogId=" + weblog.getId());
                    } else {
                        response.sendRedirect(request.getContextPath() + "/tb-ui/menu.rol?request_locale=" + user.getLocale());
                    }
                } else {
                    if (roles.size() > 0) {
                        response.sendRedirect(request.getContextPath() + "/tb-ui/menu.rol?request_locale=" + user.getLocale());
                    } else {
                        // admin has no blog yet, possibly initial setup.
                        response.sendRedirect(request.getContextPath() + "/tb-ui/admin/globalConfig");
                    }
                }
            }
        }
    }

    @RequestMapping(value = "/get-default-blog")
    public void getDefaultBlog(Principal principal, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {


        Weblog defaultBlog = WebloggerContext.getWebloggerProperties().getMainBlog();
        String path;

        if (defaultBlog != null) {
            path = PageProcessor.PATH + '/' + defaultBlog.getHandle();
        } else {
            // new install?  Redirect to register or login page based on whether a user has already been created.
            long userCount = userManager.getUserCount();
            if (userCount == 0) {
                path = "/tb-ui/register.rol";
            } else {
                path = "/tb-ui/app/login-redirect";
            }
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(path);
        dispatcher.forward(request, response);
    }

    @RequestMapping(value = "/admin/cacheInfo")
    public ModelAndView cacheInfo(Principal principal) {
        return getAdminPage(principal, "cacheInfo");
    }

    @RequestMapping(value = "/admin/pingTargets")
    public ModelAndView pingTargets(Principal principal) {
        return getAdminPage(principal, "pingTargets");
    }

    @RequestMapping(value = "/admin/globalConfig")
    public ModelAndView globalConfig(Principal principal) {
        return getAdminPage(principal, "globalConfig");
    }

    @RequestMapping(value = "/admin/userAdmin")
    public ModelAndView userAdmin(Principal principal) {
        return getAdminPage(principal, "userAdmin");
    }

    private ModelAndView getAdminPage(Principal principal, String actionName) {
        User user = userManager.getEnabledUserByUserName(principal.getName());

        Map<String, Object> myMap = new HashMap<>();
        myMap.put("pageTitle", user.getI18NMessages().getString(actionName + ".title"));
        myMap.put("menu", getMenu(user, "/tb-ui/app/admin/" + actionName, "admin", WeblogRole.NOBLOGNEEDED));
        return tightblogModelAndView(actionName, myMap, user, null);
    }

    private ModelAndView tightblogModelAndView(String actionName, Map<String, Object> map, User user, Weblog weblog) {
        map.put("authenticatedUser", user);
        map.put("actionWeblog", weblog);
        map.put("userIsAdmin", user != null && GlobalRole.ADMIN.equals(user.getGlobalRole()));
        map.put("authenticationMethod", WebloggerStaticConfig.getAuthMethod());
        map.put("registrationPolicy", WebloggerContext.getWebloggerProperties().getRegistrationPolicy());
        return new ModelAndView("." + actionName, map);
    }

    private Menu getMenu(User user, String actionName, String desiredMenu, WeblogRole requiredRole) {
        return menuHelper.getMenu(desiredMenu, user.getGlobalRole(), requiredRole, actionName, false);
    }

}