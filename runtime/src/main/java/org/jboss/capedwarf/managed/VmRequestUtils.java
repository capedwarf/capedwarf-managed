/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.capedwarf.managed;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class VmRequestUtils {
    private static final Logger logger = Logger.getLogger(VmRequestUtils.class.getName());

    private static final String HEALTH_CHECK_PATH = "/_ah/health";

    public static final double HEALTH_CHECK_INTERVAL_OFFSET_RATIO = 1.5;
    public static final int DEFAULT_CHECK_INTERVAL_SEC = 5;
    public static final String LINK_LOCAL_IP_NETWORK = "169.254";

    private static boolean isLastSuccessful = false;
    private static long timeStampOfLastNormalCheckMillis = 0;
    private static int checkIntervalSec = -1;

    public static void setCheckIntervalSec(int checkIntervalSec) {
        VmRequestUtils.checkIntervalSec = checkIntervalSec;
    }

    /**
     * Checks if a remote address is trusted for the purposes of handling
     * requests.
     *
     * @param remoteAddr String representation of the remote ip address.
     * @return True if and only if the remote address should be allowed to make
     * requests.
     */
    public static boolean isTrustedRemoteAddr(boolean isDevMode, String remoteAddr) {
        if (isDevMode) {
            return isDevMode;
        } else if (remoteAddr == null) {
            return false;
        } else if (remoteAddr.startsWith("172.17.")) {
            return true;
        } else if (remoteAddr.startsWith(LINK_LOCAL_IP_NETWORK)) {
            return true;
        } else if (remoteAddr.startsWith("127.0.0.")) {
            return true;
        }
        return false;
    }

    public static boolean isValidHealthCheckAddr(boolean isDevMode, String remoteAddr) {
        if (isTrustedRemoteAddr(isDevMode, remoteAddr)) {
            return true;
        } else if (remoteAddr == null) {
            return false;
        }
        return remoteAddr.startsWith("130.211.0.")
            || remoteAddr.startsWith("130.211.1.")
            || remoteAddr.startsWith("130.211.2.")
            || remoteAddr.startsWith("130.211.3.");
    }

    public static boolean isHealthCheck(HttpServletRequest request) {
        return HEALTH_CHECK_PATH.equalsIgnoreCase(request.getPathInfo());
    }

    public static boolean isLocalHealthCheck(HttpServletRequest request, String remoteAddr) {
        String isLastSuccessfulPara = request.getParameter("IsLastSuccessful");
        return isLastSuccessfulPara == null && !remoteAddr.startsWith(LINK_LOCAL_IP_NETWORK);
    }

    /**
     * Record last normal health check status. It sets this.isLastSuccessful based
     * on the value of "IsLastSuccessful" parameter from the query string ("yes"
     * for True, otherwise False), and also updates
     * this.timeStampOfLastNormalCheckMillis.
     *
     * @param request the HttpServletRequest
     */
    public static void recordLastNormalHealthCheckStatus(HttpServletRequest request) {
        String isLastSuccessfulPara = request.getParameter("IsLastSuccessful");
        if ("yes".equalsIgnoreCase(isLastSuccessfulPara)) {
            isLastSuccessful = true;
        } else if ("no".equalsIgnoreCase(isLastSuccessfulPara)) {
            isLastSuccessful = false;
        } else {
            isLastSuccessful = false;
            logger.warning("Wrong parameter for IsLastSuccessful: " + isLastSuccessfulPara);
        }

        timeStampOfLastNormalCheckMillis = System.currentTimeMillis();
    }

    /**
     * Handle local health check from within the VM. If there is no previous
     * normal check or that check has occurred more than checkIntervalSec seconds
     * ago, it returns unhealthy. Otherwise, returns status based value of
     * this.isLastSuccessful, "true" for success and "false" for failure.
     *
     * @param response the HttpServletResponse
     * @throws java.io.IOException when it couldn't send out response
     */
    public static void handleLocalHealthCheck(HttpServletResponse response) throws IOException {
        if (!isLastSuccessful) {
            logger.warning("unhealthy (isLastSuccessful is False)");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (timeStampOfLastNormalCheckMillis == 0) {
            logger.warning("unhealthy (no incoming remote health checks seen yet)");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        long timeOffset = System.currentTimeMillis() - timeStampOfLastNormalCheckMillis;
        if (timeOffset > checkIntervalSec * HEALTH_CHECK_INTERVAL_OFFSET_RATIO * 1000) {
            logger.warning("unhealthy (last incoming health check was " + timeOffset + "ms ago)");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();
        writer.write("ok");
        writer.flush();
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
