/*
 * Copyright (c) 2012 S.C. Axemblr Software Solutions S.R.L
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.axemblr.provisionr.cloudstack.activities;

import com.axemblr.provisionr.api.network.Network;
import com.axemblr.provisionr.api.pool.Pool;
import com.axemblr.provisionr.cloudstack.core.SecurityGroups;
import static com.google.common.base.Preconditions.checkNotNull;
import org.activiti.engine.delegate.DelegateExecution;
import org.jclouds.cloudstack.CloudStackClient;
import org.jclouds.cloudstack.domain.SecurityGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a CloudStack {@link SecurityGroup} with specified rules. If a SecurityGroup with the same name exists,
 * it will be deleted first.
 */
public class EnsureSecurityGroupExists extends CloudStackActivity {

    private static final Logger LOG = LoggerFactory.getLogger(EnsureSecurityGroupExists.class);

    @Override
    public void execute(CloudStackClient cloudStackClient, Pool pool, DelegateExecution execution) {
        Network network = checkNotNull(pool.getNetwork(), "Please configure a network for the pool");
        String securityGroupName = SecurityGroups.formatNameFromBusinessKey(execution.getProcessBusinessKey());
        SecurityGroup securityGroup = null;
        try {
            LOG.info("Creating SecurityGroup {}", securityGroupName);
            securityGroup = SecurityGroups.createSecurityGroup(cloudStackClient, securityGroupName);
        } catch (IllegalStateException e) {
            LOG.info("Failed creating SecurityGroup {} - checking if it exists", securityGroupName);
            securityGroup = SecurityGroups.getByName(cloudStackClient, securityGroupName);
            LOG.info("Delete old SecurityGroup rules for {}", securityGroupName);
            SecurityGroups.deleteNetworkRules(cloudStackClient, securityGroup);
        }
        if (securityGroup != null) {
            LOG.info("Applying network rules on SecurityGroup {}", securityGroupName);
            SecurityGroups.applyNetworkRules(cloudStackClient, securityGroup, network);
        }
    }
}