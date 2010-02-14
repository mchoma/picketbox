/*
  * JBoss, Home of Professional Open Source
  * Copyright 2007, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
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
package org.jboss.security.plugins.javaee;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.jboss.logging.Logger;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.audit.AuditLevel;
import org.jboss.security.authorization.AuthorizationContext;
import org.jboss.security.authorization.AuthorizationException;
import org.jboss.security.authorization.ResourceKeys;
import org.jboss.security.authorization.resources.WebResource;
import org.jboss.security.callbacks.SecurityContextCallbackHandler;
import org.jboss.security.identity.RoleGroup;
import org.jboss.security.javaee.AbstractWebAuthorizationHelper;

/**
 *  Web Authorization Helper Implementation
 *  @author Anil.Saldhana@redhat.com
 *  @since  Apr 18, 2008 
 *  @version $Revision$
 */
public class WebAuthorizationHelper 
extends AbstractWebAuthorizationHelper
{
   protected static Logger log = Logger.getLogger(WebAuthorizationHelper.class);
   
   @Override
   public boolean checkResourcePermission(
         Map<String, Object> contextMap, 
         ServletRequest request,
         ServletResponse response, 
         Subject callerSubject, 
         String contextID, 
         String canonicalRequestURI)
   {
      if(contextID == null)
         throw new IllegalArgumentException("ContextID is null");  
      if(request == null)
         throw new IllegalArgumentException("request is null");
      if(response == null)
         throw new IllegalArgumentException("response is null");
      if(canonicalRequestURI == null)
         throw new IllegalArgumentException("canonicalRequestURI is null");  

      AuthorizationManager authzMgr = securityContext.getAuthorizationManager();
      
      if(authzMgr == null)
         throw new IllegalStateException("Authorization Manager is null");
      
      boolean isAuthorized = false; 

      WebResource webResource = new WebResource(Collections.unmodifiableMap(contextMap));
      webResource.setPolicyContextID(contextID);
      webResource.setServletRequest(request);
      webResource.setServletResponse(response);
      webResource.setCallerSubject(callerSubject);
      webResource.setCanonicalRequestURI(canonicalRequestURI);

      SecurityContextCallbackHandler sch = new SecurityContextCallbackHandler(this.securityContext); 
      RoleGroup callerRoles = authzMgr.getSubjectRoles(callerSubject, sch);

      try
      {
         int permit = authzMgr.authorize(webResource, callerSubject, callerRoles);
         isAuthorized = (permit == AuthorizationContext.PERMIT);
         String level = (permit == AuthorizationContext.PERMIT ? AuditLevel.SUCCESS : AuditLevel.FAILURE);
         if(this.enableAudit)
            this.authorizationAudit(level,webResource, null); 
      }
      catch (AuthorizationException e)
      {
         isAuthorized = false; 
         if(log.isTraceEnabled()) 
            log.trace("hasResourcePermission check failed:"+e.getLocalizedMessage(), e); 
         if(this.enableAudit)
            authorizationAudit(AuditLevel.ERROR,webResource,e); 
      }
      return isAuthorized; 
   }

   @Override
   public boolean hasRole(
         String roleName, 
         Principal principal, 
         String servletName, 
         Set<Principal> principalRoles,  
         String contextID,
         Subject callerSubject)
   {
      if(roleName == null)
         throw new IllegalArgumentException("roleName is null");
      if(contextID == null)
         throw new IllegalArgumentException("ContextID is null");
      
      if(callerSubject == null)
         throw new IllegalArgumentException("callerSubject is null");
            
      AuthorizationManager authzMgr = securityContext.getAuthorizationManager();
      if(authzMgr == null)
         throw new IllegalStateException("Authorization Manager is null");
      
      boolean hasTheRole = false;
      Map<String,Object> map =  new HashMap<String,Object>();  
      map.put(ResourceKeys.ROLENAME, roleName); 
      map.put(ResourceKeys.ROLEREF_PERM_CHECK, Boolean.TRUE);  
      map.put(ResourceKeys.PRINCIPAL_ROLES, principalRoles);

      map.put(ResourceKeys.POLICY_REGISTRATION, getPolicyRegistration());
      
      WebResource webResource = new WebResource(Collections.unmodifiableMap(map));
      webResource.setPolicyContextID(contextID);
      webResource.setPrincipal(principal);
      webResource.setServletName(servletName);
       
      webResource.setCallerSubject(callerSubject);
      SecurityContextCallbackHandler sch = new SecurityContextCallbackHandler(this.securityContext); 
      RoleGroup callerRoles = authzMgr.getSubjectRoles(callerSubject, sch);
      
      try
      {
         int permit = authzMgr.authorize(webResource, callerSubject, callerRoles);
         hasTheRole = (permit == AuthorizationContext.PERMIT);
         String level = (hasTheRole ? AuditLevel.SUCCESS : AuditLevel.FAILURE);
         if(this.enableAudit)
           this.authorizationAudit(level,webResource, null);
      }
      catch (AuthorizationException e)
      {
         hasTheRole = false; 
         if(log.isTraceEnabled()) 
            log.trace("hasRole check failed:"+e.getLocalizedMessage(), e); 
         if(this.enableAudit)
            authorizationAudit(AuditLevel.ERROR,webResource,e); 
      }
      return hasTheRole; 
   }

   @Override
   public boolean hasUserDataPermission(Map<String, Object> contextMap, 
         ServletRequest request,
         ServletResponse response,
         String contextID,
         Subject callerSubject)
   {
      if(contextID == null)
         throw new IllegalArgumentException("ContextID is null"); 
      if(callerSubject == null)
         throw new IllegalArgumentException("callerSubject is null");
      if(request == null)
         throw new IllegalArgumentException("request is null");
      if(response == null)
         throw new IllegalArgumentException("response is null");
      
      AuthorizationManager authzMgr = securityContext.getAuthorizationManager();
      if(authzMgr == null)
         throw new IllegalStateException("Authorization Manager is null");
      
      boolean hasPerm =  false;   
      contextMap.put(ResourceKeys.POLICY_REGISTRATION, getPolicyRegistration());
      
      WebResource webResource = new WebResource(Collections.unmodifiableMap(contextMap)); 
      webResource.setPolicyContextID(contextID);
      webResource.setServletRequest(request);
      webResource.setServletResponse(response);
      
      webResource.setCallerSubject(callerSubject);
      SecurityContextCallbackHandler sch = new SecurityContextCallbackHandler(this.securityContext); 
      RoleGroup callerRoles = authzMgr.getSubjectRoles(callerSubject, sch);
      
      try
      {
         int permit = authzMgr.authorize(webResource, callerSubject, callerRoles);
         hasPerm = (permit == AuthorizationContext.PERMIT);
         String level = (hasPerm ? AuditLevel.SUCCESS : AuditLevel.FAILURE);
         if(this.enableAudit)
            this.authorizationAudit(level,webResource, null);
      }
      catch (AuthorizationException e)
      {
         hasPerm = false; 
         if(log.isTraceEnabled()) 
            log.trace("hasRole check failed:"+e.getLocalizedMessage(), e); 
         if(this.enableAudit)
            authorizationAudit(AuditLevel.ERROR,webResource,e); 
      }
      return hasPerm;
   } 
}