/* ************************************************************************
LEBAH PORTAL FRAMEWORK
Copyright (C) 2007  Shamsul Bahrin

* ************************************************************************ */

package lebah.portal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletMode;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import lebah.db.Labels;
import lebah.portal.db.CustomClass;
import lebah.portal.db.UserPage;
import lebah.portal.velocity.VServlet;
import lebah.portal.velocity.VTemplate;
import lebah.util.Util;

public class ApplicationController extends VServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse res)  throws ServletException, IOException    {
		doPost(req, res);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException  {
		System.out.println("THIS IS APPLICATION CONTROLLER....");
		res.setContentType("text/html");
		//res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		HttpSession session = req.getSession();
		
		// Add by Shaiful Nizam, 2009-08-13.
		// Velocity context is initialize here in order to prevent context
		// sharing between users.
		// =============================
		// INITIALIZING VELOCITY CONTEXT
		// =============================
		//synchronized(this) {
			context = (org.apache.velocity.VelocityContext) session.getAttribute("VELOCITY_CONTEXT");
			engine = (org.apache.velocity.app.VelocityEngine) session.getAttribute("VELOCITY_ENGINE");
			if (context == null || engine == null) {
				if (context == null) {
					System.out.println("[ControllerServlet3] Velocity context is Null.");
				}
				if (engine == null) {
					System.out.println("[ControllerServlet3] Velocity engine is Null.");
				}
				initVelocity(getServletConfig());
				session.setAttribute("VELOCITY_CONTEXT", context);
				session.setAttribute("VELOCITY_ENGINE", engine);
			}
		//}
		//System.out.println("[ControllerServlet3] Session Id = "+session.getId());	
		
		//System.setErr(new PrintStream(new FileOutputStream("c:/error_portal.txt")));
		
		//STORE VELOCITY ENGINE IN THE SESSION OBJECT
		/*
		if ( session.getAttribute("_VELOCITY_INITIALIZED") == null ) {
			session.setAttribute("_VELOCITY_ENGINE", engine);
			session.setAttribute("_VELOCITY_CONTEXT", context);
			session.setAttribute("_VELOCITY_INITIALIZED", "true");
		}
		*/		
		context.put("util", new Util());
		
		//LABELS Properties
		//context.put("label", mecca.db.Labels.getInstance().getTitles()); //<- no need bcoz already defined in DesktopController
		//to handle browser's refresh that might trigger double submission
		//get previous token
		String prev_token = session.getAttribute("form_token") != null ? (String) session.getAttribute("form_token") : "";
		//get form token
		//if user refresh form_token shall always empty
		String form_token = req.getParameter("form_token") != null ? req.getParameter("form_token") : "empty";
		//pre_token equals form_token if not refresh
		
		if ( prev_token.equals(form_token) ) {
			session.setAttribute("doPost", "true");
			session.setAttribute("isPost", new Boolean(true));
		}
		else if ( "empty".equals(form_token) ) {
			session.setAttribute("doPost", "false");
			session.setAttribute("isPost", new Boolean(false));
		}
		else {
			session.setAttribute("doPost", "false");
			session.setAttribute("isPost", new Boolean(false));
		}
		//create a new form token              
		form_token = Long.toString(System.currentTimeMillis());
		session.setAttribute("form_token", form_token);
		
		
		//end handle browser's refresh		
		
        String app_path = getServletContext().getRealPath("/");
        app_path = app_path.replace('\\', '/');		
		session.setAttribute("_portal_app_path", app_path);
		
		String uri = req.getRequestURI();
		String s1 = uri.substring(1);
		context.put("appname", s1.substring(0, s1.indexOf("/")));		
		session.setAttribute("_portal_appname", s1.substring(0, s1.indexOf("/")));		
		//get pathinfo
        String pathInfo = req.getPathInfo();
        pathInfo = pathInfo.substring(1); //get rid of the first '/'
        int slash = pathInfo.indexOf("/");
        boolean allowed = true;
        
        boolean hasSecurityToken = true;
        if ( slash == -1 ) hasSecurityToken = false;

        if ( hasSecurityToken ) {
	        //securityToken in PATH
	        String securityTokenURI = pathInfo.substring(0, pathInfo.indexOf("/"));
	        //security token in SESSION
	        String securityToken =(String) session.getAttribute("securityToken");
	        context.put("securityToken", securityToken);
	        //security token in PATH must equal to securityToken in SESSION
	        if ( !securityTokenURI.equals(securityToken)) {
	        	securityTokenDenied(engine, context, req, res);
	        	allowed = false;
	        }
	        
        }
        
        if ( allowed ){
        	
    		//LABELS Properties
    		Labels label = null;
    		String language = req.getParameter("lang");
    		if ( language != null && !"".equals(language)) {
    			session.setAttribute("_portal_language", language);
    		}
    		language = (String) session.getAttribute("_portal_language");
    		if ( language == null || "".equals(language)) {
    			label = lebah.db.Labels.getInstance();
    			context.put("label", label.getTitles());
    			language = label.getDefaultLanguage();
    			session.setAttribute("_portal_language", language);
    		} else {
    			label = lebah.db.Labels.getInstance(language);
    			context.put("label", label.getTitles());
    		}
        	
	        //pathInfo only contains action
	        pathInfo = pathInfo.substring(pathInfo.indexOf("/") + 1);
			String module = pathInfo != null ? pathInfo : "";	
			
			//module
			//session.setAttribute("_portal_action", module);
			//session.setAttribute("_portal_module", module);
	        String remoteAddr = req.getRemoteAddr();
	        //System.out.println("remote=" + remoteHost + ", " + remoteAddr);
	        boolean localAccess = false;
	        if ( "127.0.0.1".equals(remoteAddr) ) localAccess = true;
			context.put("session", session);	
			String ddir = req.getPathInfo().lastIndexOf("/") == 0 ? "../" : "../../";

			/*
			//out.println("<link href=\"" + ddir + "styles.css\" rel=\"stylesheet\" type=\"text/css\">");
	        out.println("<script type=\"text/javascript\" src=\"" + ddir + "scriptaculous/prototype.js\" ></script>");
	        out.println("<script type=\"text/javascript\" src=\"" + ddir + "scriptaculous/scriptaculous.js\" ></script>");
			out.println("<script type=\"text/javascript\" src=\"" + ddir + "scriptaculous/fixed.js\" ></script>");
			out.println("<script type=\"text/javascript\" src=\"" + ddir + "scriptaculous/dragdrop.js\" ></script>");
			out.println("<script type=\"text/javascript\" src=\"" + ddir + "scriptaculous/unittest.js\" ></script>");
			out.println("<script type=\"text/javascript\" src=\"" + ddir + "scriptaculous/ajax.js\" ></script>");
			//out.println("<link href=\"" + ddir + "styles.css\" rel=\"stylesheet\" type=\"text/css\">");
			
			//js_css
			JS_CSS js_css = new JS_CSS(engine, context, req, res);
			try {
				js_css.print();
			} catch ( Exception ex ) {
				ex.printStackTrace();
			} 
			*/
			
			//if ( !"".equals(module) ) {
				try {
					Object content = null;
					try {

						String className = "";
						
						module = (String) req.getSession().getAttribute("_module_requested");
						System.out.println("MODULE IS = " + module);

						if (( module.indexOf(".") > 0 )) {
							className = module;
							//content = ClassLoadManager.load(className);
							content = ClassLoadManager.load(className, module, req.getRequestedSessionId());
							((VTemplate) content).setId(className);
						}
						else{
							
							
							
							className = CustomClass.getName(module);
							content = ClassLoadManager.load(className, module, req.getRequestedSessionId());
							((VTemplate) content).setId(module);
							
						}

						if ( content instanceof GenericPortlet ) {
						    PortletInfo portletInfo = new PortletInfo();
							portletInfo.id = "test_id";
							portletInfo.title = "Test Title";						    
							Hashtable portletState = getPortletState(getServletConfig(), req, res, out, portletInfo);
							RenderRequest renderRequest = (RenderRequest) portletState.get("renderRequest");
							RenderResponse renderResponse = (RenderResponse) portletState.get("renderResponse");
							PortletConfig config = (PortletConfig) portletState.get("config");
							GenericPortlet portlet = (GenericPortlet) content;
							portlet.init(config);
							portlet.render(renderRequest, renderResponse);
						} else {	
							((VTemplate) content).setEnvironment(engine, context, req, res);	
							((VTemplate) content).setServletContext(getServletConfig().getServletContext());	
							((VTemplate) content).setServletConfig(getServletConfig());
							((VTemplate) content).setDiv(false);
							
							if ( content instanceof Attributable ) {
								Hashtable h = UserPage.getValuesForAttributable(module);
								if ( h != null ) {
									((Attributable) content).setValues(h);
								}	
							}
							
							try {
								if ( content != null ) {
									((VTemplate) content).setShowVM(true);
									((VTemplate) content).print(session);
								}
							} catch ( Exception ex ) {
								out.println( ex.getMessage() );
							}						
						}
						//out.println("----------");
						
					} catch ( ClassNotFoundException cnfex ) {
						content = new ErrorMsg(engine, context, req, res);
						((ErrorMsg) content).setError("ClassNotFoundException : " + cnfex.getMessage());				
					} catch ( InstantiationException iex ) {
						content = new ErrorMsg(engine, context, req, res);
						((ErrorMsg) content).setError("InstantiationException : " + iex.getMessage());			
					} catch ( IllegalAccessException illex ) {
						content = new ErrorMsg(engine, context, req, res);
						((ErrorMsg) content).setError("IllegalAccessException : " + illex.getMessage());			
					} catch ( Exception ex ) {
						content = new ErrorMsg(engine, context, req, res);
						((ErrorMsg) content).setError("Other Exception during class initiation : " + ex.getMessage());	
						ex.printStackTrace();					
					}	
					
				} catch ( Exception ex ) {
					System.out.println( ex.getMessage() );	
				} finally {
					//long totalMem = Runtime.getRuntime().totalMemory();
					//System.out.println("total memory = " + totalMem);	
				}
			//}
        }
		// CLEANUP VELOCITY CONTEXT: BEGIN 
        try {
			Object[] objArray = context.getKeys();
			//System.out.println("[ControllerServlet3] No.Of Keys before cleanup = "+objArray.length);
			for(int i = 0; i < objArray.length; i++) {
				//System.out.println("[DesktopController] key = "+key);
				context.remove(objArray[i]);
			}
			//objArray = context.getKeys();
			//System.out.println("[ControllerServlet3] No.Of Keys after cleanup = "+objArray.length);
			// CLEANUP VELOCITY CONTEXT: END
		
        } catch ( Exception e ) {
        	System.out.println(e.getMessage());
        }
	}
	
