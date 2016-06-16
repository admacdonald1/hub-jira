package com.blackducksoftware.integration.jira.issue;

import com.blackducksoftware.integration.jira.hub.model.NameVersion;

public class Issue {
	private final IssueLevel level;
	private final NameVersion hubProject;
	private final NameVersion hubComponent;
	private final String ruleUrl;
	private final String jiraProjectKey;

	public Issue(final IssueLevel level, final NameVersion hubProject, final NameVersion hubComponent,
			final String ruleUrl, final String jiraProjectKey) {
		super();
		this.level = level;
		this.hubProject = hubProject;
		this.hubComponent = hubComponent;
		this.ruleUrl = ruleUrl;
		this.jiraProjectKey = jiraProjectKey;
	}
	public IssueLevel getLevel() {
		return level;
	}
	public NameVersion getHubProject() {
		return hubProject;
	}
	public NameVersion getHubComponent() {
		return hubComponent;
	}

	public String getRuleUrl() {
		return ruleUrl;
	}
	public String getJiraProjectKey() {
		return jiraProjectKey;
	}
	@Override
	public String toString() {
		return "Issue [level=" + level + ", hubProject=" + hubProject + ", hubComponent=" + hubComponent + ", ruleUrl="
				+ ruleUrl + ", jiraProjectKey=" + jiraProjectKey + "]";
	}


}
