<%--
  Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  The ASF licenses this file to You
  under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.  For additional information regarding
  copyright in this work, please see the NOTICE file in the top level
  directory of this distribution.
--%>
<%@ include file="/WEB-INF/jsps/tightblog-taglibs.jsp" %>
<script src='<c:url value="/tb-ui/scripts/jquery-2.2.3.min.js" />'></script>
<script src="//ajax.googleapis.com/ajax/libs/angularjs/1.7.0/angular.min.js"></script>

<script>
    var contextPath = "${pageContext.request.contextPath}";
    var weblogId = "<c:out value='${actionWeblog.id}'/>";
    var templateId = "<c:out value='${param.templateId}'/>";
    var templateName = "<c:out value='${param.templateName}'/>";
    var weblogUrl = "<c:out value='${actionWeblogURL}'/>";
</script>
<script src="<c:url value='/tb-ui/scripts/commonangular.js'/>"></script>
<script src="<c:url value='/tb-ui/scripts/templateedit.js'/>"></script>

<c:url var="refreshUrl" value="/tb-ui/app/authoring/templateedit">
    <c:param name="weblogId" value="${param.weblogId}"/>
    <c:param name="templateId" value="${param.templateId}"/>
    <c:param name="templateName" value="${param.templateName}"/>
</c:url>

<input id="refreshURL" type="hidden" value="${refreshURL}"/>

<div id="successMessageDiv" class="alert alert-success" role="alert" ng-show="ctrl.showSuccessMessage" ng-cloak>
    <p><fmt:message key="generic.changes.saved"/> ({{ctrl.templateData.lastModified | date:'short'}})</p>
    <button type="button" class="close" data-ng-click="ctrl.showSuccessMessage = false" aria-label="Close">
       <span aria-hidden="true">&times;</span>
    </button>
</div>

<div id="errorMessageDiv" class="alert alert-danger" role="alert" ng-show="ctrl.errorObj.errors" ng-cloak>
    <button type="button" class="close" data-ng-click="ctrl.errorObj.errors = null" aria-label="Close">
       <span aria-hidden="true">&times;</span>
    </button>
    <ul class="list-unstyled">
       <li ng-repeat="item in ctrl.errorObj.errors">{{item.message}}</li>
    </ul>
</div>

<p class="subtitle">
   <fmt:message key="templateEdit.subtitle"/>
</p>

<p class="pagetip"><fmt:message key="templateEdit.tip" /></p>

<table cellspacing="5">
    <tr>
        <td class="label"><fmt:message key="generic.name"/>&nbsp;</td>
        <td class="field">
            <input id="name" type="text" ng-model="ctrl.templateData.name" size="50" maxlength="255" style="background: #e5e5e5" ng-readonly="ctrl.templateData.derivation != 'Blog-Only'"/>
            <span ng-if="ctrl.templateData.role.accessibleViaUrl">
                <br/>
                <c:out value="${actionWeblogURL}"/>page/<span id="linkPreview" style="color:red">{{ctrl.templateData.name}}</span>
                <span ng-if="ctrl.lastSavedName != null">
                    [<a id="launchLink" ng-click="ctrl.launchPage()"><fmt:message key="templateEdit.launch" /></a>]
                </span>
            </span>
        </td>
    </tr>

    <tr>
        <td class="label"><fmt:message key="templateEdit.role" />&nbsp;</td>
        <td class="field">
             <span>{{ctrl.templateData.role.readableName}}</span>
        </td>
    </tr>

    <tr ng-if="!ctrl.template.role.singleton">
        <td class="label" valign="top" style="padding-top: 4px">
            <fmt:message key="generic.description"/>&nbsp;
        </td>
        <td class="field">
            <textarea id="description" type="text" ng-model="ctrl.templateData.description" cols="50" rows="2"></textarea>
        </td>
    </tr>

</table>

<textarea ng-model="ctrl.templateData.template" rows="20" style="width:100%"></textarea>

<c:url var="templatesUrl" value="/tb-ui/app/authoring/templates">
    <c:param name="weblogId" value="${param.weblogId}" />
</c:url>

<table style="width:100%">
    <tr>
        <td>
            <button type="button" ng-click="ctrl.saveTemplate()"><fmt:message key='generic.save'/></button>
            <button type="button" onclick="window.location='${templatesUrl}'"><fmt:message key='generic.cancel'/></button>
        </td>
    </tr>
</table>