	private static Hashtable getPortletState(ServletConfig svtCfg,
			HttpServletRequest req,
				HttpServletResponse res,
				PrintWriter out,
				PortletInfo portletInfo) throws Exception {
		Hashtable h = new Hashtable();
		
		MerakContext context = new MerakContext();
		context.httpServletRequest = req;
		
		MerakConfig config = new MerakConfig();
		config.portletInfo = portletInfo;
		config.portletContext = context;
		
		MerakResponse renderResponse = new MerakResponse();
		MerakRequest renderRequest = new MerakRequest();	
		renderRequest.windowState = WindowState.NORMAL;
		renderRequest.portletMode = PortletMode.VIEW;
		renderResponse.printWriter = out;
		renderRequest.httpServletRequest = req;
		renderResponse.httpServletResponse = res;
		h.put("renderRequest", renderRequest);
		h.put("renderResponse", renderResponse);
		h.put("config", config);
		return h;	
	}
	
	private static void securityTokenDenied(VelocityEngine engine, VelocityContext context, HttpServletRequest req, HttpServletResponse res)  {
		try {
			VTemplate content = new SecurityTokenDenied(engine, context, req, res);
			content.print();
		} catch ( Exception e) {
			System.out.println(e.getMessage());
		} finally {
			
		}
	}
	

}
