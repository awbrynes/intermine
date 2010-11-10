package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.codegen.WebserviceJavaCodeGenerator;
import org.intermine.api.query.codegen.WebservicePerlCodeGenerator;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to handle the web service code generation.
 * Multiple-query is not supported.
 *
 * @author Fengyuan Hu
 */
public class WebserviceCodeGenAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(WebserviceCodeGenAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);

        try {
            String method = request.getParameter("method");
            String source = request.getParameter("source");

            if ("perl".equals(method)) {
                WebservicePerlCodeGenerator wsPerlCG = new WebservicePerlCodeGenerator();

                if ("templateQuery".equals(source)) {
                    String sc = wsPerlCG.generate(getTemplateQuery(im, profile, request, session));
                    output(sc, method, source, response);
                } else if ("pathQuery".equals(source)) {
                    String sc = wsPerlCG.generate(getPathQuery(session));
                    output(sc, method, source, response);
                }
            } else if ("java".equals(method)) {
                WebserviceJavaCodeGenerator wsJavaCG = new WebserviceJavaCodeGenerator();

                if ("templateQuery".equals(source)) {
                    String sc = wsJavaCG.generate(getTemplateQuery(im, profile, request, session));
                    output(sc, method, source, response);
                } else if ("pathQuery".equals(source)) {
                    String sc = wsJavaCG.generate(getPathQuery(session));
                    output(sc, method, source, response);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return mapping.findForward("begin");
        }

        return null;
    }

    /**
     * Method called to get the template query.
     * @param im InterMineAPI object
     * @param profile Profile object
     * @param request HttpServletRequest object
     * @param session HttpSession object
     * @return TemplateQuery object
     */
    private TemplateQuery getTemplateQuery(InterMineAPI im, Profile profile,
            HttpServletRequest request, HttpSession session) {

        String name = request.getParameter("name");
        String scope = request.getParameter("scope");
        String originalTemplate = request.getParameter("originalTemplate");

        TemplateManager templateManager = im.getTemplateManager();
        if (name == null) {
            throw new IllegalArgumentException("Cannot find a template in context "
                                                   + scope);
        } else {
            TemplateQuery template = (originalTemplate != null)
                                     ? templateManager.getTemplate(profile, name, scope)
                                     : (TemplateQuery) SessionMethods.getQuery(session);
            if (template != null) {
                return template;
            } else {
                throw new IllegalArgumentException("Cannot find template " + name + " in context "
                        + scope);
            }
        }
    }

    /**
     * Method called to get the path query.
     * @param session HttpSession object
     * @return PathQuery object
     */
    private PathQuery getPathQuery(HttpSession session) {
        // path query name is empty
        PathQuery query =  SessionMethods.getQuery(session);

        if (query != null) {
            return query;
        } else {
            throw new IllegalArgumentException("Cannot find a query");
        }
    }

    /**
     * Method called to print the source code.
     * @param sourceCodeString a string representing the source code
     * @param method perl/java
     * @param source template query/path query
     * @param response HttpServletResponse
     */
    private void output(String sourceCodeString, String method, String source,
            HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=" + source + "." + method);

        PrintStream out;
        try {
            out = new PrintStream(response.getOutputStream());
            out.print(sourceCodeString);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}