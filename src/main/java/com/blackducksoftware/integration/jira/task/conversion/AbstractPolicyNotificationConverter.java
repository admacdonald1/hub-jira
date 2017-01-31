/**
 * Hub JIRA Plugin
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.jira.task.conversion;

import java.util.List;
import java.util.Map;

import com.blackducksoftware.integration.hub.api.policy.PolicyRule;
import com.blackducksoftware.integration.hub.dataservice.model.ProjectVersion;
import com.blackducksoftware.integration.hub.dataservice.notification.model.NotificationContentItem;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.notification.processor.SubProcessorCache;
import com.blackducksoftware.integration.hub.notification.processor.event.NotificationEvent;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.jira.common.HubJiraConstants;
import com.blackducksoftware.integration.jira.common.HubJiraLogger;
import com.blackducksoftware.integration.jira.common.HubProjectMappings;
import com.blackducksoftware.integration.jira.common.HubUrlParser;
import com.blackducksoftware.integration.jira.common.JiraContext;
import com.blackducksoftware.integration.jira.common.JiraProject;
import com.blackducksoftware.integration.jira.common.exception.ConfigurationException;
import com.blackducksoftware.integration.jira.config.HubJiraFieldCopyConfigSerializable;
import com.blackducksoftware.integration.jira.task.JiraSettingsService;
import com.blackducksoftware.integration.jira.task.issue.JiraServices;

public abstract class AbstractPolicyNotificationConverter extends NotificationToEventConverter {
    private final HubJiraLogger logger;

    public AbstractPolicyNotificationConverter(final SubProcessorCache cache, final HubProjectMappings mappings, final JiraServices jiraServices,
            final JiraContext jiraContext, final JiraSettingsService jiraSettingsService,
            final String issueTypeName,
            final HubJiraFieldCopyConfigSerializable fieldCopyConfig,
            final HubServicesFactory hubServicesFactory, final HubJiraLogger logger)
            throws ConfigurationException {
        super(cache, jiraServices, jiraContext, jiraSettingsService, mappings, issueTypeName, fieldCopyConfig,
                hubServicesFactory, logger);
        this.logger = logger;
    }

    @Override
    public void process(final NotificationContentItem notification) throws HubIntegrationException {
        logger.debug("policyNotif: " + notification);
        final ProjectVersion projectVersion = notification.getProjectVersion();
        logger.debug("Getting JIRA project(s) mapped to Hub project: " + projectVersion.getProjectName());
        final List<JiraProject> mappingJiraProjects = getMappings()
                .getJiraProjects(projectVersion.getProjectName());
        logger.debug("There are " + mappingJiraProjects.size() + " JIRA projects mapped to this Hub project : "
                + projectVersion.getProjectName());

        if (!mappingJiraProjects.isEmpty()) {

            for (final JiraProject jiraProject : mappingJiraProjects) {
                logger.debug("JIRA Project: " + jiraProject);
                try {
                    final List<NotificationEvent> projectEvents = handleNotificationPerJiraProject(notification, jiraProject);
                    if (projectEvents != null) {
                        for (final NotificationEvent event : projectEvents) {
                            getCache().addEvent(event);
                        }
                    }
                } catch (final Exception e) {
                    logger.error(e);
                    getJiraSettingsService().addHubError(e, projectVersion.getProjectName(),
                            projectVersion.getProjectVersionName(), jiraProject.getProjectName(),
                            getJiraContext().getJiraUser().getName(), "transitionIssue");
                }

            }
        }
    }

    protected abstract List<NotificationEvent> handleNotificationPerJiraProject(final NotificationContentItem notif,
            final JiraProject jiraProject) throws HubIntegrationException;

    protected String getIssueDescription(final NotificationContentItem notif, final PolicyRule rule) {
        final StringBuilder issueDescription = new StringBuilder();
        issueDescription.append("The Black Duck Hub has detected a policy violation on Hub project ['");

        issueDescription.append(notif.getProjectVersion().getProjectName());
        issueDescription.append("' / '");
        issueDescription.append(notif.getProjectVersion().getProjectVersionName());
        issueDescription.append("'|");
        issueDescription.append(notif.getProjectVersion().getComponentsLink());
        issueDescription.append("], component '");
        issueDescription.append(notif.getComponentName());
        issueDescription.append("' / '");
        issueDescription.append(notif.getComponentVersion().getVersionName());
        issueDescription.append("'.");
        issueDescription.append(" The rule violated is: '");
        issueDescription.append(rule.getName());
        issueDescription.append("'. Rule overridable : ");
        issueDescription.append(rule.getOverridable());

        final String licenseText;
        try {
            licenseText = getComponentLicensesStringWithLinksAtlassianFormat(notif);
            issueDescription.append("\nLicense(s): ");
            issueDescription.append(licenseText);
        } catch (final HubIntegrationException e) {
            // omit license text
        }

        return issueDescription.toString();
    }

    protected String getIssueSummary(final NotificationContentItem notif, final PolicyRule rule) {
        final StringBuilder issueSummary = new StringBuilder();
        issueSummary.append("Black Duck policy violation detected on Hub project '");
        issueSummary.append(notif.getProjectVersion().getProjectName());
        issueSummary.append("' / '");
        issueSummary.append(notif.getProjectVersion().getProjectVersionName());
        issueSummary.append("', component '");
        issueSummary.append(notif.getComponentName());
        issueSummary.append("' / '");
        issueSummary.append(notif.getComponentVersion().getVersionName());
        issueSummary.append("'");
        issueSummary.append(" [Rule: '");
        issueSummary.append(rule.getName());
        issueSummary.append("']");
        return issueSummary.toString();
    }

    @Override
    public String generateEventKey(final Map<String, Object> inputData)
            throws HubIntegrationException {

        final Long jiraProjectId = (Long) inputData.get(EventDataSetKeys.JIRA_PROJECT_ID);
        final String policyRuleURL = (String) inputData.get(EventDataSetKeys.HUB_RULE_URL);
        final String hubProjectVersionUrl = (String) inputData.get(EventDataSetKeys.HUB_PROJECT_VERSION_URL);
        final String hubComponentUrl = (String) inputData.get(EventDataSetKeys.HUB_COMPONENT_URL);
        final String hubComponentVersionUrl = (String) inputData.get(EventDataSetKeys.HUB_COMPONENT_VERSION_URL);

        final StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_ISSUE_TYPE_NAME);
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_ISSUE_TYPE_VALUE_POLICY);
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_JIRA_PROJECT_ID_NAME);
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(jiraProjectId.toString());
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_HUB_PROJECT_VERSION_REL_URL_HASHED_NAME);
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(hashString(HubUrlParser.getRelativeUrl(hubProjectVersionUrl)));
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_HUB_COMPONENT_REL_URL_HASHED_NAME);
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(hashString(HubUrlParser.getRelativeUrl(hubComponentUrl)));
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_HUB_COMPONENT_VERSION_REL_URL_HASHED_NAME);
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(hashString(HubUrlParser.getRelativeUrl(hubComponentVersionUrl)));
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_HUB_POLICY_RULE_REL_URL_HASHED_NAME);
        keyBuilder.append(HubJiraConstants.ISSUE_PROPERTY_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(hashString(HubUrlParser.getRelativeUrl(policyRuleURL)));

        final String key = keyBuilder.toString();

        logger.debug("property key: " + key);
        return key;
    }
}
