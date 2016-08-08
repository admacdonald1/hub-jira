package com.blackducksoftware.integration.jira.task.conversion;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.blackducksoftware.integration.hub.api.component.ComponentVersionStatus;
import com.blackducksoftware.integration.hub.api.notification.NotificationItem;
import com.blackducksoftware.integration.hub.api.notification.PolicyOverrideNotificationItem;
import com.blackducksoftware.integration.hub.api.version.ReleaseItem;
import com.blackducksoftware.integration.hub.exception.NotificationServiceException;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.hub.notification.NotificationService;
import com.blackducksoftware.integration.jira.common.HubJiraLogger;
import com.blackducksoftware.integration.jira.common.HubProjectMappings;
import com.blackducksoftware.integration.jira.common.JiraContext;
import com.blackducksoftware.integration.jira.task.conversion.output.HubEvent;
import com.blackducksoftware.integration.jira.task.conversion.output.HubEventType;
import com.blackducksoftware.integration.jira.task.issue.JiraServices;

public class PolicyOverrideNotificationConverter extends PolicyNotificationConverter {
	private final HubJiraLogger logger = new HubJiraLogger(Logger.getLogger(this.getClass().getName()));

	public PolicyOverrideNotificationConverter(final HubProjectMappings mappings, final JiraServices jiraServices,
			final JiraContext jiraContext,
			final List<String> linksOfRulesToMonitor, final NotificationService hubNotificationService) {
		super(mappings, jiraServices, jiraContext, linksOfRulesToMonitor, hubNotificationService);
	}

	@Override
	public List<HubEvent> generateEvents(final NotificationItem notif) {
		List<HubEvent> events;

		if (!isRulesToMonitor()) {
			logger.warn("No rules-to-monitor provided, skipping policy notifications.");
			return null;
		}

		HubEventType eventType;
		String projectName;
		String projectVersionName;
		final ReleaseItem notifHubProjectReleaseItem;
		eventType = HubEventType.POLICY_OVERRIDE;
		final PolicyOverrideNotificationItem ruleViolationNotif = (PolicyOverrideNotificationItem) notif;
		final List<ComponentVersionStatus> compVerStatuses = new ArrayList<>();
		final ComponentVersionStatus componentStatus = new ComponentVersionStatus();

		try {
			componentStatus.setBomComponentVersionPolicyStatusLink(ruleViolationNotif.getContent()
					.getBomComponentVersionPolicyStatusLink());
			componentStatus.setComponentName(ruleViolationNotif.getContent().getComponentName());
			componentStatus.setComponentVersionLink(ruleViolationNotif.getContent().getComponentVersionLink());
			compVerStatuses.add(componentStatus);
			projectName = ruleViolationNotif.getContent().getProjectName();
			notifHubProjectReleaseItem = getHubNotificationService().getProjectReleaseItemFromProjectReleaseUrl(
					ruleViolationNotif.getContent().getProjectVersionLink());
			projectVersionName = notifHubProjectReleaseItem.getVersionName();
			events = handleNotification(eventType, projectName, projectVersionName, compVerStatuses,
					notifHubProjectReleaseItem);
		} catch (UnexpectedHubResponseException | NotificationServiceException e) {
			logger.error(e);
			return null;
		}
		return events;
	}

}
