/*
 * Copyright 2003-2021 JetBrains s.r.o.
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
package org.modelix.instancesmanager;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.Request;

import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

public class RedirectedURL {
    public static final String PERSONAL_DEPLOYMENT_PREFIX = "user-copy-";
    public static final String COOKIE_NAME = "modelix-mps-instance";

    public static RedirectedURL redirect(@Nullable Request baseRequest, HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.charAt(0) != '/') return null;
        int indexOfSlash = path.indexOf('/', 1);
        if (indexOfSlash < 0) return null;

        String originalDeploymentName = path.substring(1, indexOfSlash);
        String remainingPath = path.substring(indexOfSlash);

        // TODO use the ID of an authenticated user instead (or in addition)
        String personalDeploymentSuffix = null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                personalDeploymentSuffix = cookie.getValue();
            }
        }
        if (personalDeploymentSuffix == null) {
            personalDeploymentSuffix = UUID.randomUUID().toString();
        }
        if (baseRequest != null) {
            baseRequest.getResponse().addCookie(new HttpCookie(COOKIE_NAME, personalDeploymentSuffix, null, "/", 30 * 24 * 60 * 60, true, false));
        }

        String personalDeploymentName = PERSONAL_DEPLOYMENT_PREFIX + originalDeploymentName + "-" + personalDeploymentSuffix;

        return new RedirectedURL(remainingPath, originalDeploymentName, personalDeploymentName);
    }

    public final String remainingPath;
    public final  String originalDeploymentName;
    public final  String personalDeploymentName;

    public RedirectedURL(String remainingPath, String originalDeploymentName, String personalDeploymentName) {
        this.remainingPath = remainingPath;
        this.originalDeploymentName = originalDeploymentName;
        this.personalDeploymentName = personalDeploymentName;
        System.out.println("Redirect: " + remainingPath + " / " + originalDeploymentName + " / " + personalDeploymentName);
    }

    public String getRedirectedUrl(boolean websocket) {
        String url = (websocket ? "ws" : "http") + "://" + personalDeploymentName;
        if (remainingPath.startsWith("/ide/")) {
            url += ":8887" + remainingPath;
        } else {
            url += ":33333" + remainingPath;
        }
        return url;
    }
}