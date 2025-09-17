###########################################################
#ENV VARS
###########################################################
envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3
COMMON_NAMESPACE=$4
BUSINESS_NAMESPACE=$5
PSI_SELECTION_NAMESPACE=$6
SPLUNK_TOKEN=$7
APP_LOG_LEVEL=$8

SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    info
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   Parsers_File parsers.conf
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Exclude_Path *.gz,*.zip
   Parser docker
   Mem_Buf_Limit 20MB
   Buffer_Max_Size 1MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  absolutely_nothing_bud
   Log_Level    off
[OUTPUT]
   Name  splunk
   Match *
   Host  $SPLUNK_URL
   Port  443
   TLS         On
   TLS.Verify  Off
   Message_Key $APP_NAME
   Splunk_Token $SPLUNK_TOKEN
"
PARSER_CONFIG="
[PARSER]
    Name        docker
    Format      json
"
###########################################################
#Setup for config-maps base & FB
###########################################################
echo Showing PSI selection namespace: "$PSI_SELECTION_NAMESPACE"
echo Creating config map "$APP_NAME"-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-config-map \
 --from-literal=GRAD_TRAX_API="http://educ-grad-trax-api.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080/" \
 --dry-run=client -o yaml | oc apply -f -

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map \
  --from-literal=fluent-bit.conf="$FLB_CONFIG" \
  --from-literal=parsers.conf="$PARSER_CONFIG" \
  --dry-run=client -o yaml | oc apply -f -
