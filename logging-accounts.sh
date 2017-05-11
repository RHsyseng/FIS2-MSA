#! /bin/bash
# Create and configure secrets and service accounts for logging project
# Usage: ./logging-accounts.sh projectName

# If no project name is specified as an argument, 'logging' is assumed
PROJ=${1:-"logging"}

# clean up anything that might be handing around from a previous run or logging installation
oc delete secret logging-deployer
oc delete sa logging-deployer
oc delete oauthclient kibana-proxy
oc delete sa aggregated-logging-fluentd;
oc delete sa aggregated-logging-kibana;
oc delete sa aggregated-logging-elasticsearch;
oc delete sa aggregated-logging-curator;

# create a new secret for Kibana to use to serve cert (/dev/null to have one generated)
oc secrets new logging-deployer nothing=/dev/null

# create service account 'logging-deployer' using the secret created
oc create -f - <<SASTOP
apiVersion: v1
kind: ServiceAccount
metadata:
  name: logging-deployer
secrets:
- name: logging-deployer
SASTOP

# add cluster-admin privs to the new service account, needed for the logging template
oadm policy add-cluster-role-to-user cluster-admin system:serviceaccount:$PROJ:logging-deployer;

# create the additional service accounts needed for the template
oc create sa aggregated-logging-fluentd;
oc create sa aggregated-logging-kibana;
oc create sa aggregated-logging-elasticsearch;
oc create sa aggregated-logging-curator;

# add each of these service accounts to the privileged security context contraint
oadm policy add-scc-to-user privileged system:serviceaccount:$PROJ:aggregated-logging-fluentd;
oadm policy add-scc-to-user privileged system:serviceaccount:$PROJ:aggregated-logging-kibana;
oadm policy add-scc-to-user privileged system:serviceaccount:$PROJ:aggregated-logging-elasticsearch;
oadm policy add-scc-to-user privileged system:serviceaccount:$PROJ:aggregated-logging-curator;
oadm policy add-cluster-role-to-user cluster-reader system:serviceaccount:$PROJ:aggregated-logging-fluentd;
oadm policy add-cluster-role-to-user cluster-reader system:serviceaccount:$PROJ:aggregated-logging-kibana;
oadm policy add-cluster-role-to-user cluster-reader system:serviceaccount:$PROJ:aggregated-logging-elasticsearch;
oadm policy add-cluster-role-to-user cluster-reader system:serviceaccount:$PROJ:aggregated-logging-curator;